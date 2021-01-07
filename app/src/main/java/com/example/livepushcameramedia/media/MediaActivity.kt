package com.example.livepushcameramedia.media

import android.media.MediaFormat
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.PathUtils
import com.example.livepushcameramedia.R
import com.ywl5320.libmusic.WlMusic
import com.ywl5320.listener.OnShowPcmDataListener
import kotlinx.android.synthetic.main.activity_media.*

/**
 * 拍摄视频，添加音乐水印
 */
class MediaActivity : AppCompatActivity() {
    private var isEncoding = false
    private var myMediaEncoder: BaseMediaEncoder? = null
    private var mWlMusic: WlMusic? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media)
        mWlMusic = WlMusic.getInstance()
        mWlMusic!!.setCallBackPcmData(true)
        mWlMusic!!.setOnShowPcmDataListener(object : OnShowPcmDataListener {
            @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
            override fun onPcmInfo(samplerate: Int, bit: Int, channels: Int) {
                LogUtils.i("onPcmInfo")
                //开始播放
                if (myMediaEncoder == null) {
                    myMediaEncoder = MyMediaEncoder(this@MediaActivity, mSurfaceView.getCameraTextureId())
                    myMediaEncoder!!.initMediaEncoder(
                        mSurfaceView.getEglContext(), 1080, 1920, MediaFormat.MIMETYPE_VIDEO_AVC,
                        PathUtils.getInternalAppFilesPath() + "/MyVideo.mp4",
                        MediaFormat.MIMETYPE_AUDIO_AAC,samplerate,channels)
                    myMediaEncoder!!.startRecord()
                }
            }

            override fun onPcmData(pcmdata: ByteArray?, size: Int, clock: Long) {
//                LogUtils.i("onPcmData")
                    //播放中的pcm数据
                    if (pcmdata != null) {
                        myMediaEncoder!!.putPCMData(pcmdata,size)
                    }
            }
        })
        mWlMusic!!.setOnPreparedListener {
            LogUtils.i("setOnPreparedListener")
            mWlMusic!!.playCutAudio(10,30)
        }
        mWlMusic!!.setOnCompleteListener {
            LogUtils.i("setOnCompleteListener")
            myMediaEncoder!!.stopRecord()
            myMediaEncoder = null
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun startEncode(view: View) {
        isEncoding = !isEncoding
        if (isEncoding) {
            mEncodeBtn.text = "结束录制"
            mWlMusic!!.source = PathUtils.getInternalAppFilesPath()+"/test.mp3"
            mWlMusic!!.prePared()
        } else {
            mEncodeBtn.text = "开始录制"
            myMediaEncoder?.stopRecord()
            myMediaEncoder = null
            mWlMusic!!.stop()
        }
    }
}