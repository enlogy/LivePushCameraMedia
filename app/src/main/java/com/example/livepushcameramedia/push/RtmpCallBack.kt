package com.example.livepushcameramedia.push

interface RtmpCallBack {
    fun onConnecting()
    fun onConnectionSuccess()
    fun onConnectFail(msg:String)
}