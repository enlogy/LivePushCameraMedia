package com.example.livepushcameramedia.camera

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.livepushcameramedia.R
import kotlinx.android.synthetic.main.activity_camera.*

class CameraActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

    }

    fun onBlackWhite(view: View) {
        mCameraSurfaceView.setBlackWhiteRender()
    }
}