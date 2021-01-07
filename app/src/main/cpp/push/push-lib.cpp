//
// Created by enlogy chen on 1/4/21.
//

#include <jni.h>
#include <string>
#include "RtmpPush.h"
#include "RtmpPushCallJava.h"
#include "../AndroidLog.h"

JavaVM *javaVM = NULL;
RtmpPush *rtmpPush = NULL;
bool pushExit = false;
RtmpPushCallJava *callJava = NULL;
extern "C"
JNIEXPORT void JNICALL
Java_com_example_livepushcameramedia_push_RTMPPush_initPush(JNIEnv *env, jobject thiz,
                                                            jstring push_url) {
    if(rtmpPush == NULL){
        pushExit = false;
        const char* url = env->GetStringUTFChars(push_url,0);
        callJava = new RtmpPushCallJava(env,javaVM,&thiz);
        rtmpPush = new RtmpPush(url, callJava);
        rtmpPush->init();
        env->ReleaseStringUTFChars(push_url, url);
    }
}

/**
 * System.loadlibary时会被调用
 */
extern "C"
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved)
{
        javaVM = vm;
        JNIEnv* env;
        if (vm->GetEnv((void**)&env, JNI_VERSION_1_4) != JNI_OK)
        {
                        LOGE("GetEnv failed!");
                return -1;
        }
        return JNI_VERSION_1_4;
}

extern "C"
JNIEXPORT void JNICALL JNI_OnUnload(JavaVM* vm, void* reserved){
        javaVM = NULL;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_livepushcameramedia_push_RTMPPush_pushSPSAndPPS(JNIEnv *env, jobject thiz,
                                                                 jbyteArray sps_, jint sps_len,
                                                                 jbyteArray pps_, jint pps_len) {
    if (rtmpPush != NULL){
        //jbyteArray转成jbyte指针数组，sps指针指向一个数组,很关键，没有这一步memcpy会报错
        jbyte *sps = env->GetByteArrayElements(sps_, NULL);
        jbyte *pps = env->GetByteArrayElements(pps_, NULL);

        rtmpPush->pushSPSPPS(reinterpret_cast<char *>(sps), sps_len, reinterpret_cast<char *>(pps), pps_len);

        env->ReleaseByteArrayElements(sps_, sps, 0);
        env->ReleaseByteArrayElements(pps_, pps, 0);
    }

}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_livepushcameramedia_push_RTMPPush_pushVideoData(JNIEnv *env, jobject thiz,
                                                                 jbyteArray data_,jint data_len,jboolean keyFrame) {

    if (rtmpPush != NULL && !pushExit){
        jbyte *data = env->GetByteArrayElements(data_, NULL);
        rtmpPush->pushVideoData(reinterpret_cast<char *>(data), data_len, keyFrame);
        env->ReleaseByteArrayElements(data_, data, 0);
    }

}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_livepushcameramedia_push_RTMPPush_pushAudioData(JNIEnv *env, jobject thiz,
                                                                 jbyteArray data_,jint data_len) {
    if (rtmpPush != NULL && !pushExit){
        jbyte *data = env->GetByteArrayElements(data_, NULL);
        rtmpPush->pushAudioData(reinterpret_cast<char *>(data), data_len);
        env->ReleaseByteArrayElements(data_, data, 0);
    }

}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_livepushcameramedia_push_RTMPPush_stopPush(JNIEnv *env, jobject thiz) {
    if (rtmpPush != NULL){
        pushExit = true;
        rtmpPush->stopPush();
        delete(rtmpPush);
        delete(callJava);
        rtmpPush = NULL;
        callJava = NULL;
    }

}