#pragma once

#include <iostream>
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

    std::mutex frameMutex;
    std::vector<uint8_t> frameBuffer;

    int frameWidth = 1920;
    int frameHeight = 1080;

    int dataSize = frameWidth * frameHeight * 3;
    // byte[]生成
    jbyteArray byteArray = nullptr;

    std::atomic<bool> decodeReady{false};
    std::atomic<bool> playing{false};

    DVDPlayerStruct(JavaVM *vm, jobject canvasObj)
        : jvm(vm)
    {
        JNIEnv *env = nullptr;
        jvm->AttachCurrentThread((void **)&env, nullptr);
        javaCanvasObj = env->NewGlobalRef(canvasObj);
        jbyteArray localArray = env->NewByteArray(dataSize);
        byteArray = (jbyteArray)env->NewGlobalRef(localArray);
        env->DeleteLocalRef(localArray);
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
            env->DeleteGlobalRef(byteArray);
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

        JNIEnv *env = nullptr;
        self->jvm->AttachCurrentThread((void **)&env, nullptr);

        jclass cls = env->GetObjectClass(self->javaCanvasObj);
        jmethodID displayMid = env->GetMethodID(cls, "display", "([B)V");
        env->SetByteArrayRegion(
            self->byteArray,
            0,
            self->dataSize,
            reinterpret_cast<jbyte *>(self->frameBuffer.data()));
        env->CallVoidMethod(self->javaCanvasObj, displayMid, self->byteArray);

        long currentMs = libvlc_media_player_get_time(self->mp);
        jmethodID setPointMid = env->GetMethodID(cls, "setCurrentPoint", "(J)V");
        env->CallVoidMethod(self->javaCanvasObj, setPointMid, (jlong)currentMs);

        self->jvm->DetachCurrentThread();
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

        frameBuffer.resize(frameWidth * frameHeight * 3);

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
                    "(II)V");

            env->CallVoidMethod(
                javaCanvasObj,
                mid,
                frameWidth,
                frameHeight);
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
        }
    }

    bool isDecodeReady() const { return decodeReady; }
    bool isStarted() const { return playing; }
};