package com.example.livepushcameramedia.picture_mp3

import android.media.MediaFormat
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.util.PathUtils
import com.example.livepushcameramedia.R
import com.example.livepushcameramedia.media.BaseMediaEncoder
import com.ywl5320.libmusic.WlMusic
import com.ywl5320.listener.OnShowPcmDataListener
import kotlinx.android.synthetic.main.activity_pirture_mp3.*

class PictureMp3Activity :AppCompatActivity(){
    private var isEncoding = false
    private var mPictureMp3MediaEncoder: BaseMediaEncoder? = null
    private var mWlMusic: WlMusic? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pirture_mp3)
        mWlMusic = WlMusic.getInstance()
        mWlMusic!!.setCallBackPcmData(true)
        mWlMusic!!.setOnPreparedListener {
            mWlMusic!!.playCutAudio(1,120)
        }

        mWlMusic!!.setOnCompleteListener {
            mPictureMp3MediaEncoder!!.stopRecord()
            mPictureMp3MediaEncoder = null
        }

        mWlMusic!!.setOnShowPcmDataListener(object :OnShowPcmDataListener{
            @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
            override fun onPcmInfo(samplerate: Int, bit: Int, channels: Int) {
                mPictureMp3MediaEncoder = PictureMp3MediaEncoder(this@PictureMp3Activity,mSurfaceView.getTextureId())
                mPictureMp3MediaEncoder!!.initMediaEncoder(mSurfaceView.getEglContext(),1080,1920,
                    MediaFormat.MIMETYPE_VIDEO_AVC,PathUtils.getInternalAppFilesPath() + "/Mp3Picture.mp4",
                    MediaFormat.MIMETYPE_AUDIO_AAC,samplerate,channels)
                mPictureMp3MediaEncoder!!.startRecord()

                //开启线程更换渲染的图片
                Thread{
                    for (i in 1 .. 257){
                        val imgResId =
                            resources.getIdentifier("img_$i", "drawable", "com.example.livepushcameramedia")
                        mSurfaceView.setImageResId(imgResId)
                        Thread.sleep(80)
                    }
                }.start()
            }

            override fun onPcmData(pcmdata: ByteArray?, size: Int, clock: Long) {
                if (pcmdata != null)
                mPictureMp3MediaEncoder?.putPCMData(pcmdata,size)
            }
        })
    }

    fun startEncode(view: View) {
        isEncoding = !isEncoding
        if (isEncoding) {
            mEncodeBtn.text = "结束录制"
            mWlMusic!!.source = PathUtils.getInternalAppFilesPath()+ "/thegirl.m4a"
            mWlMusic!!.prePared()
        } else {
            mEncodeBtn.text = "开始录制"
            mPictureMp3MediaEncoder?.stopRecord()
            mPictureMp3MediaEncoder = null
            mWlMusic!!.stop()
        }
    }
}