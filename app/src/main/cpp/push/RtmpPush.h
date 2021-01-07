//
// Created by enlogy chen on 1/4/21.
//

#ifndef LIVEPUSHCAMERAMEDIA_RTMPPUSH_H
#define LIVEPUSHCAMERAMEDIA_RTMPPUSH_H
extern "C"{
#include "../librtmp/rtmp.h"
}

#include "pthread.h"
#include "Queue.h"
#include "RtmpPushCallJava.h"

class RtmpPush {
public:
    RTMP *rtmp = NULL;
    char *url = NULL;
    Queue* queue = NULL;
    pthread_t push_thread;
    RtmpPushCallJava *callJava = NULL;
    bool startPushing = false;
    long startTime = 0;
public:
    RtmpPush(const char *url, RtmpPushCallJava *pJava);
    ~RtmpPush();
    //初始化
    void init();
    void pushSPSPPS(char *sps,int sps_len,char *pps,int pps_len);
    void pushVideoData(char *data,int data_len, bool keyframe);
    void pushAudioData(char *data,int data_len);
    void stopPush();
};


#endif //LIVEPUSHCAMERAMEDIA_RTMPPUSH_H
