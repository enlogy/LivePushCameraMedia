package com.example.livepushcameramedia.push

import android.app.Activity
import android.content.Context
import android.graphics.SurfaceTexture
import android.util.AttributeSet
import com.example.livepushcameramedia.camera.CameraRender
import com.example.livepushcameramedia.camera.GLCamera
import com.example.livepushcameramedia.egl.EGLSurfaceView

class PushCameraSurfaceView:EGLSurfaceView{
    private var cameraRender: PushCameraRender
    private lateinit var camera: GLCamera

    constructor(context: Context?) : this(context,null)
    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs,0)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )
    {
        cameraRender = PushCameraRender(context!!)
        cameraRender.surfaceTextureListener = object : PushCameraRender.SurfaceTextureListener {
            override fun onCreate(surfaceTexture: SurfaceTexture) {
                val width = (context as Activity).windowManager.defaultDisplay.width
                val height = context.windowManager.defaultDisplay.height
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