//
// Created by enlogy chen on 1/4/21.
//

#include "Queue.h"

Queue::Queue() {
    //初始化锁
    pthread_mutex_init(&mutexPacket,NULL);
    pthread_cond_init(&condPacket,NULL);
}
//退出时调用
Queue::~Queue() {
    //注销锁
    pthread_mutex_destroy(&mutexPacket);
    pthread_cond_destroy(&condPacket);
}

int Queue::putRTMPPacket(RTMPPacket *packet) {
    pthread_mutex_lock(&mutexPacket);
    //入队
    queuePacket.push(packet);
    //发送信号
    pthread_cond_signal(&condPacket);
    pthread_mutex_unlock(&mutexPacket);
    return 0;
}

RTMPPacket *Queue::getRTMPPacket() {
    pthread_mutex_lock(&mutexPacket);
    RTMPPacket* p = NULL;
    if (!queuePacket.empty()){
        //获取队列最前面的数据
        p = queuePacket.front();
        //出队，根据业务逻辑需要
        queuePacket.pop();
    } else{
        //释放锁
        pthread_cond_wait(&condPacket,&mutexPacket);
    }
    pthread_mutex_unlock(&mutexPacket);
    return p;
}

void Queue::clearQueue() {
    pthread_mutex_lock(&mutexPacket);
    while (!queuePacket.empty()){
        //出队和释放资源
        RTMPPacket* p = queuePacket.front();
        queuePacket.pop();
        RTMPPacket_Free(p);
        p = NULL;
    }
    pthread_mutex_unlock(&mutexPacket);
}

void Queue::notifyQueue() {
    pthread_mutex_lock(&mutexPacket);
    pthread_cond_signal(&condPacket);
    pthread_mutex_unlock(&mutexPacket);
}
