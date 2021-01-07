package com.example.livepushcameramedia.picture_mp3

import android.content.Context
import android.util.AttributeSet
import com.example.livepushcameramedia.R
import com.example.livepushcameramedia.egl.EGLSurfaceView

class PictureMp3SurfaceView: EGLSurfaceView {
    private var pictureMp3Render:PictureMp3Render
    constructor(context: Context?) : this(context,null)
    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs,0)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ){
        pictureMp3Render = PictureMp3Render(context!!)
        pictureMp3Render.setImageResId(R.drawable.img_1)
        setRender(pictureMp3Render)
    }

    fun getTextureId():Int{
        return pictureMp3Render.fboTextureId
    }

    fun setImageResId(imgResId:Int){
        pictureMp3Render.setImageResId(imgResId)
    }
}