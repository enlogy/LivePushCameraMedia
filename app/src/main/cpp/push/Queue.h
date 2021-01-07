//
// Created by enlogy chen on 1/4/21.
//

#ifndef LIVEPUSHCAMERAMEDIA_QUEUE_H
#define LIVEPUSHCAMERAMEDIA_QUEUE_H

#include "queue"
#include "pthread.h"
#include "../AndroidLog.h"

extern "C"{
#include "../librtmp/rtmp.h"
};
class Queue {
public:
    //rtmp数据包队列
    std::queue<RTMPPacket *>queuePacket;
    //互斥锁
    pthread_mutex_t mutexPacket;
    //通知变量
    pthread_cond_t condPacket;
public:
    Queue();
    ~Queue();

    //入队
    int putRTMPPacket(RTMPPacket *packet);

    //返回RTMPPacket数据包指针
    RTMPPacket* getRTMPPacket();

    //清空队列
    void clearQueue();

    //通知队列,防止阻塞
    void notifyQueue();
};


#endif //LIVEPUSHCAMERAMEDIA_QUEUE_H
