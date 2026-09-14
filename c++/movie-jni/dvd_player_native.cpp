#include <jni.h>
#include "jniobject.hpp"
#include "dvd_native_structs.hpp"
#include <iostream>

extern JavaVM *g_vm;

// ===============================
// DVDPlayerNative クラス
// ===============================
class DVDPlayerNative : public JNIObject
{
public:
  DVDPlayerNative() : JNIObject(g_vm) {}

  jlong create(JNIEnv *env, jobject obj) override
  {
    JavaVM *vm = nullptr;
    env->GetJavaVM(&vm);
    return reinterpret_cast<jlong>(new DVDPlayerStruct(vm, obj));
  }

  void destroy(JNIEnv *env, jobject obj, jlong handle) override
  {
    delete reinterpret_cast<DVDPlayerStruct *>(handle);
  }
};

extern "C"
{

  // ===============================
  // DVDCanvas.create()
  // ===============================
  JNIEXPORT jlong JNICALL Java_sn_tools_natives_swing_canvas_DVDCanvas_create(JNIEnv *env, jobject obj)
  {
    JavaVM *vm = nullptr;
    env->GetJavaVM(&vm);

    auto *p = new DVDPlayerStruct(vm, obj);
    return reinterpret_cast<jlong>(p);
  }

  // ===============================
  // DVDCanvas.destroy()
  // ===============================
  JNIEXPORT void JNICALL Java_sn_tools_natives_swing_canvas_DVDCanvas_destroy(JNIEnv *env, jobject obj)
  {
    jclass cls = env->GetObjectClass(obj);
    jmethodID mid = env->GetMethodID(cls, "getHandleId", "()J");
    jlong handle = env->CallLongMethod(obj, mid);

    if (handle == 0)
      return;

    auto *player = reinterpret_cast<DVDPlayerStruct *>(handle);
    delete player;

    jmethodID setMid = env->GetMethodID(cls, "setHandleId", "(J)V");
    env->CallVoidMethod(obj, setMid, (jlong)0);

    std::cout << "[native] destroy()" << std::endl;
  }

  // ===============================
  // DVDCanvas.setFile(String)
  // ===============================
  JNIEXPORT jlong JNICALL Java_sn_tools_natives_swing_canvas_DVDCanvas_setFile(JNIEnv *env, jobject obj, jstring path)
  {
    jclass cls = env->GetObjectClass(obj);
    jmethodID mid = env->GetMethodID(cls, "getHandleId", "()J");
    jlong handle = env->CallLongMethod(obj, mid);

    if (handle == 0)
      return 0;

    auto *player = reinterpret_cast<DVDPlayerStruct *>(handle);

    const char *cpath = env->GetStringUTFChars(path, nullptr);
    std::cout << "[native] setFile: " << cpath << std::endl;

    long length = player->setFile(cpath);

    env->ReleaseStringUTFChars(path, cpath);

    return length;
  }

  // ===============================
  // DVDCanvas.start()
  // ===============================
  JNIEXPORT void JNICALL Java_sn_tools_natives_swing_canvas_DVDCanvas_start(JNIEnv *env, jobject obj)
  {
    jclass cls = env->GetObjectClass(obj);
    jmethodID mid = env->GetMethodID(cls, "getHandleId", "()J");
    jlong handle = env->CallLongMethod(obj, mid);

    if (handle == 0)
      return;

    auto *player = reinterpret_cast<DVDPlayerStruct *>(handle);

    std::cout << "[native] start()" << std::endl;
    player->start();
  }

  // ===============================
  // DVDCanvas.stop()
  // ===============================
  JNIEXPORT void JNICALL Java_sn_tools_natives_swing_canvas_DVDCanvas_stop(JNIEnv *env, jobject obj)
  {
    jclass cls = env->GetObjectClass(obj);
    jmethodID mid = env->GetMethodID(cls, "getHandleId", "()J");
    jlong handle = env->CallLongMethod(obj, mid);

    if (handle == 0)
      return;

    auto *player = reinterpret_cast<DVDPlayerStruct *>(handle);

    std::cout << "[native] stop()" << std::endl;
    player->stop(); // ★ pause() ではなく stop() が正しい
  }

  // ===============================
  // DVDCanvas.movePoint(int)
  // ===============================
  JNIEXPORT void JNICALL Java_sn_tools_natives_swing_canvas_DVDCanvas_movePoint(JNIEnv *env, jobject obj, jint ms)
  {
    jclass cls = env->GetObjectClass(obj);
    jmethodID mid = env->GetMethodID(cls, "getHandleId", "()J");
    jlong handle = env->CallLongMethod(obj, mid);

    if (handle == 0)
      return;

    auto *player = reinterpret_cast<DVDPlayerStruct *>(handle);

    std::cout << "[native] movePoint: " << ms << std::endl;
    player->movePoint(ms);
  }

  // ===============================
  // DVDCanvas.skip(boolean)
  // ===============================
  JNIEXPORT void JNICALL Java_sn_tools_natives_swing_canvas_DVDCanvas_skip(JNIEnv *env, jobject obj, jboolean forward)
  {
    jclass cls = env->GetObjectClass(obj);
    jmethodID mid = env->GetMethodID(cls, "getHandleId", "()J");
    jlong handle = env->CallLongMethod(obj, mid);

    if (handle == 0)
      return;

    auto *player = reinterpret_cast<DVDPlayerStruct *>(handle);

    std::cout << "[native] skip: " << forward << std::endl;
    player->skip(forward == JNI_TRUE);
  }

  // ===============================
  // DVDCanvas.sendKey(int)
  // ===============================
  JNIEXPORT void JNICALL Java_sn_tools_natives_swing_canvas_DVDCanvas_sendKey(JNIEnv *env, jobject obj, jint key)
  {
    jclass cls = env->GetObjectClass(obj);
    jmethodID mid = env->GetMethodID(cls, "getHandleId", "()J");
    jlong handle = env->CallLongMethod(obj, mid);

    if (handle == 0)
      return;

    auto *player = reinterpret_cast<DVDPlayerStruct *>(handle);

    std::cout << "[native] sendKey: " << key << std::endl;
    player->sendKey(key);
  }

  // ===============================
  // DVDCanvas.isDecodeReady()
  // ===============================
  JNIEXPORT jboolean JNICALL Java_sn_tools_natives_swing_canvas_DVDCanvas_isDecodeReady(JNIEnv *env, jobject obj)
  {
    jclass cls = env->GetObjectClass(obj);
    jmethodID mid = env->GetMethodID(cls, "getHandleId", "()J");
    jlong handle = env->CallLongMethod(obj, mid);

    if (handle == 0)
      return JNI_FALSE;

    auto *player = reinterpret_cast<DVDPlayerStruct *>(handle);

    return player->isDecodeReady() ? JNI_TRUE : JNI_FALSE;
  }

  // ===============================
  // DVDCanvas.isStarted()
  // ===============================
  JNIEXPORT jboolean JNICALL Java_sn_tools_natives_swing_canvas_DVDCanvas_isStarted(JNIEnv *env, jobject obj)
  {
    jclass cls = env->GetObjectClass(obj);
    jmethodID mid = env->GetMethodID(cls, "getHandleId", "()J");
    jlong handle = env->CallLongMethod(obj, mid);

    if (handle == 0)
      return JNI_FALSE;

    auto *player = reinterpret_cast<DVDPlayerStruct *>(handle);

    return player->isStarted() ? JNI_TRUE : JNI_FALSE;
  }

  // ===============================
  // DVDCanvas.getFrame(byte[])
  // ===============================
  JNIEXPORT jint JNICALL Java_sn_tools_natives_swing_canvas_DVDCanvas_getFrame(JNIEnv *env, jobject obj, jbyteArray buffer)
  {
    jclass cls = env->GetObjectClass(obj);
    jmethodID mid = env->GetMethodID(cls, "getHandleId", "()J");
    jlong handle = env->CallLongMethod(obj, mid);

    if (handle == 0)
      return 0;

    auto *player = reinterpret_cast<DVDPlayerStruct *>(handle);

    jbyte *buf = env->GetByteArrayElements(buffer, nullptr);

    int size = player->getFrame(reinterpret_cast<unsigned char *>(buf));

    env->ReleaseByteArrayElements(buffer, buf, 0);

    return size;
  }

} // extern "C"
