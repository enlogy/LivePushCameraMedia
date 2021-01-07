package com.example.livepushcameramedia.push

class RTMPPush {
    companion object{
        init {
            System.loadLibrary("push-lib")
        }
    }
    var rtmpCallBack:RtmpCallBack? = null
    fun initLivePush(pushUrl:String){
        if(pushUrl.isNotEmpty())
        initPush(pushUrl)
    }
    private fun onConnecting(){
        rtmpCallBack?.onConnecting()
    }
    private fun onConnectionSuccess(){
        rtmpCallBack?.onConnectionSuccess()
    }
    private fun onConnectFail(msg:String){
        rtmpCallBack?.onConnectFail(msg)
    }

    fun pushSPSPPS(sps: ByteArray,pps: ByteArray){
        if (sps.isNotEmpty()&&pps.isNotEmpty())
        pushSPSAndPPS(sps,sps.size,pps,pps.size)
    }

    fun pushVideoData(data:ByteArray,keyFrame:Boolean){
        if (data.isNotEmpty())
            pushVideoData(data,data.size,keyFrame)
    }

    fun pushAudioData(data:ByteArray){
        if (data.isNotEmpty())
            pushAudioData(data,data.size)
    }

    fun stopLivePush(){
        stopPush()
    }

    private external fun pushSPSAndPPS(sps:ByteArray,sps_len:Int,pps:ByteArray,pps_len:Int)
    private external fun pushVideoData(data:ByteArray,data_len:Int,keyFrame:Boolean)
    private external fun pushAudioData(data:ByteArray,data_len:Int)
    private external fun stopPush();
    private external fun initPush(pushUrl:String)
}