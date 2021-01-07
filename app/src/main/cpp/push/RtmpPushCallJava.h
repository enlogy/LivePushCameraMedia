//
// Created by enlogy chen on 1/4/21.
//

#ifndef LIVEPUSHCAMERAMEDIA_RTMPPUSHCALLJAVA_H
#define LIVEPUSHCAMERAMEDIA_RTMPPUSHCALLJAVA_H

#include "../../../../../../../../Library/Android/sdk/ndk/21.0.6113669/toolchains/llvm/prebuilt/darwin-x86_64/sysroot/usr/include/jni.h"
#define RTMP_THREAD_MAIN 1
#define RTMP_THREAD_CHILD 2
/**
 * RTMP状态回调类
 */
class RtmpPushCallJava {
public:
    JNIEnv *jniEnv = NULL;
    JavaVM *jvm = NULL;
    jobject jobj;
    jmethodID jmi_connecting;
    jmethodID jmi_connection_success;
    jmethodID jmi_connection_fail;
public:
    RtmpPushCallJava(JNIEnv *jniEnv, JavaVM *jvm, jobject *jobj);
    ~RtmpPushCallJava();
    //对应java的RTMP状态接受接口
    //fun onConnecting()
    //    fun onConnectionSuccess()
    //    fun onConnectFail(msg:String)
    void onConnecting(int type);
    void onConnectionSuccess();
    void onConnectFail(char* msg);
};


#endif //LIVEPUSHCAMERAMEDIA_RTMPPUSHCALLJAVA_H
