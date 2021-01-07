//
// Created by enlogy chen on 1/4/21.
//

#include "RtmpPushCallJava.h"
#include "../../../../../../../../Library/Android/sdk/ndk/21.0.6113669/toolchains/llvm/prebuilt/darwin-x86_64/sysroot/usr/include/c++/v1/cwchar"


RtmpPushCallJava::RtmpPushCallJava(JNIEnv *jniEnv, JavaVM *jvm, jobject *jobj) {
    this->jniEnv = jniEnv;
    this->jvm = jvm;
    this->jobj = jniEnv->NewGlobalRef(*jobj);
    jclass jlz = jniEnv->GetObjectClass(this->jobj);
    jmi_connecting = jniEnv->GetMethodID(jlz,"onConnecting","()V");
    jmi_connection_success = jniEnv->GetMethodID(jlz,"onConnectionSuccess","()V");
    jmi_connection_fail = jniEnv->GetMethodID(jlz,"onConnectFail","(Ljava/lang/String;)V");
}

RtmpPushCallJava::~RtmpPushCallJava() {
    jniEnv->DeleteGlobalRef(jobj);
    jvm = NULL;
    jniEnv = NULL;
}

/**
 * 主线程或者子线程
 * @param type
 */
void RtmpPushCallJava::onConnecting(int type) {
    if(type == RTMP_THREAD_CHILD)
    {
        JNIEnv *jniEnv;
        if(jvm->AttachCurrentThread(&jniEnv, 0) != JNI_OK)
        {
            return;
        }
        jniEnv->CallVoidMethod(jobj, jmi_connecting);
        jvm->DetachCurrentThread();
    }
    else
    {
        jniEnv->CallVoidMethod(jobj, jmi_connecting);
    }
}

/**
 * 在子线程中
 */
void RtmpPushCallJava::onConnectionSuccess() {
    JNIEnv *jniEnv;
    if(jvm->AttachCurrentThread(&jniEnv, 0) != JNI_OK)
    {
        return;
    }
    jniEnv->CallVoidMethod(jobj, jmi_connection_success);
    jvm->DetachCurrentThread();
}

/**
 * 在子线程中
 */
void RtmpPushCallJava::onConnectFail(char *msg) {
    JNIEnv *jniEnv;
    if(jvm->AttachCurrentThread(&jniEnv, 0) != JNI_OK)
    {
        return;
    }

    jstring jmsg = jniEnv->NewStringUTF(msg);

    jniEnv->CallVoidMethod(jobj, jmi_connection_fail, jmsg);

    jniEnv->DeleteLocalRef(jmsg);
    jvm->DetachCurrentThread();
}
