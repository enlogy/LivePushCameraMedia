package com.example.livepushcameramedia.media

import android.content.Context

class MyMediaEncoder(context: Context,textureId:Int):BaseMediaEncoder() {
    private val myMediaRender:MyMediaRender = MyMediaRender(context,textureId)

    init {
        setRender(myMediaRender)
    }
}