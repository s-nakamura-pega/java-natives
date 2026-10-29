#pragma once

#include <iostream>
#include <memory>
#include <vlc/libvlc.h>
#include <vlc/vlc.h>
#include <jni.h>
#include <mutex>
#include <vector>
#include <string>
#include <atomic>

struct DVDPlayerStruct
{
    libvlc_instance_t *vlc = nullptr;
    libvlc_media_player_t *mp = nullptr;
    libvlc_media_t *media = nullptr;

    JavaVM *jvm = nullptr;
    jobject javaCanvasObj = nullptr;
    jmethodID displayMid = nullptr;
    jmethodID setPointMid = nullptr;

    std::mutex frameMutex;
    std::vector<uint8_t> frameBuffer;

    int frameWidth = 1920;
    int frameHeight = 1080;

    int dataSize = frameWidth * frameHeight * 3;
    // ByteBuffer
    jobject byteBuffer = nullptr;

    std::atomic<bool> decodeReady{false};
    std::atomic<bool> playing{false};

    DVDPlayerStruct(JavaVM *vm, jobject canvasObj)
        : jvm(vm)
    {
        frameBuffer.resize(dataSize);
        JNIEnv *env = nullptr;
        jvm->AttachCurrentThread((void **)&env, nullptr);
        javaCanvasObj = env->NewGlobalRef(canvasObj);
        jobject localJobject = env->NewDirectByteBuffer(frameBuffer.data(), dataSize);
        byteBuffer = env->NewGlobalRef(localJobject);
        env->DeleteLocalRef(localJobject);
        jclass cls = env->GetObjectClass(canvasObj);
        displayMid = env->GetMethodID(cls, "display", "(Ljava/nio/ByteBuffer;)V");
        setPointMid = env->GetMethodID(cls, "setCurrentPoint", "(J)V");
        jvm->DetachCurrentThread();

        const char *args[] = {
            "--no-video-title-show",
            "--quiet",
            "--no-sub-autodetect-file"};
        vlc = libvlc_new(3, args);
    }

    ~DVDPlayerStruct()
    {
        stop();

        if (mp)
        {
            libvlc_media_player_release(mp);
            mp = nullptr;
        }
        if (media)
        {
            libvlc_media_release(media);
            media = nullptr;
        }
        if (vlc)
        {
            libvlc_release(vlc);
            vlc = nullptr;
        }

        if (javaCanvasObj)
        {
            JNIEnv *env = nullptr;
            jvm->AttachCurrentThread((void **)&env, nullptr);
            env->DeleteGlobalRef(byteBuffer);
            env->DeleteGlobalRef(javaCanvasObj);
            jvm->DetachCurrentThread();
            javaCanvasObj = nullptr;
        }
    }

    // ==========================
    // libVLC callbacks
    // ==========================
    static void *lock(void *opaque, void **planes)
    {
        auto *self = static_cast<DVDPlayerStruct *>(opaque);
        self->frameMutex.lock();
        *planes = self->frameBuffer.data();
        return nullptr;
    }

    static void unlock(void *opaque, void *picture, void *const *planes)
    {
        auto *self = static_cast<DVDPlayerStruct *>(opaque);
        self->frameMutex.unlock();
    }

    static void display(void *opaque, void *picture)
    {
        auto *self = static_cast<DVDPlayerStruct *>(opaque);

        static thread_local JavaVM *tlsJvm = nullptr;

        static thread_local std::unique_ptr<
            JNIEnv,
            void (*)(JNIEnv *)>
            holder{
                nullptr,
                [](JNIEnv *)
                {
                    if (tlsJvm)
                        tlsJvm->DetachCurrentThread();
                }};

        if (!holder)
        {
            tlsJvm = self->jvm;

            JNIEnv *env = nullptr;

            if (self->jvm->GetEnv((void **)&env, JNI_VERSION_1_8) == JNI_EDETACHED)
            {
                self->jvm->AttachCurrentThread((void **)&env, nullptr);
            }

            holder.reset(env);
        }

        JNIEnv *env = holder.get();

        env->CallVoidMethod(
            self->javaCanvasObj,
            self->displayMid,
            self->byteBuffer);

        long currentMs = libvlc_media_player_get_time(self->mp);

        env->CallVoidMethod(
            self->javaCanvasObj,
            self->setPointMid,
            (jlong)currentMs);
    }

    static void onChapterChanged(const libvlc_event_t *event, void *userData)
    {
        auto *self = static_cast<DVDPlayerStruct *>(userData);
        JNIEnv *env = nullptr;
        self->jvm->AttachCurrentThread((void **)&env, nullptr);

        long durationMs = libvlc_media_player_get_length(self->mp);

        jclass clsLocal = env->GetObjectClass(self->javaCanvasObj);
        jmethodID setPointMid = env->GetMethodID(clsLocal, "changeDuration", "(J)V");
        env->CallVoidMethod(self->javaCanvasObj, setPointMid, (jlong)durationMs);

        self->jvm->DetachCurrentThread();
    }

    // ==========================
    // setFile（完全修正版）
    // ==========================
    long setFile(const char *path)
    {
        decodeReady = false;
        stop();

        if (mp)
        {
            libvlc_media_player_release(mp);
            mp = nullptr;
        }

        if (media)
        {
            libvlc_media_release(media);
            media = nullptr;
        }

        mp = libvlc_media_player_new(vlc);

        libvlc_video_set_callbacks(
            mp,
            lock,
            unlock,
            display,
            this);

        int stride = frameWidth * 3;
        // stride = (stride + 3) & ~3;
        libvlc_video_set_format(
            mp,
            "RV24",
            frameWidth,
            frameHeight,
            stride);

        media = libvlc_media_new_location(
            vlc,
            "dvd://");

        if (!media)
        {
            std::cerr
                << "Failed to create media for DVD."
                << std::endl;
            return -1;
        }

        std::string opt =
            std::string(":dvd-device=") + path;

        libvlc_media_add_option(
            media,
            opt.c_str());

        libvlc_media_player_set_media(
            mp,
            media);

        //
        // Javaへサイズ通知
        //
        {
            JNIEnv *env = nullptr;

            jvm->AttachCurrentThread(
                (void **)&env,
                nullptr);

            jclass cls =
                env->GetObjectClass(
                    javaCanvasObj);

            jmethodID mid =
                env->GetMethodID(
                    cls,
                    "initCanvas",
                    "(IIZ)V");

            env->CallVoidMethod(
                javaCanvasObj,
                mid,
                frameWidth,
                frameHeight,
                true);
            jvm->DetachCurrentThread();
        }

        libvlc_video_set_aspect_ratio(mp, "16:9");
        libvlc_video_set_scale(mp, 1.0f);
        libvlc_video_set_spu(mp, -1);

        libvlc_event_manager_t *eventManager = libvlc_media_player_event_manager(mp);
        libvlc_event_attach(eventManager, libvlc_MediaPlayerLengthChanged, onChapterChanged, this);
        // libvlc_event_attach(eventManager, libvlc_MediaPlayerTitleChanged, onChapterChanged, this);
        // libvlc_event_attach(eventManager, libvlc_MediaPlayerChapterChanged, onChapterChanged, this);

        decodeReady = true;

        return 1;
    }

    // ==========================
    // stop（完全修正版）
    // ==========================
    void stop()
    {
        if (!playing)
            return;

        playing = false;

        if (mp)
        {
            libvlc_media_player_pause(mp);
        }
    }

    void start()
    {
        if (!mp)
            return;

        libvlc_media_player_play(mp);
        playing = true;
    }

    void movePoint(int64_t ms)
    {
        if (!mp)
            return;

        libvlc_media_player_set_time(mp, ms);
    }

    void skip(bool forward)
    {
        if (!mp)
            return;

        int cur = libvlc_media_player_get_time(mp);
        int target = cur + (forward ? 10000 : -10000);
        libvlc_media_player_set_time(mp, target);
    }

    void sendKey(int key)
    {
        if (!mp)
            return;

        switch (key)
        {
        case 37:
            libvlc_media_player_navigate(mp, libvlc_navigate_left);
            break;
        case 39:
            libvlc_media_player_navigate(mp, libvlc_navigate_right);
            break;
        case 38:
            libvlc_media_player_navigate(mp, libvlc_navigate_up);
            break;
        case 40:
            libvlc_media_player_navigate(mp, libvlc_navigate_down);
            break;
        case 10:
            libvlc_media_player_navigate(mp, libvlc_navigate_activate);
            break;
        case 77:
            libvlc_media_player_set_title(mp, 0);
            break;
        case 70:
            skip(true);
            break;
        case 66:
            skip(false);
            break;
        case 32:
            if (playing)
            {
                stop();
            }
            else
            {
                start();
            }
            break;
        }
    }

    void sendMouseEvent(int x, int y, bool isClick)
    {
        // libVLC3では未対応
    }

    bool isDecodeReady() const { return decodeReady; }
    bool isStarted() const { return playing; }
};