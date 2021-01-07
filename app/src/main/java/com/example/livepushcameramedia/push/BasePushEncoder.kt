package com.example.livepushcameramedia.push

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Build
import android.view.Surface
import androidx.annotation.RequiresApi
import com.blankj.utilcode.util.LogUtils
import com.example.livepushcameramedia.egl.EGLHelper
import com.example.livepushcameramedia.egl.EGLSurfaceRender
import com.example.livepushcameramedia.egl.EGLSurfaceView
import java.lang.ref.WeakReference
import javax.microedition.khronos.egl.EGLContext

abstract class BasePushEncoder {
    companion object {
        const val RENDERMODE_WHEN_DIRTY = 0
        const val RENDERMODE_CONTINUOUSLY = 1
    }

    private val lock = Object()

    private var mGLThread: GLThread? = null
    private var mVideoEncoderThread: VideoEncoderThread? = null
    private var mAudioEncoderThread: AudioEncoderThread? = null
    private var mEGlContext: EGLContext? = null
    private var mSurface: Surface? = null
    private var mEGLSurfaceRender: EGLSurfaceRender? = null
    private var mRenderMode: Int = RENDERMODE_CONTINUOUSLY
    private var width = 0
    private var height = 0
    private var videoEncoder: MediaCodec? = null
    private var audioEncoder: MediaCodec? = null
    private var audioPts: Long = 0
    private var sampleRate = 0
    private var channelCount = 0
    var onMediaInfoListener:OnMediaInfoListener? = null
    private var audioRecordHelper:AudioRecordHelper? = null
    fun setRender(render: EGLSurfaceRender) {
        mEGLSurfaceRender = render
    }

    fun startRecord() {
        if (mSurface != null && mEGlContext != null) {
            mGLThread = GLThread(WeakReference(this))
            mGLThread!!.isCreate = true
            mGLThread!!.isChange = true
            mVideoEncoderThread = VideoEncoderThread(WeakReference(this))
            mAudioEncoderThread = AudioEncoderThread(WeakReference(this))
            mGLThread!!.start()
            mVideoEncoderThread!!.start()
            mAudioEncoderThread!!.start()
            audioRecordHelper!!.startRecord()
        }
    }

    fun stopRecord() {
        audioRecordHelper?.stopRecord()
        mGLThread?.exit()
        mVideoEncoderThread?.exit()
        mAudioEncoderThread?.exit()
        mGLThread = null
        mVideoEncoderThread = null
        mAudioEncoderThread = null
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    fun initMediaEncoder(
        eglContext: EGLContext,
        width: Int,
        height: Int,
        videoMimeTye: String,
        audioMimeType: String,
        sampleRate: Int,
        channelCount: Int
    ) {
        mEGlContext = eglContext
        this.width = width
        this.height = height
        initVideoEncoder(videoMimeTye, width, height)
        initAudioEncoder(audioMimeType, sampleRate, channelCount)
        audioRecordHelper = AudioRecordHelper()
        audioRecordHelper!!.onAudioCallBack = object :AudioRecordHelper.OnAudioCallBack{
            override fun onPcmData(data: ByteArray, dataSize:Int) {
                if (data.isNotEmpty())
                putPCMData(data,dataSize)
            }
        }
    }

    private fun initAudioEncoder(mime: String, sampleRate: Int, channelCount: Int) {
        this.channelCount = channelCount
        this.sampleRate = sampleRate
        val audioFormat = MediaFormat.createAudioFormat(mime, sampleRate, channelCount)
        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, 96000)
        audioFormat.setInteger(
            MediaFormat.KEY_AAC_PROFILE,
            MediaCodecInfo.CodecProfileLevel.AACObjectLC
        )
        audioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 4096 * 10)

        audioEncoder = MediaCodec.createEncoderByType(mime)
        audioEncoder!!.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
    }


    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private fun initVideoEncoder(mimeTye: String, width: Int, height: Int) {
        val mediaFormat = MediaFormat.createVideoFormat(mimeTye, width, height)
        mediaFormat.setInteger(
            MediaFormat.KEY_COLOR_FORMAT,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
        )
        //码率
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, width * height * 4)
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30)
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        videoEncoder = MediaCodec.createEncoderByType(mimeTye)
        videoEncoder!!.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        mSurface = videoEncoder!!.createInputSurface()
    }

    /**
     * 音频数据PCM
     */
    fun putPCMData(buffer: ByteArray, size: Int) {
        if (mAudioEncoderThread != null && !mAudioEncoderThread!!.isExit && size > 0) {
            val inputBufferIndex = audioEncoder!!.dequeueInputBuffer(0)
            if (inputBufferIndex >= 0) {
                val byteBuffer = audioEncoder!!.inputBuffers[inputBufferIndex]
                byteBuffer.clear()
                byteBuffer.put(buffer)
                audioEncoder!!.queueInputBuffer(
                    inputBufferIndex,
                    0,
                    size,
                    getAudioPts(size,sampleRate),
                    0
                )
            }
        }
    }
    
    private fun getAudioPts(size: Int,sampleRate:Int): Long {
        audioPts += (1.0 * size  / (sampleRate * 2 * 2) * 1000000.0).toLong()
        return audioPts
    }

    class VideoEncoderThread(var weakEncoder: WeakReference<BasePushEncoder>) : Thread() {
        private var isExit = false
        private val videoEncoder: MediaCodec = weakEncoder.get()!!.videoEncoder!!
        private var bufferInfo: MediaCodec.BufferInfo = MediaCodec.BufferInfo()
        private var pts: Long = 0
        private var sps:ByteArray? = null
        private var pps:ByteArray? = null
        //记录是否关键帧I
        private var keyFrame = false
        @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
        override fun run() {
            super.run()
            videoEncoder.start()
            while (true) {
                if (isExit) {
                    //release
                    videoEncoder.stop()
                    videoEncoder.release()
                    LogUtils.i("video录制完成")
                    break
                }

                var outputBufferIndex = videoEncoder.dequeueOutputBuffer(bufferInfo, 0)
                if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    LogUtils.i("=========MediaCodec.INFO_OUTPUT_FORMAT_CHANGED============")
                    //获取SPS和PPS
                    val spsBuffer = videoEncoder.outputFormat.getByteBuffer("csd-0")
                    sps = ByteArray(spsBuffer!!.remaining())
                    spsBuffer.get(sps,0,sps!!.size)

                    val ppsBuffer = videoEncoder.outputFormat.getByteBuffer("csd-1")
                    pps = ByteArray(ppsBuffer!!.remaining())
                    ppsBuffer.get(pps,0,pps!!.size)

//                    LogUtils.i("sps===: ${ConvertUtils.bytes2HexString(sps)}")
                } else {
                    while (outputBufferIndex >= 0) {
                            val outputBuffer = videoEncoder.outputBuffers[outputBufferIndex]
                            outputBuffer.position(bufferInfo.offset)
                            outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                            if (pts == 0L) {
                                pts = bufferInfo.presentationTimeUs
                            }
                            bufferInfo.presentationTimeUs = bufferInfo.presentationTimeUs - pts
//                            LogUtils.i("video 录制时间: ${bufferInfo.presentationTimeUs / 1000000}")

                        //获取视频数据
                        val data = ByteArray(outputBuffer.remaining())
                        outputBuffer.get(data,0,data.size)
//                        LogUtils.i("data===: ${ConvertUtils.bytes2HexString(data)}")
                        //判断关键帧，在关键帧前发送pps sps数据，用于接收方解析
                        keyFrame = false
                        if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_KEY_FRAME){
                            keyFrame = true
                            weakEncoder.get()!!.onMediaInfoListener?.onSPSAndPPSInfo(sps!!,pps!!)
                        }
                        //发送视频数据
                        weakEncoder.get()!!.onMediaInfoListener?.onVideoInfo(data,keyFrame)

                        videoEncoder.releaseOutputBuffer(outputBufferIndex, false)
                        outputBufferIndex = videoEncoder.dequeueOutputBuffer(bufferInfo, 0)
                    }
                }

            }
        }

        fun exit() {
            isExit = true
        }
    }

    class AudioEncoderThread(var weakEncoder: WeakReference<BasePushEncoder>) : Thread() {
        var isExit = false
        private val audioEncoder: MediaCodec = weakEncoder.get()!!.audioEncoder!!
        private var bufferInfo: MediaCodec.BufferInfo = MediaCodec.BufferInfo()
        private var pts: Long = 0

        @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
        override fun run() {
            super.run()
            audioEncoder.start()
            while (true) {
                if (isExit) {
                    //release
                    audioEncoder.stop()
                    audioEncoder.release()
                    LogUtils.i("audio录制完成")
                    break
                }
                var outputBufferIndex = audioEncoder.dequeueOutputBuffer(bufferInfo, 0)
                if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                } else {
                    while (outputBufferIndex >= 0) {
                            val outputBuffer = audioEncoder.outputBuffers[outputBufferIndex]
                            outputBuffer.position(bufferInfo.offset)
                            outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                            if (pts == 0L) {
                                pts = bufferInfo.presentationTimeUs
                            }
                            bufferInfo.presentationTimeUs = bufferInfo.presentationTimeUs - pts
//                            LogUtils.i("audio录制时间: ${bufferInfo.presentationTimeUs / 1000000}")

                        //获取音频数据
                        val data = ByteArray(outputBuffer.remaining())
                        outputBuffer.get(data,0,data.size)
                        //发送音频数据
                        weakEncoder.get()!!.onMediaInfoListener?.onAudioInfo(data)

                        audioEncoder.releaseOutputBuffer(outputBufferIndex, false)
                        outputBufferIndex = audioEncoder.dequeueOutputBuffer(bufferInfo, 0)
                    }
                }

            }
        }

        fun exit() {
            isExit = true
        }
    }

    class GLThread(var weakEncoder: WeakReference<BasePushEncoder>) : Thread() {
        var isCreate = false
        var isChange = false
        private var lock: Object? = null
        private var eglHelper: EGLHelper? = null
        private var isExit = false
        override fun run() {
            super.run()
            lock = Object()
            eglHelper = EGLHelper()
            eglHelper!!.configure(
                weakEncoder.get()?.mSurface!!,
                weakEncoder.get()?.mEGlContext
            )
            while (true) {
                if (isExit) {
                    //release
                    eglHelper!!.destoryEgl()
                    break
                }
                onCreate()
                onChange()
                onDraw()
                when (weakEncoder.get()?.mRenderMode) {
                    EGLSurfaceView.RENDERMODE_WHEN_DIRTY -> {
                        synchronized(lock!!) {
                            lock!!.wait()
                        }
                    }
                    EGLSurfaceView.RENDERMODE_CONTINUOUSLY -> {
                        sleep(1000 / 60)
                    }
                    else -> {
                        throw RuntimeException("mRenderMode is error")
                    }
                }
            }
        }

        private fun onCreate() {
            if (isCreate) {
                val render = weakEncoder.get()?.mEGLSurfaceRender
                    ?: throw RuntimeException("mEGLSurfaceRender is null")
                render.onSurfaceCreated()
                isCreate = false
            }
        }

        private fun onChange() {
            if (isChange) {
                val render = weakEncoder.get()?.mEGLSurfaceRender
                    ?: throw RuntimeException("mEGLSurfaceRender is null")
                render.onSurfaceChanged(weakEncoder.get()!!.width, weakEncoder.get()!!.height)
                isChange = false
            }
        }

        private fun onDraw() {
            val render = weakEncoder.get()?.mEGLSurfaceRender
                ?: throw RuntimeException("mEGLSurfaceRender is null")
            render.onDrawFrame()
            eglHelper!!.swapBuffers()
        }

        fun requestRender() {
            synchronized(lock!!) {
                lock!!.notify()
            }
        }

        fun exit() {
            isExit = true
            synchronized(lock!!) {
                lock!!.notify()
            }
        }
    }
    interface OnMediaInfoListener{
        fun onSPSAndPPSInfo(sps:ByteArray,pps:ByteArray)
        fun onVideoInfo(data: ByteArray, keyFrame: Boolean)
        fun onAudioInfo(data: ByteArray)
    }
}