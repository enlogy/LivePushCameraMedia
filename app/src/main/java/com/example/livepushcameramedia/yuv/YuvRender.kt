package com.example.livepushcameramedia.yuv

import android.content.Context
import android.opengl.GLES20
import com.example.livepushcameramedia.R
import com.example.livepushcameramedia.egl.EGLSurfaceRender
import com.example.livepushcameramedia.egl.ShaderUtil
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class YuvRender(var context: Context):EGLSurfaceRender {
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
    private var vertexBuffer: FloatBuffer
    private var fragmentBuffer: FloatBuffer
    //vbo
    private lateinit var vbo:IntArray
    private lateinit var fbo:IntArray
    var fboTextureId:Int = 0
    private var program = 0
    private var vPosition = 0
    private var fPosition = 0
    private var ySampler = 0
    private var uSampler = 0
    private var vSampler = 0
    private lateinit var samplerTextures:IntArray
    private val fboRender = YuvFboRender(context)
    var w = 0
    var h = 0

    var y: Buffer? = null
    var u: Buffer? = null
    var v: Buffer? = null

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
    }
    override fun onSurfaceCreated() {
        program = ShaderUtil.linkProgram(context, R.raw.vertex_shader, R.raw.fragment_shader_yuv)
        vPosition = GLES20.glGetAttribLocation(program,"v_Position")
        fPosition = GLES20.glGetAttribLocation(program,"f_Position")
        ySampler = GLES20.glGetUniformLocation(program,"sampler_y")
        uSampler = GLES20.glGetUniformLocation(program,"sampler_u")
        vSampler = GLES20.glGetUniformLocation(program,"sampler_v")

        samplerTextures = IntArray(3)
        GLES20.glGenTextures(3,samplerTextures,0)
        for (i in 0..2){
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,samplerTextures[i])
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR
            )
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR
            )
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE
            )
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE
            )
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,0)
        }

        //create vbo
        vbo = IntArray(1)
        GLES20.glGenBuffers(1,vbo,0)
        //bind vbo and set vbo data
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER,vbo[0])
        GLES20.glBufferData(
            GLES20.GL_ARRAY_BUFFER,vertexPoints.size * 4 + fragmentPoints.size * 4,null,
            GLES20.GL_STATIC_DRAW)
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
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT)
        //设置过滤
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)

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
            720,
            500,
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
        GLES20.glClearColor(1f,0f,0f,1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(program)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,fbo[0])
        if (w > 0 && h > 0 && y != null && u != null && v != null) {
            GLES20.glUseProgram(program)
            GLES20.glEnableVertexAttribArray(vPosition)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER,vbo[0])
            GLES20.glVertexAttribPointer(
                vPosition, 2, GLES20.GL_FLOAT, false,
                8, 0
            )
            GLES20.glEnableVertexAttribArray(fPosition)
            GLES20.glVertexAttribPointer(fPosition, 2, GLES20.GL_FLOAT, false, 8, vertexPoints.size * 4)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER,0)
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, samplerTextures[0])
            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D,
                0,
                GLES20.GL_LUMINANCE,
                w,
                h,
                0,
                GLES20.GL_LUMINANCE,
                GLES20.GL_UNSIGNED_BYTE,
                y
            )
            GLES20.glUniform1i(ySampler, 0)
            GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, samplerTextures[1])
            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D,
                0,
                GLES20.GL_LUMINANCE,
                w / 2,
                h / 2,
                0,
                GLES20.GL_LUMINANCE,
                GLES20.GL_UNSIGNED_BYTE,
                u
            )
            GLES20.glUniform1i(uSampler, 1)
            GLES20.glActiveTexture(GLES20.GL_TEXTURE2)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, samplerTextures[2])
            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D,
                0,
                GLES20.GL_LUMINANCE,
                w / 2,
                h / 2,
                0,
                GLES20.GL_LUMINANCE,
                GLES20.GL_UNSIGNED_BYTE,
                v
            )
            GLES20.glUniform1i(vSampler, 2)
            y?.clear()
            u?.clear()
            v?.clear()
            y = null
            u = null
            v = null
        }
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,0)
        fboRender.onDraw(fboTextureId)
    }

    fun setFrameData(
        w: Int,
        h: Int,
        by: ByteArray,
        bu: ByteArray,
        bv: ByteArray
    ) {
        this.w = w
        this.h = h
        y = ByteBuffer.wrap(by)
        u = ByteBuffer.wrap(bu)
        v = ByteBuffer.wrap(bv)
    }
}