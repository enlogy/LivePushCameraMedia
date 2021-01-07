package com.example.livepushcameramedia.push

import android.media.MediaFormat
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.util.LogUtils
import com.example.livepushcameramedia.R
import kotlinx.android.synthetic.main.activity_push.*

/**
 * NALU类型计算
 * //0x开头的表示16进制
 * //计算原理：将16进制转化成2进制，再由2进制后五位转化为16进制，得到的数就是NALU类型所表示的东西
 * 例如：SPS：0x000000000167
 *
 */
class PushActivity :AppCompatActivity(){
    var rtmpPush:RTMPPush? = null
    var pushEncoder:PushEncoder? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_push)
//        initPush("rtmp://192.168.1.7/myapp/mystream")
        rtmpPush = RTMPPush()
        rtmpPush!!.rtmpCallBack = object :RtmpCallBack{
            override fun onConnecting() {
                LogUtils.i("onConnecting")
            }

            @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
            override fun onConnectionSuccess() {
                LogUtils.i("onConnectionSuccess")
                //连接成功后开始初始化推流相关编码器
                pushEncoder = PushEncoder(this@PushActivity,mSurfaceView.getCameraTextureId())
                pushEncoder!!.initMediaEncoder(mSurfaceView.getEglContext(),1080/2,1920/2, MediaFormat.MIMETYPE_VIDEO_AVC,
                    MediaFormat.MIMETYPE_AUDIO_AAC,44100,2)

                pushEncoder!!.onMediaInfoListener = object :BasePushEncoder.OnMediaInfoListener{
                    override fun onSPSAndPPSInfo(sps: ByteArray, pps: ByteArray) {
                        rtmpPush!!.pushSPSPPS(sps, pps)
                    }

                    override fun onVideoInfo(data: ByteArray, keyFrame: Boolean) {
                        rtmpPush!!.pushVideoData(data,keyFrame)
                    }

                    override fun onAudioInfo(data: ByteArray) {
                        rtmpPush!!.pushAudioData(data)
                    }
                }
                pushEncoder!!.startRecord()
            }

            override fun onConnectFail(msg: String) {
                LogUtils.i("onConnectFail:$msg")
            }
        }
        //rtmp://192.168.1.7/myapp/mystream
        rtmpPush!!.initLivePush("rtmp://192.168.1.7/myapp/mystream")
    }

    override fun onDestroy() {
        super.onDestroy()
        rtmpPush!!.stopLivePush()
    }
}