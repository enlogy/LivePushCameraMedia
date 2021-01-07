package com.example.livepushcameramedia.push

import android.media.AudioAttributes
import android.media.AudioFormat
import android.os.Build
import androidx.annotation.RequiresApi

class AudioConfig {
    companion object {
        /**
         * 采样率，现在能够保证在所有设备上使用的采样率是44100Hz, 但是其他的采样率（22050, 16000, 11025）在一些设备上也可以使用。
         */
        val SAMPLE_RATE_INHZ = 44100

        /**
         * 声道数。CHANNEL_IN_MONO(单声道) and CHANNEL_IN_STEREO(双声道). 其中CHANNEL_IN_MONO是可以保证在所有设备能够使用的(在oppo findx中报错)。
         */
        val CHANNEL_CONFIG: Int = AudioFormat.CHANNEL_IN_STEREO

        /**
         * 返回的音频数据的格式。 ENCODING_PCM_8BIT, ENCODING_PCM_16BIT, and ENCODING_PCM_FLOAT.
         */
        val AUDIO_FORMAT: Int = AudioFormat.ENCODING_PCM_16BIT

        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        val audioAttributes: AudioAttributes =
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()

        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        val audioFormat: AudioFormat =
            AudioFormat.Builder().setSampleRate(SAMPLE_RATE_INHZ).setEncoding(AUDIO_FORMAT)
                .setChannelMask(CHANNEL_CONFIG).build()

    }
}