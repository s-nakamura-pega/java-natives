#pragma once
#include <jni.h>

class JNIObject {
protected:
    JavaVM* vm;

public:
    JNIObject(JavaVM* vm) : vm(vm) {}

    JNIEnv* getEnv() {
        JNIEnv* env = nullptr;
        vm->AttachCurrentThread(reinterpret_cast<void**>(&env), nullptr);
        return env;
    }

    virtual jlong create(JNIEnv* env, jobject obj) = 0;
    virtual void destroy(JNIEnv* env, jobject obj, jlong handle) = 0;
};
