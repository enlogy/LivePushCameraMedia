#include <jni.h>
#include <string>
#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>
#include "RecordBuffer.h"
//#include "AndroidLog.h"

SLObjectItf slEngineObjectItf = NULL;
SLEngineItf slEngineItf = NULL;

SLObjectItf slRecordObjectItf = NULL;
SLRecordItf slRecordItf = NULL;

SLAndroidSimpleBufferQueueItf recordBufferQueue = NULL;
RecordBuffer *recordBuffer;

FILE *pcmFile = NULL;

bool finish = false;

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_livepushcameramedia_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

/**
 * pcm数据处理后回调
 * @param caller
 * @param pContext
 */
void bufferQueueCallback(SLAndroidSimpleBufferQueueItf caller,
                         void *pContext){
        fwrite(recordBuffer->getNowBuffer(),1,4096,pcmFile);
        if (finish){
//            LOGE("录制完成");
            //关闭录音
            (*slRecordItf) -> SetRecordState(slRecordItf,SL_RECORDSTATE_STOPPED);
            (*slEngineObjectItf) -> Destroy(slEngineObjectItf);
            slEngineObjectItf = NULL;
            slEngineItf = NULL;
            (*slRecordObjectItf)->Destroy(slRecordObjectItf);
            slRecordObjectItf = NULL;
            slRecordItf = NULL;
            delete(recordBuffer);
        } else{
//            LOGE("正在录制");
            //入队pcm数据处理后回调bufferQueueCallback
            (*recordBufferQueue) -> Enqueue(recordBufferQueue,recordBuffer->getRecordBuffer(),4096);
        }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_livepushcameramedia_opensles_OpenSLESActivity_startRecord(JNIEnv *env,
                                                                           jobject thiz,
                                                                           jstring pcm_path) {
    finish = false;
    const char *path = env -> GetStringUTFChars(pcm_path,0);
    pcmFile = fopen(path,"w");

    //创建OpenSLES引擎
    slCreateEngine(&slEngineObjectItf, 0, NULL, 0, NULL, NULL);
    (*slEngineObjectItf) -> Realize(slEngineObjectItf, SL_BOOLEAN_FALSE);
    (*slEngineObjectItf) -> GetInterface(slEngineObjectItf, SL_IID_ENGINE, &slEngineItf);

    //创建录音器AudioRecord
    SLDataLocator_IODevice loc_dev = {SL_DATALOCATOR_IODEVICE,
                                      SL_IODEVICE_AUDIOINPUT,
                                      SL_DEFAULTDEVICEID_AUDIOINPUT,
                                      NULL};
    SLDataSource audioSrc = {&loc_dev, NULL};
    SLDataLocator_AndroidSimpleBufferQueue loc_bq = {
            SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE,
            2
    };
    SLDataFormat_PCM format_pcm = {
            SL_DATAFORMAT_PCM, 2, SL_SAMPLINGRATE_44_1,
            SL_PCMSAMPLEFORMAT_FIXED_16, SL_PCMSAMPLEFORMAT_FIXED_16,
            SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT, SL_BYTEORDER_LITTLEENDIAN
    };
    SLDataSink audioSnk = {&loc_bq, &format_pcm};
    const SLInterfaceID id[1] = {SL_IID_ANDROIDSIMPLEBUFFERQUEUE};
    const SLboolean req[1] = {SL_BOOLEAN_TRUE};
    (*slEngineItf) -> CreateAudioRecorder(slEngineItf,&slRecordObjectItf,&audioSrc, &audioSnk, 1, id, req);
    (*slRecordObjectItf) -> Realize(slRecordObjectItf,SL_BOOLEAN_FALSE);
    //获取AudioRecord对象slRecordItf
    (*slRecordObjectItf) -> GetInterface(slRecordObjectItf,SL_IID_RECORD,&slRecordItf);
    //CREATE ANDROID SIMPLE BUFFER QUEUE
    (*slRecordObjectItf) -> GetInterface(slRecordObjectItf,SL_IID_ANDROIDSIMPLEBUFFERQUEUE,&recordBufferQueue);

    recordBuffer = new RecordBuffer(4096);
    //入队,设置队列大小
    (*recordBufferQueue) -> Enqueue(recordBufferQueue,recordBuffer->getRecordBuffer(),4096);
    //设置监听
    (*recordBufferQueue) -> RegisterCallback(recordBufferQueue,bufferQueueCallback,NULL);
    //开启录音
    (*slRecordItf) -> SetRecordState(slRecordItf,SL_RECORDSTATE_RECORDING);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_livepushcameramedia_opensles_OpenSLESActivity_stopRecord(JNIEnv *env,
                                                                          jobject thiz) {
   finish = true;
}
