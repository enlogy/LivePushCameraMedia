package com.example.livepushcameramedia.push

import android.media.AudioRecord
import android.media.MediaRecorder
import com.blankj.utilcode.util.PathUtils
import com.blankj.utilcode.util.ThreadUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class AudioRecordHelper {
    @Volatile
    private var isRecording = false
    private var audioRecord: AudioRecord? = null
    private var bufferSizeInBytes = 0
    var onAudioCallBack: OnAudioCallBack? = null
    init {
        bufferSizeInBytes = AudioRecord.getMinBufferSize(
            AudioConfig.SAMPLE_RATE_INHZ,
            AudioConfig.CHANNEL_CONFIG,
            AudioConfig.AUDIO_FORMAT
        )
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            AudioConfig.SAMPLE_RATE_INHZ,
            AudioConfig.CHANNEL_CONFIG,
            AudioConfig.AUDIO_FORMAT,
            bufferSizeInBytes
        )

    }

    private fun readAudioToPcm() {
        ThreadUtils.getSinglePool().submit {
            val data = ByteArray(bufferSizeInBytes)
            while (isRecording) {
                val readSize = audioRecord!!.read(data, 0, bufferSizeInBytes)
                if (AudioRecord.ERROR_INVALID_OPERATION != readSize) {
                    onAudioCallBack?.onPcmData(data,readSize)
                }
            }
        }
    }


    fun startRecord() {
        if (isRecording)
            return
        isRecording = true
        audioRecord!!.startRecording()
        readAudioToPcm()
    }

    fun stopRecord() {
        if (!isRecording)
            return
        isRecording = false
        audioRecord!!.stop()
    }

    fun release() {
        audioRecord!!.release()
    }
    interface OnAudioCallBack{
        fun onPcmData(data:ByteArray,dataSize:Int)
    }
}