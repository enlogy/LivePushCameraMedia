package com.example.livepushcameramedia.picture_mp3

import android.content.Context
import com.example.livepushcameramedia.media.BaseMediaEncoder

class PictureMp3MediaEncoder(context: Context, textureId:Int):BaseMediaEncoder() {
    private var glRender :PictureMp3GlRender? = null
    init {
        glRender = PictureMp3GlRender(context, textureId)
        setRender(glRender!!)
    }
}