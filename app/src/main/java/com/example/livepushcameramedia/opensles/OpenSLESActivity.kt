package com.example.livepushcameramedia.opensles

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.util.PathUtils
import com.example.livepushcameramedia.R

class OpenSLESActivity:AppCompatActivity() {
    companion object{
        init {
            System.loadLibrary("native-lib")
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_opensles)
    }

    fun startRecord(view: View) {
        startRecord(PathUtils.getInternalAppFilesPath()+"/my_record.pcm")
    }

    fun stopRecord(view: View) {
        stopRecord()
    }

    external fun startRecord(pcmPath:String)
    external fun stopRecord()
}