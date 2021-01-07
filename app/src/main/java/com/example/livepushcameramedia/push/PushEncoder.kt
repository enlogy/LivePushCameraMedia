package com.example.livepushcameramedia.push

import android.content.Context

class PushEncoder(var context: Context, var textureId:Int):BasePushEncoder() {
    var pushEncoderRender:PushEncoderRender = PushEncoderRender(context, textureId)

    init {
        setRender(pushEncoderRender)
    }
}