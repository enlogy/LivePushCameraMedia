package com.example.livepushcameramedia.utils

import android.content.Context
import android.graphics.*
import android.opengl.GLES20
import android.opengl.GLUtils
import com.blankj.utilcode.util.ConvertUtils
import java.nio.ByteBuffer

class DrawUtil {
    companion object {
        fun createTextImage(
            text: String,
            textSize: Float,
            textColor: String,
            bgColor: String,
            padding: Float
        ): Bitmap {
            val mPadding = ConvertUtils.dp2px(padding).toFloat()
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.color = Color.parseColor(textColor)
            paint.style = Paint.Style.FILL
            paint.textSize = ConvertUtils.sp2px(textSize).toFloat()
            val width = paint.measureText(text)
            val top = paint.fontMetrics.top
            val bottom = paint.fontMetrics.bottom
            val bitmap = Bitmap.createBitmap(
                (width + mPadding * 2).toInt(),
                (bottom - top + mPadding * 2).toInt(),
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.parseColor(bgColor))
            canvas.drawText(text, mPadding, -top + mPadding, paint)
            return bitmap
        }

        /**
         * 需要在linkProgram后调用
          */
        fun loadBitmapTexture(bitmap: Bitmap): Int {
            val textureId = IntArray(1)
            GLES20.glGenTextures(1, textureId, 0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[0])
            //设置环绕，顶点坐标超出纹理坐标范围时作用，s==x t==y GL_REPEAT 重复
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT)
            //过滤（纹理像素映射到坐标点）：（GL_TEXTURE_MIN_FILTER缩小、GL_TEXTURE_MAG_FILTER放大：GL_LINEAR线性）
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR
            )
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR
            )

            val bitmapBuffer = ByteBuffer.allocate(bitmap.width * bitmap.height * 4)
            bitmap.copyPixelsToBuffer(bitmapBuffer)
            bitmapBuffer.flip()

            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D,
                0,
                GLES20.GL_RGBA,
                bitmap.width,
                bitmap.height,
                0,
                GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE,
                bitmapBuffer
            )

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
            return textureId[0]
        }

        /**
         * 需要在linkProgram后调用
         */
        fun loadTexture(context: Context, resId: Int): IntArray {
            val textureId = IntArray(1)
            GLES20.glGenTextures(1, textureId, 0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[0])
            //设置环绕，顶点坐标超出纹理坐标范围时作用，s==x t==y GL_REPEAT 重复
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT)
            //过滤（纹理像素映射到坐标点）：（GL_TEXTURE_MIN_FILTER缩小、GL_TEXTURE_MAG_FILTER放大：GL_LINEAR线性）
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR
            )
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR
            )
            val bitmap = BitmapFactory.decodeResource(context.resources, resId)
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
            return textureId
        }
    }
}