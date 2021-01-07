//
// Created by enlogy chen on 12/30/20.
//

#ifndef LIVEPUSHCAMERAMEDIA_RECORDBUFFER_H
#define LIVEPUSHCAMERAMEDIA_RECORDBUFFER_H


class RecordBuffer {
public:
    //双指针表示一维数组buffer
    short **buffer;
    int index = -1;
public:
    //声明构造方法
    RecordBuffer(int bufferSize);
    ~RecordBuffer();

    //定义方法
    short *getRecordBuffer();
    short *getNowBuffer();
};


#endif //LIVEPUSHCAMERAMEDIA_RECORDBUFFER_H
