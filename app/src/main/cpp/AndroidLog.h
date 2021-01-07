
#pragma once
#ifndef LIVEPUSHCAMERAMEDIA_ANDROIDLOG_H
#define LIVEPUSHCAMERAMEDIA_ANDROIDLOG_H

#include "android/log.h"
#define LOGD(FORMAT,...) __android_log_print(ANDROID_LOG_DEBUG,"enlogy",FORMAT,##__VA_ARGS__);
#define LOGE(FORMAT,...) __android_log_print(ANDROID_LOG_ERROR,"enlogy",FORMAT,##__VA_ARGS__);

//#define LOG_DEBUG false

#endif//LIVEPUSHCAMERAMEDIA_ANDROIDLOG_H
