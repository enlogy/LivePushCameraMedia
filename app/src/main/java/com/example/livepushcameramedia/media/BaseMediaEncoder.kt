package com.example.livepushcameramedia.media

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import android.view.Surface
import androidx.annotation.RequiresApi
import com.blankj.utilcode.util.LogUtils
import com.example.livepushcameramedia.egl.EGLHelper
import com.example.livepushcameramedia.egl.EGLSurfaceRender
import com.example.livepushcameramedia.egl.EGLSurfaceView
import java.lang.ref.WeakReference
import javax.microedition.khronos.egl.EGLContext

abstract class BaseMediaEncoder {
    companion object {
        const val RENDERMODE_WHEN_DIRTY = 0
        const val RENDERMODE_CONTINUOUSLY = 1
    }

    private val lock = Object()

    @Volatile
    private var muxerStarted: Boolean = false

    @Volatile
    private var muxerStoped: Boolean = false
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
    private var muxer: MediaMuxer? = null
    private var audioPts: Long = 0
    private var sampleRate = 0
    private var channelCount = 0
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
        }
    }

    fun stopRecord() {
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
        path: String,
        audioMimeType: String,
        sampleRate: Int,
        channelCount: Int
    ) {
        LogUtils.i("path:$path")
        mEGlContext = eglContext
        this.width = width
        this.height = height
        initVideoEncoder(videoMimeTye, width, height)
        initAudioEncoder(audioMimeType, sampleRate, channelCount)
        initMediaMuxer(path)
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
        audioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 4096)

        audioEncoder = MediaCodec.createEncoderByType(mime)
        audioEncoder!!.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private fun initMediaMuxer(path: String) {
        muxer = MediaMuxer(path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
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
                    getAudioPts(size,sampleRate,channelCount),
                    0
                )
            }
        }
    }
    
    private fun getAudioPts(size: Int,sampleRate:Int,channelCount: Int): Long {
        audioPts += (1.0 * size  / (sampleRate * 2 * 2) * 1000000.0).toLong()
        return audioPts
    }

    class VideoEncoderThread(var weakEncoder: WeakReference<BaseMediaEncoder>) : Thread() {
        private var isExit = false
        private val videoEncoder: MediaCodec = weakEncoder.get()!!.videoEncoder!!
        private val muxer: MediaMuxer = weakEncoder.get()!!.muxer!!
        private var bufferInfo: MediaCodec.BufferInfo = MediaCodec.BufferInfo()
        var videoTrackIndex = -1
        private var pts: Long = 0

        @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
        override fun run() {
            super.run()
            videoEncoder.start()
            while (true) {
                if (isExit) {
                    //release
                    videoEncoder.stop()
                    videoEncoder.release()
                    if (!weakEncoder.get()!!.muxerStoped) {
                        weakEncoder.get()!!.muxerStoped = true
                        LogUtils.i("video  muxer.stop()")
                        muxer.stop()
                        muxer.release()
                    }
                    LogUtils.i("video录制完成")
                    break
                }
                var outputBufferIndex = videoEncoder.dequeueOutputBuffer(bufferInfo, 0)
                if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    videoTrackIndex = muxer.addTrack(videoEncoder.outputFormat)
                    LogUtils.i("add videoTrackIndex")
                    if (weakEncoder.get()!!.mAudioEncoderThread!!.audioTrackIndex != -1 && !weakEncoder.get()!!.muxerStarted) {
                        LogUtils.i("video 开始录制 muxer.start()")
                        muxer.start()
                        weakEncoder.get()!!.muxerStarted = true
                    }
                } else {
                    while (outputBufferIndex >= 0) {
                        if (weakEncoder.get()!!.muxerStarted){
                            val outputBuffer = videoEncoder.outputBuffers[outputBufferIndex]
                            outputBuffer.position(bufferInfo.offset)
                            outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                            if (pts == 0L) {
                                pts = bufferInfo.presentationTimeUs
                            }
                            bufferInfo.presentationTimeUs = bufferInfo.presentationTimeUs - pts
                            LogUtils.i("video 录制时间: ${bufferInfo.presentationTimeUs / 1000000}")
                            muxer.writeSampleData(videoTrackIndex, outputBuffer, bufferInfo)
                        }
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

    class AudioEncoderThread(var weakEncoder: WeakReference<BaseMediaEncoder>) : Thread() {
        var isExit = false
        private val audioEncoder: MediaCodec = weakEncoder.get()!!.audioEncoder!!
        private val muxer: MediaMuxer = weakEncoder.get()!!.muxer!!
        private var bufferInfo: MediaCodec.BufferInfo = MediaCodec.BufferInfo()
        var audioTrackIndex = -1
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
                    if (!weakEncoder.get()!!.muxerStoped) {
                        weakEncoder.get()!!.muxerStoped = true
                        LogUtils.i("audio  muxer.stop()")
                        muxer.stop()
                        muxer.release()
                    }
                    LogUtils.i("audio录制完成")
                    break
                }
                var outputBufferIndex = audioEncoder.dequeueOutputBuffer(bufferInfo, 0)
                if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    audioTrackIndex = muxer.addTrack(audioEncoder.outputFormat)
                    LogUtils.i("add audioTrackIndex")
                    if (weakEncoder.get()!!.mVideoEncoderThread!!.videoTrackIndex != -1 && !weakEncoder.get()!!.muxerStarted) {
                        LogUtils.i("audio thread 开始录制 muxer.start()")
                        muxer.start()
                        weakEncoder.get()!!.muxerStarted = true
                    }
                } else {
                    while (outputBufferIndex >= 0) {
                        if (weakEncoder.get()!!.muxerStarted){
                            val outputBuffer = audioEncoder.outputBuffers[outputBufferIndex]
                            outputBuffer.position(bufferInfo.offset)
                            outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                            if (pts == 0L) {
                                pts = bufferInfo.presentationTimeUs
                            }
                            bufferInfo.presentationTimeUs = bufferInfo.presentationTimeUs - pts
                            LogUtils.i("audio录制时间: ${bufferInfo.presentationTimeUs / 1000000}")
                            muxer.writeSampleData(audioTrackIndex, outputBuffer, bufferInfo)
                        }
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

    class GLThread(var weakEncoder: WeakReference<BaseMediaEncoder>) : Thread() {
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
}