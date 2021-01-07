package com.example.livepushcameramedia.camera

import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.Camera
import com.blankj.utilcode.util.ScreenUtils

class GLCamera(var surfaceTexture: SurfaceTexture,var width:Int,var height:Int) : Camera.PreviewCallback {
    private var screenWidth:Int = ScreenUtils.getScreenWidth()
    private var screenHeight:Int = ScreenUtils.getScreenHeight()

    private var camera: Camera? = null
    private val back = Camera.CameraInfo.CAMERA_FACING_BACK
    private val front = Camera.CameraInfo.CAMERA_FACING_FRONT
    fun startPreview() {
        if (camera == null) {
            camera = Camera.open(back)
        }
        setParams()
    }

    private fun setParams() {
        val params = camera!!.parameters
        val pictureSize = getFitSize(params.supportedPictureSizes)
        params.setPictureSize(pictureSize!!.width, pictureSize.height)
        val previewSize = getFitSize(params.supportedPreviewSizes)
        params.setPreviewSize(previewSize!!.width, previewSize.height)
        params.previewFormat = ImageFormat.NV21
        camera!!.parameters = params

        camera!!.setPreviewCallback(this)
        camera!!.setPreviewTexture(surfaceTexture)
        camera!!.startPreview()
    }

    fun stopPreview() {
        camera!!.stopPreview()
        camera!!.release()
        camera = null
    }

    override fun onPreviewFrame(data: ByteArray?, camera: Camera?) {

    }

    private fun getFitSize(sizes: List<Camera.Size>): Camera.Size? {
        if (width < height) {
            val t = height
            height = width
            width = t
        }
        for (size in sizes) {
            if (1.0f * size.width / size.height == 1.0f * width / height) {
                return size
            }
        }
        return sizes[0]
    }
}