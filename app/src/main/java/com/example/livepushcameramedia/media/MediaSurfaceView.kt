package com.example.livepushcameramedia.media

import android.app.Activity
import android.content.Context
import android.graphics.SurfaceTexture
import android.util.AttributeSet
import com.blankj.utilcode.util.LogUtils
import com.example.livepushcameramedia.camera.CameraRender
import com.example.livepushcameramedia.camera.GLCamera
import com.example.livepushcameramedia.egl.EGLSurfaceView

class MediaSurfaceView:EGLSurfaceView {
    private var cameraRender: MediaSurfaceRender
    private lateinit var camera: GLCamera


    constructor(context: Context?) : this(context,null)
    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs,0)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ){
        cameraRender = MediaSurfaceRender(context!!)
        cameraRender.surfaceTextureListener = object : MediaSurfaceRender.SurfaceTextureListener {
            override fun onCreate(surfaceTexture: SurfaceTexture) {
                val width = (context as Activity).windowManager.defaultDisplay.width
                val height = context.windowManager.defaultDisplay.height
                LogUtils.i("Display.width: $width       Display.height: $height")
                camera = GLCamera(surfaceTexture, width, height)
                camera.startPreview()
            }
        }
        setRender(cameraRender)
    }

    fun getCameraTextureId():Int{
        return cameraRender.fboTextureId
    }
}