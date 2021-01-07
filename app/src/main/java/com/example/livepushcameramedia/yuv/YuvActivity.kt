package com.example.livepushcameramedia.yuv

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.util.PathUtils
import com.example.livepushcameramedia.R
import kotlinx.android.synthetic.main.activity_yuv.*
import java.io.File
import java.io.FileInputStream

class YuvActivity:AppCompatActivity() {
    private var fis: FileInputStream? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_yuv)
    }

    fun startEncode(view: View) {
        Thread{
            try {
                val w = 640
                val h = 360
                fis = FileInputStream(File(PathUtils.getInternalAppFilesPath()+"/sintel_640_360.yuv"))
                val y = ByteArray(w * h)
                val u = ByteArray(w * h / 4)
                val v = ByteArray(w * h / 4)
                while (true) {
                    val ry = fis!!.read(y)
                    val ru = fis!!.read(u)
                    val rv = fis!!.read(v)
                    if (ry > 0 && ru > 0 && rv > 0) {
                        mSurfaceView.setFrameData(w, h, y, u, v)
                        Thread.sleep(40)
                    } else {
                        Log.d("enlogy", "完成")
                        break
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()

    }
}