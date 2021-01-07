//
// Created by enlogy chen on 12/30/20.
//

#include "RecordBuffer.h"

RecordBuffer::~RecordBuffer() {
    for (int i = 0; i < 2; ++i) {
        delete buffer[i];
    }
}

RecordBuffer::RecordBuffer(int bufferSize) {
    buffer = new short *[2];
    for (int i = 0; i < 2; ++i) {
        buffer[i] = new short[bufferSize];
    }
}

short *RecordBuffer::getRecordBuffer() {
    index++;
    if(index>1){
        index = 1;
    }
    return buffer[index];
}

short *RecordBuffer::getNowBuffer() {
    return buffer[index];
}
