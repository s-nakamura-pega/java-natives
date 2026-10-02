#pragma once

#include <mpv/client.h>
#include <mpv/render.h>

#include <jni.h>

#include <atomic>
#include <cstdint>
#include <cstring>
#include <mutex>
#include <string>
#include <thread>
#include <vector>

struct DVDPlayerStruct
{
    mpv_handle *mpv = nullptr;
    mpv_render_context *renderCtx = nullptr;

    JavaVM *jvm = nullptr;
    jobject javaCanvasObj = nullptr;

    // mpv software renderer 用
    // BGR0: 4 bytes / pixel
    std::vector<uint8_t> renderBuffer;

    // Java TYPE_3BYTE_BGR 用
    // BGR: 3 bytes / pixel
    std::vector<uint8_t> frameBuffer;

    std::mutex frameMutex;

    int frameWidth = 720;
    int frameHeight = 480;

    std::atomic<bool> running{false};
    std::atomic<bool> redrawRequested{false};
    std::atomic<bool> decodeReady{false};
    std::atomic<bool> playing{false};

    std::thread eventThread;

    DVDPlayerStruct(JavaVM *vm, jobject canvasObj)
        : jvm(vm)
    {
        JNIEnv *env = nullptr;

        if (jvm->AttachCurrentThread(
                reinterpret_cast<void **>(&env),
                nullptr) != JNI_OK)
        {
            return;
        }

        javaCanvasObj = env->NewGlobalRef(canvasObj);

        jvm->DetachCurrentThread();

        mpv = mpv_create();
        if (!mpv)
            return;

        /*
         * mpv自身にはウィンドウを作らせない。
         * 描画結果はlibmpv render APIから取得する。
         */
        mpv_set_option_string(mpv, "vo", "libmpv");

        if (mpv_initialize(mpv) < 0)
        {
            mpv_destroy(mpv);
            mpv = nullptr;
            return;
        }

        /*
         * OpenGLではなくsoftware rendererを使用。
         * Swingへ渡すためのCPUメモリへ直接描画する。
         */
        mpv_render_param params[] = {
            {MPV_RENDER_PARAM_API_TYPE,
             const_cast<char *>(MPV_RENDER_API_TYPE_SW)},
            {MPV_RENDER_PARAM_INVALID,
             nullptr}};

        if (mpv_render_context_create(
                &renderCtx,
                mpv,
                params) < 0)
        {

            mpv_terminate_destroy(mpv);
            mpv = nullptr;
            return;
        }

        mpv_render_context_set_update_callback(
            renderCtx,
            onRenderUpdate,
            this);

        running = true;

        eventThread = std::thread([this]()
                                  { eventLoop(); });
    }

    ~DVDPlayerStruct()
    {
        running = false;

        if (mpv)
            mpv_wakeup(mpv);

        if (eventThread.joinable())
            eventThread.join();

        if (renderCtx)
        {
            mpv_render_context_set_update_callback(
                renderCtx,
                nullptr,
                nullptr);

            mpv_render_context_free(renderCtx);
            renderCtx = nullptr;
        }

        if (mpv)
        {
            mpv_terminate_destroy(mpv);
            mpv = nullptr;
        }

        if (javaCanvasObj)
        {
            JNIEnv *env = nullptr;

            if (jvm->AttachCurrentThread(
                    reinterpret_cast<void **>(&env),
                    nullptr) == JNI_OK)
            {

                env->DeleteGlobalRef(javaCanvasObj);
                javaCanvasObj = nullptr;

                jvm->DetachCurrentThread();
            }
        }
    }

    // --------------------------------------------------
    // libmpv -> redraw notification
    // --------------------------------------------------

    static void onRenderUpdate(void *opaque)
    {
        auto *self =
            static_cast<DVDPlayerStruct *>(opaque);

        self->redrawRequested = true;

        if (self->mpv)
            mpv_wakeup(self->mpv);
    }

    // --------------------------------------------------
    // mpv event thread
    // --------------------------------------------------

    void eventLoop()
    {
        while (running)
        {

            if (redrawRequested.exchange(false))
                renderFrame();

            mpv_event *event =
                mpv_wait_event(mpv, 0.05);

            if (!event)
                continue;

            switch (event->event_id)
            {

            case MPV_EVENT_FILE_LOADED:
                initializeFrame();
                decodeReady = true;
                break;

            case MPV_EVENT_END_FILE:
                playing = false;
                break;

            case MPV_EVENT_SHUTDOWN:
                running = false;
                break;

            default:
                break;
            }
        }
    }

    // --------------------------------------------------
    // Frame initialization
    // --------------------------------------------------

    void initializeFrame()
    {
        {
            std::lock_guard<std::mutex> lock(frameMutex);

            renderBuffer.resize(
                static_cast<size_t>(frameWidth) *
                frameHeight * 4);

            frameBuffer.resize(
                static_cast<size_t>(frameWidth) *
                frameHeight * 3);
        }

        callInitCanvas();
    }

    // --------------------------------------------------
    // Render mpv -> BGR0 -> TYPE_3BYTE_BGR
    // --------------------------------------------------

    void renderFrame()
    {
        if (!renderCtx)
            return;

        int size[2] = {
            frameWidth,
            frameHeight};

        int stride =
            frameWidth * 4;

        std::lock_guard<std::mutex> lock(frameMutex);

        if (renderBuffer.empty() ||
            frameBuffer.empty())
        {
            return;
        }

        /*
         * mpvへは4byte/pixelのBGR0を要求。
         *
         * Javaへ直接渡さず、下でBGR24へ変換する。
         */
        mpv_render_param params[] = {
            {MPV_RENDER_PARAM_SW_SIZE,
             size},
            {MPV_RENDER_PARAM_SW_FORMAT,
             const_cast<char *>("bgr0")},
            {MPV_RENDER_PARAM_SW_STRIDE,
             &stride},
            {MPV_RENDER_PARAM_SW_POINTER,
             renderBuffer.data()},
            {MPV_RENDER_PARAM_INVALID,
             nullptr}};

        if (mpv_render_context_render(
                renderCtx,
                params) < 0)
        {
            return;
        }

        /*
         * BGR0
         *
         * B G R 0 | B G R 0 | ...
         *
         * ↓
         *
         * Java BufferedImage.TYPE_3BYTE_BGR
         *
         * B G R | B G R | ...
         */

        const size_t pixelCount =
            static_cast<size_t>(frameWidth) *
            frameHeight;

        for (size_t i = 0; i < pixelCount; ++i)
        {

            const size_t src = i * 4;
            const size_t dst = i * 3;

            frameBuffer[dst] = renderBuffer[src];
            frameBuffer[dst + 1] = renderBuffer[src + 1];
            frameBuffer[dst + 2] = renderBuffer[src + 2];
        }

        /*
         * frameBuffer完成後にJavaへ通知。
         *
         * Java:
         * repaintCallback()
         *   -> getFrame()
         *   -> BufferedImage
         */
        callRepaint();
    }

    // --------------------------------------------------
    // Java callbacks
    // --------------------------------------------------

    void callInitCanvas()
    {
        JNIEnv *env = nullptr;

        if (jvm->AttachCurrentThread(
                reinterpret_cast<void **>(&env),
                nullptr) != JNI_OK)
        {
            return;
        }

        jclass cls =
            env->GetObjectClass(javaCanvasObj);

        if (cls)
        {
            jmethodID mid =
                env->GetMethodID(
                    cls,
                    "initCanvas",
                    "(II)V");

            if (mid)
            {
                env->CallVoidMethod(
                    javaCanvasObj,
                    mid,
                    frameWidth,
                    frameHeight);
            }

            env->DeleteLocalRef(cls);
        }

        jvm->DetachCurrentThread();
    }

    void callRepaint()
    {
        JNIEnv *env = nullptr;

        if (jvm->AttachCurrentThread(
                reinterpret_cast<void **>(&env),
                nullptr) != JNI_OK)
        {
            return;
        }

        jclass cls =
            env->GetObjectClass(javaCanvasObj);

        if (cls)
        {
            jmethodID mid =
                env->GetMethodID(
                    cls,
                    "repaintCallback",
                    "()V");

            if (mid)
                env->CallVoidMethod(
                    javaCanvasObj,
                    mid);

            env->DeleteLocalRef(cls);
        }

        jvm->DetachCurrentThread();
    }

    // --------------------------------------------------
    // DVD load
    // --------------------------------------------------

    long setFile(const char *path)
    {
        if (!mpv)
            return -1;

        decodeReady = false;
        playing = false;

        {
            std::lock_guard<std::mutex> lock(frameMutex);

            renderBuffer.clear();
            frameBuffer.clear();
        }

        /*
         * dvd-device はloadfileより先に設定する。
         */
        if (mpv_set_property_string(
                mpv,
                "dvd-device",
                path) < 0)
        {
            return -1;
        }

        const char *cmd[] = {
            "loadfile",
            "dvd://",
            "replace",
            nullptr};

        if (mpv_command(
                mpv,
                cmd) < 0)
        {
            return -1;
        }

        /*
         * Javaデモ側では
         *
         * length > 0
         *
         * を成功判定にしているため、
         * DVDでは未知のdurationを仮に1として返す。
         *
         * 実際のduration取得を実装する場合は
         * durationプロパティからmsを返せばよい。
         */
        return 1;
    }

    // --------------------------------------------------
    // Playback
    // --------------------------------------------------

    void start()
    {
        if (!mpv || !decodeReady)
            return;

        mpv_set_property_string(
            mpv,
            "pause",
            "no");

        playing = true;
    }

    void stop()
    {
        if (!mpv)
            return;

        /*
         * Java API上の stop は
         * 再開可能な停止として扱う。
         */
        mpv_set_property_string(
            mpv,
            "pause",
            "yes");

        playing = false;
    }

    // --------------------------------------------------
    // Seek
    // --------------------------------------------------

    void movePoint(int ms)
    {
        if (!mpv)
            return;

        std::string seconds =
            std::to_string(
                static_cast<double>(ms) /
                1000.0);

        const char *cmd[] = {
            "seek",
            seconds.c_str(),
            "absolute",
            nullptr};

        mpv_command(mpv, cmd);
    }

    void skip(bool forward)
    {
        if (!mpv)
            return;

        const char *cmd[] = {
            "seek",
            forward ? "10" : "-10",
            "relative",
            nullptr};

        mpv_command(mpv, cmd);
    }

    // --------------------------------------------------
    // DVD navigation
    // --------------------------------------------------

    void sendKey(int key)
    {
        if (!mpv)
            return;

        const char *name = nullptr;

        switch (key)
        {
        case 37:
            name = "LEFT";
            break;

        case 39:
            name = "RIGHT";
            break;

        case 38:
            name = "UP";
            break;

        case 40:
            name = "DOWN";
            break;

        case 10:
            name = "ENTER";
            break;

        default:
            return;
        }

        const char *cmd[] = {
            "keypress",
            name,
            nullptr};

        mpv_command(mpv, cmd);
    }

    // --------------------------------------------------
    // State
    // --------------------------------------------------

    bool isDecodeReady() const
    {
        return decodeReady;
    }

    bool isStarted() const
    {
        return playing;
    }

    // --------------------------------------------------
    // Swing frame transfer
    // --------------------------------------------------

    int getFrame(unsigned char *out)
    {
        std::lock_guard<std::mutex> lock(frameMutex);

        if (frameBuffer.empty())
            return 0;

        const size_t size =
            static_cast<size_t>(frameWidth) *
            frameHeight * 3;

        std::memcpy(
            out,
            frameBuffer.data(),
            size);

        return static_cast<int>(size);
    }

    DVDPlayerStruct(
        const DVDPlayerStruct &) = delete;

    DVDPlayerStruct &
    operator=(
        const DVDPlayerStruct &) = delete;
};