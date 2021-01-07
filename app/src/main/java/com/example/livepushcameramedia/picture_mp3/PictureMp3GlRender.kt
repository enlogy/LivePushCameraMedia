package com.example.livepushcameramedia.picture_mp3

import android.content.Context
import android.opengl.GLES20
import com.example.livepushcameramedia.R
import com.example.livepushcameramedia.egl.EGLSurfaceRender
import com.example.livepushcameramedia.egl.ShaderUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class PictureMp3GlRender(var context: Context,var textureId:Int):EGLSurfaceRender {
    private var vertexPoints = floatArrayOf(
        -1f, -1f,
        1f, -1f,
        -1f, 1f,
        1f, 1f
    )
    private var fragmentPoints = floatArrayOf(
        0f, 1f,
        1f, 1f,
        0f, 0f,
        1f, 0f
    )
    private var vertexBuffer: FloatBuffer
    private var fragmentBuffer: FloatBuffer
    private var vPosition = 0
    private var fPosition = 0
    private var program = 0
    private lateinit var vbo: IntArray

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
        program = ShaderUtil.linkProgram(context, R.raw.vertex_shader, R.raw.fragment_shader)
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
    }

    override fun onSurfaceChanged(width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame() {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glClearColor(0f, 1f, 0f, 1f)
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
    }
}