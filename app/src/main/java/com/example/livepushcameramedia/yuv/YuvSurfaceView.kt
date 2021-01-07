package com.example.livepushcameramedia.yuv

import android.content.Context
import android.util.AttributeSet
import com.example.livepushcameramedia.egl.EGLSurfaceView

class YuvSurfaceView:EGLSurfaceView {
    private var mRender: YuvRender
    constructor(context: Context?) : this(context,null)
    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs,0)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ){
        mRender = YuvRender(context!!)
        setRender(mRender)
        setRenderMode(RENDERMODE_WHEN_DIRTY)
    }
    fun setFrameData(
        w: Int,
        h: Int,
        by: ByteArray,
        bu: ByteArray,
        bv: ByteArray
    ){
        mRender.setFrameData(w, h, by, bu, bv)
        requestRender()
    }
}