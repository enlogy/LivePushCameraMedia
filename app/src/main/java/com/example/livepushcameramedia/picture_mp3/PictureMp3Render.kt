package com.example.livepushcameramedia.picture_mp3

import android.app.Activity
import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLUtils
import com.blankj.utilcode.util.LogUtils
import com.example.livepushcameramedia.FboRender
import com.example.livepushcameramedia.R
import com.example.livepushcameramedia.egl.EGLSurfaceRender
import com.example.livepushcameramedia.egl.ShaderUtil
import com.example.livepushcameramedia.utils.DrawUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class PictureMp3Render(var context: Context):EGLSurfaceRender {
    private var vertexPoints = floatArrayOf(
        -1f, -1f,
        1f, -1f,
        -1f, 1f,
        1f, 1f
    )
    // OpenGL标准坐标(左下角0.0，右上角1.1) fbo所使用的坐标,非纹理坐标
    private val fragmentPoints = floatArrayOf(
        0f, 0f,
        1f, 0f,
        0f, 1f,
        1f, 1f
    )
    private var vertexBuffer:FloatBuffer
    private var fragmentBuffer:FloatBuffer
    //vbo
    private lateinit var vbo:IntArray
    private lateinit var fbo:IntArray
    var fboTextureId:Int = 0
    private var screenWidth: Int
    private var screenHeight: Int
    private var imgResId = 0
    private var program = 0
    private var vPosition = 0
    private var fPosition = 0
    private val fboRender = FboRender(context)

    init {
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
        screenWidth = (context as Activity).windowManager.defaultDisplay.width
        screenHeight = (context as Activity).windowManager.defaultDisplay.height
    }

    override fun onSurfaceCreated() {
        program = ShaderUtil.linkProgram(context, R.raw.vertex_shader,R.raw.fragment_shader)
        vPosition = GLES20.glGetAttribLocation(program,"v_Position")
        fPosition = GLES20.glGetAttribLocation(program,"f_Position")
        //create vbo
        vbo = IntArray(1)
        GLES20.glGenBuffers(1,vbo,0)
        //bind vbo and set vbo data
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER,vbo[0])
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER,vertexPoints.size * 4 + fragmentPoints.size * 4,null,GLES20.GL_STATIC_DRAW)
        //vertexBuffer
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER,0,vertexPoints.size * 4,vertexBuffer)
        //fragmentBuffer
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER,vertexPoints.size * 4,fragmentPoints.size * 4,fragmentBuffer)
        //unbind vbo
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER,0)

        //create fbo need texture
        val texture = IntArray(1)
        GLES20.glGenTextures(1,texture,0)
        fboTextureId = texture[0]
        //bind texture
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,fboTextureId)
        //设置环绕
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_REPEAT)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_REPEAT)
        //设置过滤
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR)

        //create fbo
        fbo = IntArray(1)
        GLES20.glGenBuffers(1,fbo,0)
        //bind fbo and set fboTexture attribute
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,fbo[0])
        //set texture width height
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D,
            0,
            GLES20.GL_RGBA,
            1080,
            600,
            0,
            GLES20.GL_RGBA,
            GLES20.GL_UNSIGNED_BYTE,
            null
        )
        //fbo bind texture
        GLES20.glFramebufferTexture2D(
            GLES20.GL_FRAMEBUFFER,
            GLES20.GL_COLOR_ATTACHMENT0,
            GLES20.GL_TEXTURE_2D,
            fboTextureId,
            0
        )
        //unbind texture
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,0)
        //unbind fbo
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,0)

        fboRender.onCreate()
    }

    override fun onSurfaceChanged(width: Int, height: Int) {
        GLES20.glViewport(0,0,width, height)
        fboRender.onChange(width, height)
    }

    override fun onDrawFrame() {
        GLES20.glClearColor(0f,1f,0f,0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(program)
        //open fbo
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,fbo[0])
        val textureId = DrawUtil.loadTexture(context,imgResId)

        //bind data
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER,vbo[0])
        GLES20.glEnableVertexAttribArray(vPosition)
        GLES20.glVertexAttribPointer(vPosition,2,GLES20.GL_FLOAT,false,8,0)
        GLES20.glEnableVertexAttribArray(fPosition)
        GLES20.glVertexAttribPointer(fPosition,2,GLES20.GL_FLOAT,false,8,vertexPoints.size * 4)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER,0)
        //draw
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,textureId[0])
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,0)
        //close
        GLES20.glDeleteTextures(1,textureId,0)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,0)

//        //fbo draw
        fboRender.onDraw(fboTextureId)
    }

    fun setImageResId(resId:Int){
        imgResId = resId
    }

    private fun loadTexture(resId: Int): Int {
        val textureId = IntArray(1)
        GLES20.glGenTextures(1, textureId, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[0])
        //设置环绕，顶点坐标超出纹理坐标范围时作用，s==x t==y GL_REPEAT 重复
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT)
        //过滤（纹理像素映射到坐标点）：（GL_TEXTURE_MIN_FILTER缩小、GL_TEXTURE_MAG_FILTER放大：GL_LINEAR线性）
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        val bitmap = BitmapFactory.decodeResource(context.resources, resId)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        return textureId[0]
    }
}