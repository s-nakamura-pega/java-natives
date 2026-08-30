#pragma once

extern "C"
{
#include <libavformat/avformat.h>
#include <libavcodec/avcodec.h>
#include <libswscale/swscale.h>
#include <libswresample/swresample.h>
}
#include <chrono>
#include <portaudio.h>

#include <cstring>
#include <thread>
#include <atomic>
#include <mutex>
#include <queue>
#include <vector>
#include <jni.h>

// ------------------------------
// PortAudio 自動デバイス選択
// ------------------------------
int chooseBestAudioDevice()
{
    int numDevices = Pa_GetDeviceCount();
    if (numDevices < 0)
        return -1;

    int best = -1;

    for (int i = 0; i < numDevices; i++)
    {
        const PaDeviceInfo *info = Pa_GetDeviceInfo(i);

        if (info->maxOutputChannels <= 0)
            continue;

        if (strstr(info->name, "cards.pcm") != nullptr)
            continue;
        if (strstr(info->name, "default") != nullptr)
            continue;
        if (strstr(info->name, "sysdefault") != nullptr)
            continue;

        best = i;
        break;
    }

    if (best < 0)
        best = Pa_GetDefaultOutputDevice();

    return best;
}

// ------------------------------
// PlayerStruct 本体
// ------------------------------
struct PlayerStruct
{
    // FFmpeg core
    AVFormatContext *fmtCtx = nullptr;

    // video
    AVCodecContext *decCtx = nullptr;
    AVStream *videoStream = nullptr;
    int videoStreamIndex = -1;
    SwsContext *swsCtx = nullptr;

    // audio
    AVCodecContext *audioCtx = nullptr;
    AVStream *audioStream = nullptr;
    int audioStreamIndex = -1;
    SwrContext *swrCtx = nullptr;
    PaStream *paStream = nullptr;

    // threads
    std::thread readThread;
    std::thread videoThread;
    std::thread audioThread;
    std::atomic<bool> running{false};
    std::atomic<bool> playing{false};
    std::atomic<bool> decodeReady{false};

    // packet queues
    std::queue<AVPacket *> videoQueue;
    std::queue<AVPacket *> audioQueue;
    std::mutex queueMutex;

    // frame buffer (RGB24)
    std::mutex frameMutex;
    std::vector<uint8_t> frameBuffer;
    int frameWidth = 0;
    int frameHeight = 0;

    // duration
    long durationMs = -1;

    // Java
    JavaVM *jvm = nullptr;
    jobject javaCanvasObj = nullptr;
    std::mutex fmtMutex;

    PlayerStruct(JavaVM *vm, jobject canvasObj)
        : jvm(vm)
    {
        avformat_network_init();
        Pa_Initialize();

        JNIEnv *env = nullptr;
        jvm->AttachCurrentThread((void **)&env, nullptr);
        javaCanvasObj = env->NewGlobalRef(canvasObj);
        jvm->DetachCurrentThread();
    }

    ~PlayerStruct()
    {
        running = false;
        playing = false;

        if (readThread.joinable())
            readThread.join();
        if (videoThread.joinable())
            videoThread.join();
        if (audioThread.joinable())
            audioThread.join();

        if (paStream)
        {
            Pa_StopStream(paStream);
            Pa_CloseStream(paStream);
        }
        Pa_Terminate();

        if (javaCanvasObj)
        {
            JNIEnv *env = nullptr;
            jvm->AttachCurrentThread((void **)&env, nullptr);
            env->DeleteGlobalRef(javaCanvasObj);
            jvm->DetachCurrentThread();
        }

        clearQueues();

        if (swsCtx)
            sws_freeContext(swsCtx);
        if (swrCtx)
            swr_free(&swrCtx);
        if (decCtx)
            avcodec_free_context(&decCtx);
        if (audioCtx)
            avcodec_free_context(&audioCtx);
        if (fmtCtx)
            avformat_close_input(&fmtCtx);
    }

    void clearQueues()
    {
        std::lock_guard<std::mutex> lock(queueMutex);
        while (!videoQueue.empty())
        {
            AVPacket *p = videoQueue.front();
            videoQueue.pop();
            av_packet_free(&p);
        }
        while (!audioQueue.empty())
        {
            AVPacket *p = audioQueue.front();
            audioQueue.pop();
            av_packet_free(&p);
        }
    }

    long setFile(const char *path)
    {
        decodeReady = false;
        playing = false;
        running = false;

        if (readThread.joinable())
            readThread.join();
        if (videoThread.joinable())
            videoThread.join();
        if (audioThread.joinable())
            audioThread.join();

        clearQueues();

        if (swsCtx)
        {
            sws_freeContext(swsCtx);
            swsCtx = nullptr;
        }
        if (swrCtx)
        {
            swr_free(&swrCtx);
            swrCtx = nullptr;
        }
        if (decCtx)
        {
            avcodec_free_context(&decCtx);
            decCtx = nullptr;
        }
        if (audioCtx)
        {
            avcodec_free_context(&audioCtx);
            audioCtx = nullptr;
        }
        if (fmtCtx)
        {
            avformat_close_input(&fmtCtx);
            fmtCtx = nullptr;
        }

        if (avformat_open_input(&fmtCtx, path, nullptr, nullptr) < 0)
            return -1;
        if (avformat_find_stream_info(fmtCtx, nullptr) < 0)
            return -1;

        videoStreamIndex = -1;
        audioStreamIndex = -1;
        videoStream = nullptr;
        audioStream = nullptr;

        for (unsigned i = 0; i < fmtCtx->nb_streams; ++i)
        {
            auto *st = fmtCtx->streams[i];
            if (st->codecpar->codec_type == AVMEDIA_TYPE_VIDEO && videoStreamIndex < 0)
            {
                videoStreamIndex = i;
                videoStream = st;
            }
            else if (st->codecpar->codec_type == AVMEDIA_TYPE_AUDIO && audioStreamIndex < 0)
            {
                audioStreamIndex = i;
                audioStream = st;
            }
        }
        if (videoStreamIndex < 0)
            return -1;

        // video codec
        {
            const AVCodec *dec = avcodec_find_decoder(videoStream->codecpar->codec_id);
            decCtx = avcodec_alloc_context3(dec);
            avcodec_parameters_to_context(decCtx, videoStream->codecpar);
            avcodec_open2(decCtx, dec, nullptr);

            frameWidth = decCtx->width;
            frameHeight = decCtx->height;

            swsCtx = sws_getContext(
                frameWidth, frameHeight, decCtx->pix_fmt,
                frameWidth, frameHeight, AV_PIX_FMT_RGB24,
                SWS_BILINEAR, nullptr, nullptr, nullptr);

            frameBuffer.resize(frameWidth * frameHeight * 3);
        }

        // audio codec
        if (audioStreamIndex >= 0)
        {
            const AVCodec *adec = avcodec_find_decoder(audioStream->codecpar->codec_id);
            audioCtx = avcodec_alloc_context3(adec);
            avcodec_parameters_to_context(audioCtx, audioStream->codecpar);
            avcodec_open2(audioCtx, adec, nullptr);

            AVChannelLayout outLayout;
            av_channel_layout_default(&outLayout, 2);

            swrCtx = swr_alloc();
            swr_alloc_set_opts2(
                &swrCtx,
                &outLayout,
                AV_SAMPLE_FMT_S16,
                48000,
                &audioCtx->ch_layout,
                audioCtx->sample_fmt,
                audioCtx->sample_rate,
                0,
                nullptr);
            swr_init(swrCtx);

            int dev = chooseBestAudioDevice();

            PaStreamParameters outParams;
            outParams.device = dev;
            outParams.channelCount = 2;
            outParams.sampleFormat = paInt16;
            outParams.suggestedLatency =
                Pa_GetDeviceInfo(outParams.device)->defaultLowOutputLatency;
            outParams.hostApiSpecificStreamInfo = nullptr;

            Pa_OpenStream(
                &paStream,
                nullptr,
                &outParams,
                48000,
                1024,
                paClipOff,
                nullptr,
                nullptr);

            Pa_StartStream(paStream);
        }

        if (fmtCtx->duration != AV_NOPTS_VALUE)
            durationMs = fmtCtx->duration / (AV_TIME_BASE / 1000);
        else
            durationMs = -1;

        decodeReady = true;

        {
            JNIEnv *env = nullptr;
            jvm->AttachCurrentThread((void **)&env, nullptr);
            jclass cls = env->GetObjectClass(javaCanvasObj);
            jmethodID mid = env->GetMethodID(cls, "initCanvas", "(II)V");
            env->CallVoidMethod(javaCanvasObj, mid, frameWidth, frameHeight);
            jvm->DetachCurrentThread();
        }

        running = true;

        // READ THREAD（唯一の av_read_frame）
        readThread = std::thread([this]()
                                 {
            AVPacket *pkt = av_packet_alloc();

            while (running) {
                {
                    // ① fmtCtx を先にロック
                    std::lock_guard<std::mutex> fmtLock(fmtMutex);

                    if (av_read_frame(fmtCtx, pkt) < 0) break;
                }

                {
                    // ② queue を後からロック
                    std::lock_guard<std::mutex> queueLock(queueMutex);

                    if (pkt->stream_index == videoStreamIndex) {
                        videoQueue.push(av_packet_clone(pkt));
                    } else if (pkt->stream_index == audioStreamIndex) {
                        audioQueue.push(av_packet_clone(pkt));
                    }
                }

        av_packet_unref(pkt);
    }

    av_packet_free(&pkt); });

        // VIDEO THREAD
        videoThread = std::thread([this]()
                                  {
            JNIEnv *env = nullptr;
            jvm->AttachCurrentThread((void **)&env, nullptr);

            jclass cls = env->GetObjectClass(javaCanvasObj);
            jmethodID repaintMid = env->GetMethodID(cls, "repaintCallback", "()V");

            AVFrame *frame = av_frame_alloc();
            AVFrame *rgbFrame = av_frame_alloc();

            rgbFrame->format = AV_PIX_FMT_RGB24;
            rgbFrame->width = frameWidth;
            rgbFrame->height = frameHeight;
            av_frame_get_buffer(rgbFrame, 32);

            auto startTime = std::chrono::steady_clock::now();

            while (running) {
                if (!playing) {
                    std::this_thread::sleep_for(std::chrono::milliseconds(5));
                    continue;
                }

                AVPacket *pkt = nullptr;
                {
                    std::lock_guard<std::mutex> lock(queueMutex);
                    if (!videoQueue.empty()) {
                        pkt = videoQueue.front();
                        videoQueue.pop();
                    }
                }

                if (!pkt) {
                    std::this_thread::sleep_for(std::chrono::milliseconds(1));
                    continue;
                }

                avcodec_send_packet(decCtx, pkt);

                while (avcodec_receive_frame(decCtx, frame) == 0) {
                    double pts_ms = 0;
                    if (frame->pts != AV_NOPTS_VALUE)
                        pts_ms = frame->pts * av_q2d(videoStream->time_base) * 1000.0;

                    auto now = std::chrono::steady_clock::now();
                    double elapsed_ms =
                        std::chrono::duration<double, std::milli>(now - startTime).count();

                    if (pts_ms > elapsed_ms)
                        std::this_thread::sleep_for(
                            std::chrono::milliseconds((long)(pts_ms - elapsed_ms)));

                    // ★ Java に現在位置を通知
                    jclass cls = env->GetObjectClass(javaCanvasObj);
                    jmethodID setPointMid = env->GetMethodID(cls, "setCurrentPoint", "(I)V");
                    env->CallVoidMethod(javaCanvasObj, setPointMid, (jint)pts_ms);

                    sws_scale(
                        swsCtx,
                        frame->data, frame->linesize,
                        0, frameHeight,
                        rgbFrame->data, rgbFrame->linesize
                    );

                    {
                        std::lock_guard<std::mutex> lock(frameMutex);
                        std::memcpy(frameBuffer.data(), rgbFrame->data[0],
                                    frameWidth * frameHeight * 3);
                    }

                    env->CallVoidMethod(javaCanvasObj, repaintMid);
                }

                av_packet_free(&pkt);
            }

            av_frame_free(&rgbFrame);
            av_frame_free(&frame);

            jvm->DetachCurrentThread(); });

        // AUDIO THREAD
        audioThread = std::thread([this]()
                                  {
            if (!audioCtx || !swrCtx || !paStream) return;

            AVFrame *frame = av_frame_alloc();

            const int maxSamples = 4096;
            std::vector<int16_t> pcmBuf(maxSamples * 2);

            while (running) {
                if (!playing) {
                    std::this_thread::sleep_for(std::chrono::milliseconds(5));
                    continue;
                }

                AVPacket *pkt = nullptr;
                {
                    std::lock_guard<std::mutex> lock(queueMutex);
                    if (!audioQueue.empty()) {
                        pkt = audioQueue.front();
                        audioQueue.pop();
                    }
                }

                if (!pkt) {
                    std::this_thread::sleep_for(std::chrono::milliseconds(1));
                    continue;
                }

                avcodec_send_packet(audioCtx, pkt);

                while (avcodec_receive_frame(audioCtx, frame) == 0) {
                    uint8_t *outData[] = {
                        reinterpret_cast<uint8_t *>(pcmBuf.data())
                    };

                    int outSamples = swr_convert(
                        swrCtx,
                        outData,
                        maxSamples,
                        (const uint8_t **)frame->data,
                        frame->nb_samples
                    );

                    if (outSamples > 0) {
                        Pa_WriteStream(paStream, pcmBuf.data(), outSamples);
                    }
                }

                av_packet_free(&pkt);
            }

            av_frame_free(&frame); });

        return durationMs;
    }

    void start()
    {
        if (decodeReady)
            playing = true;
    }
    void stop() { playing = false; }

    void movePoint(int ms)
    {
        playing = false;
        std::this_thread::sleep_for(std::chrono::milliseconds(10));

        {
            // ① fmtMutex → queueMutex の順番でロック
            std::lock_guard<std::mutex> fmtLock(fmtMutex);
            std::lock_guard<std::mutex> queueLock(queueMutex);
            clearQueues();

            // ④ FFmpeg のシーク処理
            int64_t ts = (int64_t)((double)ms / 1000.0 *
                                   videoStream->time_base.den / videoStream->time_base.num);

            {
                // ★ FFmpeg の fmtCtx は readThread と競合するのでロックが必要
                std::lock_guard<std::mutex> lock(fmtMutex); // ← 必須（後述）
                av_seek_frame(fmtCtx, videoStreamIndex, ts, AVSEEK_FLAG_BACKWARD);
                avcodec_flush_buffers(decCtx);
                if (audioCtx)
                    avcodec_flush_buffers(audioCtx);
            }

            // ⑤ queue をもう一度クリア（seek 後に古いパケットが残る可能性がある）
            clearQueues();
        }

        // ⑥ 再生再開
        playing = true;
    }

    bool isDecodeReady() const { return decodeReady; }
    bool isStarted() const { return playing; }

    int getFrame(unsigned char *out)
    {
        std::lock_guard<std::mutex> lock(frameMutex);
        if (frameBuffer.empty())
            return 0;
        std::memcpy(out, frameBuffer.data(), frameBuffer.size());
        return (int)frameBuffer.size();
    }

    PlayerStruct(const PlayerStruct &) = delete;
    PlayerStruct &operator=(const PlayerStruct &) = delete;
    PlayerStruct(PlayerStruct &&) = delete;
    PlayerStruct &operator=(PlayerStruct &&) = delete;
};
