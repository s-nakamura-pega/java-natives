#pragma once

#include <iostream>
#include <vlc/vlc.h>
#include <jni.h>
#include <mutex>
#include <vector>
#include <string>
#include <atomic>
#include <cstring>

struct DVDPlayerStruct
{
    libvlc_instance_t *vlc = nullptr;
    libvlc_media_player_t *mp = nullptr;
    libvlc_media_t *media = nullptr;

    JavaVM *jvm = nullptr;
    jobject javaCanvasObj = nullptr;

    std::mutex frameMutex;
    std::vector<uint8_t> frameBuffer;

    int frameWidth = 720;
    int frameHeight = 480;

    std::atomic<bool> decodeReady{false};
    std::atomic<bool> playing{false};

    DVDPlayerStruct(JavaVM *vm, jobject canvasObj)
        : jvm(vm)
    {
        JNIEnv *env = nullptr;
        jvm->AttachCurrentThread((void **)&env, nullptr);
        javaCanvasObj = env->NewGlobalRef(canvasObj);
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
        std::lock_guard<std::mutex> lock(self->frameMutex);
        *planes = self->frameBuffer.data();
        return nullptr;
    }

    static void unlock(void *opaque, void *picture, void *const *planes)
    {
    }

    static void display(void *opaque, void *picture)
    {
        auto *self = static_cast<DVDPlayerStruct *>(opaque);

        JNIEnv *env = nullptr;
        self->jvm->AttachCurrentThread((void **)&env, nullptr);

        jclass cls = env->GetObjectClass(self->javaCanvasObj);
        jmethodID mid = env->GetMethodID(cls, "repaintCallback", "()V");
        env->CallVoidMethod(self->javaCanvasObj, mid);

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

        frameBuffer.resize(frameWidth * frameHeight * 3);

        libvlc_video_set_callbacks(
            mp,
            lock,
            unlock,
            display,
            this);

        libvlc_video_set_format(
            mp,
            "RV24",
            frameWidth,
            frameHeight,
            frameWidth * 3);

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
                    "(II)V");

            env->CallVoidMethod(
                javaCanvasObj,
                mid,
                frameWidth,
                frameHeight);
        }

        decodeReady = true;

        return 1;
    }

    // ==========================
    // stop（完全修正版）
    // ==========================
    void stop()
    {
        playing = false;

        if (mp)
        {
            // 再生停止
            libvlc_media_player_stop(mp);

            // コールバック無効化
            libvlc_video_set_callbacks(mp, nullptr, nullptr, nullptr, nullptr);
            libvlc_video_set_format(mp, nullptr, 0, 0, 0);

            decodeReady = false;

            std::lock_guard<std::mutex> lock(frameMutex);
            frameBuffer.clear();
        }
    }

    void start()
    {
        if (!mp)
            return;

        libvlc_media_player_play(mp);
        playing = true;
    }

    void movePoint(int ms)
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
        }
    }

    bool isDecodeReady() const { return decodeReady; }
    bool isStarted() const { return playing; }

    int getFrame(unsigned char *out)
    {
        std::lock_guard<std::mutex> lock(frameMutex);
        if (frameBuffer.empty())
            return 0;

        std::memcpy(out, frameBuffer.data(), frameBuffer.size());
        return frameBuffer.size();
    }
};
