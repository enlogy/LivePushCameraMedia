package com.example.livepushcameramedia.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES20
import com.example.livepushcameramedia.R
import com.example.livepushcameramedia.egl.EGLSurfaceRender
import com.example.livepushcameramedia.egl.ShaderUtil
import com.example.livepushcameramedia.utils.DrawUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class MyMediaRender(var context: Context,var textureId:Int) :EGLSurfaceRender{

    private var vertexPoints = floatArrayOf(
        -1f, -1f,
        1f, -1f,
        -1f, 1f,
        1f, 1f,
    //水印坐标占位
    0f, 0f,
    0f, 0f,
    0f, 0f,
    0f, 0f
    )
    private var fragmentPoints = floatArrayOf(
        0f, 1f,
        1f, 1f,
        0f, 0f,
        1f, 0f
    )
    private  var vertexBuffer: FloatBuffer
    private  var fragmentBuffer: FloatBuffer
    private var vPosition = 0
    private var fPosition = 0
    private var program = 0
    private lateinit var vbo: IntArray
    private var waterImgTextureId:Int = 0
    //两种方式都可以
    private var waterBmp:Bitmap = DrawUtil.createTextImage("我是水印", 16f, "#ff0000", "#00000000", 8f)
//    private var waterBmp = BitmapFactory.decodeResource(context.resources, R.mipmap.mimg)

    init {
        //为水印顶点赋值vertexPoints
        //在顶点坐标中画图计算，0.1：设置水印的高在顶点坐标占0.1大小,假定水印右下角坐标点后计算
        val r: Float = 1.0f * waterBmp.width / waterBmp.height
        val w = r * 0.1f
        vertexPoints[8] = 0.8f - w
        vertexPoints[9] = -0.8f

        vertexPoints[10] = 0.8f
        vertexPoints[11] = -0.8f

        vertexPoints[12] = 0.8f - w
        vertexPoints[13] = -0.7f

        vertexPoints[14] = 0.8f
        vertexPoints[15] = -0.7f

        vertexBuffer = ByteBuffer.allocateDirect(vertexPoints.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertexPoints)
        vertexBuffer.position(0)
        fragmentBuffer = ByteBuffer.allocateDirect(fragmentPoints.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(fragmentPoints)
        fragmentBuffer.position(0)
    }

    override fun onSurfaceCreated() {
        //开启透明度
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA,GLES20.GL_ONE_MINUS_SRC_ALPHA)

        program = ShaderUtil.linkProgram(context, R.raw.vertex_shader, R.raw.fragment_shader)
//        GLES20.glUseProgram(program)
        //属性
        vPosition = GLES20.glGetAttribLocation(program, "v_Position")
        fPosition = GLES20.glGetAttribLocation(program, "f_Position")

        //vbo
        vbo = IntArray(1)
        GLES20.glGenBuffers(1, vbo, 0)

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo[0])
        GLES20.glBufferData(
            GLES20.GL_ARRAY_BUFFER,
            vertexPoints.size * 4 + fragmentPoints.size * 4,
            null,
            GLES20.GL_STATIC_DRAW
        )
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, vertexPoints.size * 4, vertexBuffer)
        GLES20.glBufferSubData(
            GLES20.GL_ARRAY_BUFFER,
            vertexPoints.size * 4,
            fragmentPoints.size * 4,
            fragmentBuffer
        )
        //赋值后可以解绑
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)

        //两种方式都可以
        waterImgTextureId =  DrawUtil.loadBitmapTexture(waterBmp)
        //waterImgTextureId =  DrawUtil.loadTexture(context,R.mipmap.mimg)
    }

    override fun onSurfaceChanged(width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame() {

        GLES20.glUseProgram(program)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo[0])
        //shader赋值
        GLES20.glEnableVertexAttribArray(vPosition)
        GLES20.glVertexAttribPointer(vPosition, 2, GLES20.GL_FLOAT, false, 8, 0)
        GLES20.glEnableVertexAttribArray(fPosition)
        GLES20.glVertexAttribPointer(fPosition, 2, GLES20.GL_FLOAT, false, 8, vertexPoints.size * 4)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)

//        //添加水印
//        vbo加载顶点和纹理数据
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo[0])
        GLES20.glEnableVertexAttribArray(vPosition)
        GLES20.glVertexAttribPointer(vPosition, 2, GLES20.GL_FLOAT, false, 8, 32)//4个xy点
        GLES20.glEnableVertexAttribArray(fPosition)
        GLES20.glVertexAttribPointer(fPosition, 2, GLES20.GL_FLOAT, false, 8, vertexPoints.size * 4)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        //渲染fbo的texture生成一个水印图片
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, waterImgTextureId)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)

    }
}