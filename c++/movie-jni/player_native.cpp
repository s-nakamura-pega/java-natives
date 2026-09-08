#include <jni.h>
#include "jniobject.hpp"
#include "native_structs.hpp"
#include <iostream>

extern JavaVM *g_vm;

class PlayerNative : public JNIObject
{
public:
  PlayerNative() : JNIObject(g_vm) {}

  jlong create(JNIEnv *env, jobject obj) override
  {
    JavaVM *vm = nullptr;
    env->GetJavaVM(&vm);
    return reinterpret_cast<jlong>(new PlayerStruct(vm, obj));
  }

  void destroy(JNIEnv *env, jobject obj, jlong handle) override
  {
    delete reinterpret_cast<PlayerStruct *>(handle);
  }
};

extern "C"
{

  // MovieCanvas.create()
  JNIEXPORT jlong JNICALL Java_sn_tools_natives_swing_canvas_MovieCanvas_create(JNIEnv *env, jobject obj)
  {

    JavaVM *vm = nullptr;
    env->GetJavaVM(&vm);

    PlayerStruct *p = new PlayerStruct(vm, obj);
    return reinterpret_cast<jlong>(p);
  }

  // MovieCanvas.destroy()
  JNIEXPORT void JNICALL Java_sn_tools_natives_swing_canvas_MovieCanvas_destroy(JNIEnv *env, jobject obj)
  {

    jclass cls = env->GetObjectClass(obj);
    jmethodID mid = env->GetMethodID(cls, "getHandleId", "()J");
    jlong handle = env->CallLongMethod(obj, mid);

    if (handle == 0)
      return;

    PlayerStruct *player = reinterpret_cast<PlayerStruct *>(handle);
    // ★ グローバル参照の削除は PlayerStruct のデストラクタ側でやる
    delete player;
    jmethodID setMid = env->GetMethodID(cls, "setHandleId", "(J)V");
    env->CallVoidMethod(obj, setMid, (jlong)0);
    std::cout << "[native] destroy()" << std::endl;
  }

  // MovieCanvas.setFile(String)
  JNIEXPORT jlong JNICALL Java_sn_tools_natives_swing_canvas_MovieCanvas_setFile(JNIEnv *env, jobject obj, jstring path)
  {

    jclass cls = env->GetObjectClass(obj);
    jmethodID mid = env->GetMethodID(cls, "getHandleId", "()J");
    jlong handle = env->CallLongMethod(obj, mid);

    if (handle == 0)
      return 0;

    PlayerStruct *player = reinterpret_cast<PlayerStruct *>(handle);

    const char *cpath = env->GetStringUTFChars(path, nullptr);
    std::cout << "[native] setFile: " << cpath << std::endl;

    long length = player->setFile(cpath);

    env->ReleaseStringUTFChars(path, cpath);

    return length;
  }

  // MovieCanvas.start()
  JNIEXPORT void JNICALL Java_sn_tools_natives_swing_canvas_MovieCanvas_start(JNIEnv *env, jobject obj)
  {

    jclass cls = env->GetObjectClass(obj);
    jmethodID mid = env->GetMethodID(cls, "getHandleId", "()J");
    jlong handle = env->CallLongMethod(obj, mid);

    if (handle == 0)
      return;

    PlayerStruct *player = reinterpret_cast<PlayerStruct *>(handle);

    std::cout << "[native] start()" << std::endl;
    player->start();
  }

  // MovieCanvas.stop()
  JNIEXPORT void JNICALL Java_sn_tools_natives_swing_canvas_MovieCanvas_stop(JNIEnv *env, jobject obj)
  {

    jclass cls = env->GetObjectClass(obj);
    jmethodID mid = env->GetMethodID(cls, "getHandleId", "()J");
    jlong handle = env->CallLongMethod(obj, mid);

    if (handle == 0)
      return;

    PlayerStruct *player = reinterpret_cast<PlayerStruct *>(handle);

    std::cout << "[native] stop()" << std::endl;

    player->pause();
  }

  // MovieCanvas.movePoint(int)
  JNIEXPORT void JNICALL Java_sn_tools_natives_swing_canvas_MovieCanvas_movePoint(JNIEnv *env, jobject obj, jint ms)
  {

    jclass cls = env->GetObjectClass(obj);
    jmethodID mid = env->GetMethodID(cls, "getHandleId", "()J");
    jlong handle = env->CallLongMethod(obj, mid);

    if (handle == 0)
      return;

    PlayerStruct *player = reinterpret_cast<PlayerStruct *>(handle);

    std::cout << "[native] movePoint: " << ms << std::endl;
    player->movePoint(ms);
  }

  // MovieCanvas.isDecodeReady()
  JNIEXPORT jboolean JNICALL Java_sn_tools_natives_swing_canvas_MovieCanvas_isDecodeReady(JNIEnv *env, jobject obj)
  {

    jclass cls = env->GetObjectClass(obj);
    jmethodID mid = env->GetMethodID(cls, "getHandleId", "()J");
    jlong handle = env->CallLongMethod(obj, mid);

    if (handle == 0)
      return JNI_FALSE;

    PlayerStruct *player = reinterpret_cast<PlayerStruct *>(handle);

    return player->isDecodeReady() ? JNI_TRUE : JNI_FALSE;
  }

  // MovieCanvas.isStarted()
  JNIEXPORT jboolean JNICALL Java_sn_tools_natives_swing_canvas_MovieCanvas_isStarted(JNIEnv *env, jobject obj)
  {

    jclass cls = env->GetObjectClass(obj);
    jmethodID mid = env->GetMethodID(cls, "getHandleId", "()J");
    jlong handle = env->CallLongMethod(obj, mid);

    if (handle == 0)
      return JNI_FALSE;

    PlayerStruct *player = reinterpret_cast<PlayerStruct *>(handle);

    return player->isStarted() ? JNI_TRUE : JNI_FALSE;
  }

  // MovieCanvas.getFrame(byte[])
  JNIEXPORT jint JNICALL Java_sn_tools_natives_swing_canvas_MovieCanvas_getFrame(JNIEnv *env, jobject obj, jbyteArray buffer)
  {

    jclass cls = env->GetObjectClass(obj);
    jmethodID mid = env->GetMethodID(cls, "getHandleId", "()J");
    jlong handle = env->CallLongMethod(obj, mid);

    if (handle == 0)
      return 0;

    PlayerStruct *player = reinterpret_cast<PlayerStruct *>(handle);

    jbyte *buf = env->GetByteArrayElements(buffer, nullptr);

    int size = player->getFrame(reinterpret_cast<unsigned char *>(buf));

    env->ReleaseByteArrayElements(buffer, buf, 0);

    return size;
  }
}
