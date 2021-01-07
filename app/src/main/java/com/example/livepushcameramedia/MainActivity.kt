package com.example.livepushcameramedia

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.constant.PermissionConstants
import com.blankj.utilcode.util.PermissionUtils
import com.example.livepushcameramedia.camera.CameraActivity
import com.example.livepushcameramedia.media.MediaActivity
import com.example.livepushcameramedia.opensles.OpenSLESActivity
import com.example.livepushcameramedia.picture_mp3.PictureMp3Activity
import com.example.livepushcameramedia.push.PushActivity
import com.example.livepushcameramedia.yuv.YuvActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
//    external fun stringFromJNI(): String
//
//    companion object {
//        // Used to load the 'native-lib' library on application startup.
//        init {
//            System.loadLibrary("native-lib")
//        }
//    }

    fun onCameraPreview(view: View) {
        PermissionUtils.permission(PermissionConstants.CAMERA).callback { isAllGranted, granted, deniedForever, denied ->
            if (isAllGranted){
                val intent = Intent(this,CameraActivity::class.java)
                startActivity(intent)
            }
        }.request()

    }

    fun onOpenGLMedia(view: View) {
        PermissionUtils.permission(PermissionConstants.CAMERA,PermissionConstants.STORAGE).callback { isAllGranted, granted, deniedForever, denied ->
            if (isAllGranted){
                val intent = Intent(this,MediaActivity::class.java)
                startActivity(intent)
            }
        }.request()
    }

    fun onMp3PictureMedia(view: View) {
        PermissionUtils.permission(PermissionConstants.STORAGE).callback { isAllGranted, granted, deniedForever, denied ->
            if (isAllGranted){
                val intent = Intent(this,PictureMp3Activity::class.java)
                startActivity(intent)
            }
        }.request()
    }

    fun onYuvMedia(view: View) {
        PermissionUtils.permission(PermissionConstants.STORAGE).callback { isAllGranted, granted, deniedForever, denied ->
            if (isAllGranted){
                val intent = Intent(this,YuvActivity::class.java)
                startActivity(intent)
            }
        }.request()
    }

    fun onOpenSLESRecord(view: View) {
        PermissionUtils.permission(PermissionConstants.STORAGE).callback { isAllGranted, granted, deniedForever, denied ->
            if (isAllGranted){
                val intent = Intent(this,OpenSLESActivity::class.java)
                startActivity(intent)
            }
        }.request()
    }

    fun onPushlive(view: View) {
        PermissionUtils.permission(PermissionConstants.MICROPHONE).callback { isAllGranted, granted, deniedForever, denied ->
            if (isAllGranted){
                val intent = Intent(this,PushActivity::class.java)
                startActivity(intent)
            }
        }.request()
    }
}
