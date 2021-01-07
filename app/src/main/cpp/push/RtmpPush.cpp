//
// Created by enlogy chen on 1/4/21.
//

#include <malloc.h>
#include <cstring>
#include "../AndroidLog.h"
#include "RtmpPush.h"
#include "RtmpPushCallJava.h"

RtmpPush::RtmpPush(const char *url, RtmpPushCallJava *pJava) {
    //分配空间
    this->url = static_cast<char *>(malloc(512));
    //拷贝url
    strcpy(this->url,url);
    this->callJava = pJava;
    queue = new Queue();
}

RtmpPush::~RtmpPush() {
    queue->notifyQueue();
    queue->clearQueue();
    //释放url内存
    free(url);
}

/**
 * 在子线程中回调
 * @param data
 * @return
 */
void *callBackPush(void *data){
    RtmpPush *rtmpPush = static_cast<RtmpPush *>(data);
    //分配内存空间
    rtmpPush->rtmp = RTMP_Alloc();
    //初始化RTMP
    RTMP_Init(rtmpPush->rtmp);
    //设置超时10s
    rtmpPush->rtmp->Link.timeout = 10;
    //追加直播标志
    rtmpPush->rtmp->Link.lFlags |= RTMP_LF_LIVE;
    //set url
    RTMP_SetupURL(rtmpPush->rtmp,rtmpPush->url);
    //set 可写
    RTMP_EnableWrite(rtmpPush->rtmp);
    //连接服务器 return 0 连接失败 1连接成功
    if (!RTMP_Connect(rtmpPush->rtmp,NULL)){
        LOGE("连接服务器失败")
        //回调RTMP状态到java
        rtmpPush->callJava->onConnectFail("RTMP Status: 连接服务器失败");
        goto end;
    }
    //连接资源,seekTime在直播中无作用
    if (!RTMP_ConnectStream(rtmpPush->rtmp,0)){
        LOGE("连接数据流失败")
        rtmpPush->callJava->onConnectFail("RTMP Status: 连接数据流失败");
        goto end;
    }
    LOGD("开始推流");
    rtmpPush->callJava->onConnectionSuccess();
    rtmpPush->startPushing = true;
    //记录开始连接的时间
    rtmpPush->startTime = RTMP_GetTime();
    while (true){
        if (!rtmpPush->startPushing){
            break;
        }
        //进行推流
        RTMPPacket *packet = NULL;
        packet = rtmpPush->queue->getRTMPPacket();
        if(packet != NULL){
            int result = RTMP_SendPacket(rtmpPush->rtmp,packet,1);
            //打印结果
            LOGD("RTMP_SendPacket result is %d", result)
            //释放packet
            RTMPPacket_Free(packet);
            free(packet);
            packet = NULL;
        }
    }

    end:
    //关闭和释放资源
    RTMP_Close(rtmpPush->rtmp);
    RTMP_Free(rtmpPush->rtmp);
    rtmpPush->rtmp = NULL;

    //完成后退出当前线程
    pthread_exit(&rtmpPush->push_thread);
}

/**
 * 主线程或者子线程
 */
void RtmpPush::init() {
    LOGD("连接服务器中")
    //回调RTMP状态到java
    callJava->onConnecting(RTMP_THREAD_MAIN);
    //创建线程，回调监听callBackPush
    pthread_create(&push_thread,NULL,callBackPush,this);
}

void RtmpPush::pushSPSPPS(char *sps, int sps_len, char *pps, int pps_len) {
    //16 header 信息的长度
    int bodySize = sps_len + pps_len + 16;
    //把数据封装进RTMPPacket里面进行传输
    RTMPPacket *packet = static_cast<RTMPPacket *>(malloc(sizeof(RTMPPacket)));
    RTMPPacket_Alloc(packet, bodySize);
    RTMPPacket_Reset(packet);

    //一个char类型占8bit，也就是1byte
    //16进制两位一个字节如0x17
    //取出rtmppacket 的body字段
    char *body = packet->m_body;
    //body填充数据
    int i = 0;
    body[i++] = 0x17;
    body[i++] = 0x00;
    body[i++] = 0x00;
    body[i++] = 0x00;
    body[i++] = 0x00;
    //sps+pps数据
    body[i++] = 0x01;
    body[i++] = sps[1];
    body[i++] = sps[2];
    body[i++] = sps[3];
    body[i++] = 0xff;
    //sps
    body[i++] = 0xe1;
    body[i++] = (sps_len >> 8)&0xff;
    body[i++] = sps_len & 0xff;
    memcpy(&body[i], sps, sps_len);
    i += sps_len;
    //pps
    body[i++] = 0x01;
    body[i++] = (pps_len >> 8) & 0xff;
    body[i++] = pps_len & 0xff;
    memcpy(&body[i], pps, pps_len);
    //设置packet相关数据
    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    packet->m_nBodySize = bodySize;
    packet->m_nTimeStamp = 0;
    packet->m_hasAbsTimestamp = 0;
    packet->m_nChannel = 0x04;
    packet->m_headerType = RTMP_PACKET_SIZE_MEDIUM;
    packet->m_nInfoField2 = rtmp->m_stream_id;
    //入队
    queue->putRTMPPacket(packet);

}

//结构体格式要按照文档
void RtmpPush::pushVideoData(char *data,int data_len, bool keyframe) {
    int bodysize = data_len + 9;
    RTMPPacket *packet = static_cast<RTMPPacket *>(malloc(sizeof(RTMPPacket)));
    RTMPPacket_Alloc(packet, bodysize);
    RTMPPacket_Reset(packet);

    char *body = packet->m_body;
    int i = 0;

    if(keyframe)
    {
        body[i++] = 0x17;
    } else{
        body[i++] = 0x27;
    }

    body[i++] = 0x01;
    body[i++] = 0x00;
    body[i++] = 0x00;
    body[i++] = 0x00;

    body[i++] = (data_len >> 24) & 0xff;
    body[i++] = (data_len >> 16) & 0xff;
    body[i++] = (data_len >> 8) & 0xff;
    body[i++] = data_len & 0xff;
    memcpy(&body[i], data, data_len);

    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    packet->m_nBodySize = bodysize;
    packet->m_nTimeStamp = RTMP_GetTime() - startTime;
    packet->m_hasAbsTimestamp = 0;
    packet->m_nChannel = 0x04;
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    packet->m_nInfoField2 = rtmp->m_stream_id;

    queue->putRTMPPacket(packet);
}

void RtmpPush::pushAudioData(char *data, int data_len) {
    int bodysize = data_len + 2;
    RTMPPacket *packet = static_cast<RTMPPacket *>(malloc(sizeof(RTMPPacket)));
    RTMPPacket_Alloc(packet, bodysize);
    RTMPPacket_Reset(packet);
    char *body = packet->m_body;
    body[0] = 0xAF;
    body[1] = 0x01;
    memcpy(&body[2], data, data_len);

    packet->m_packetType = RTMP_PACKET_TYPE_AUDIO;
    packet->m_nBodySize = bodysize;
    packet->m_nTimeStamp = RTMP_GetTime() - startTime;
    packet->m_hasAbsTimestamp = 0;
    packet->m_nChannel = 0x04;
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    packet->m_nInfoField2 = rtmp->m_stream_id;
    queue->putRTMPPacket(packet);
}

void RtmpPush::stopPush() {
    startPushing = false;
    queue->notifyQueue();
    pthread_join(push_thread, NULL);
}



