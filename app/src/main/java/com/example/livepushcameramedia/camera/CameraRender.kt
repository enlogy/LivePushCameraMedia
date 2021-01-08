package com.example.livepushcameramedia.camera

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES
import android.opengl.GLES20
import android.opengl.GLUtils
import android.opengl.Matrix
import com.blankj.utilcode.util.ScreenUtils
import com.example.livepushcameramedia.FboRender
import com.example.livepushcameramedia.R
import com.example.livepushcameramedia.egl.EGLSurfaceRender
import com.example.livepushcameramedia.egl.ShaderUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class CameraRender(var context: Context) : EGLSurfaceRender,
    SurfaceTexture.OnFrameAvailableListener {
    private val vertexPoints = floatArrayOf(
        -1f, -1f,
        1f, -1f,
        -1f, 1f,
        1f, 1f
    )

    //fbo坐标
    private val fragmentPoints = floatArrayOf(
        0f, 0f,
        1f, 0f,
        0f, 1f,
        1f, 1f
    )
    private var vertexBuffer: FloatBuffer
    private var fragmentBuffer: FloatBuffer
    private var screenWidth: Int
    private var screenHeight: Int
    var fboTextureId = 0
    private var imgTextureId = 0
    private var vPosition = 0
    private var fPosition = 0
    private var uMatrix = 0
    private val matrix = FloatArray(16)
    private var program = 0
    private lateinit var fbo: IntArray
    private lateinit var vbo: IntArray
    private var fboRender = FboRender(context)
    private var bwRender = FboBlackWhiteRender(context)
    var surfaceTextureListener: SurfaceTextureListener? = null
    private lateinit var surfaceTexture: SurfaceTexture
    private var mWidth = 0
    private var mHeight = 0
    private var isBlackWhite = false
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

        //初始化矩阵为0
        Matrix.setIdentityM(matrix, 0)
    }

    @SuppressLint("Recycle")
    override fun onSurfaceCreated() {
        //创建vbo
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
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)

        //创建fbo绑定所需的texture
        val textureId = IntArray(1)
        GLES20.glGenTextures(1, textureId, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[0])
        fboTextureId = textureId[0]
        //设置环绕，顶点坐标超出纹理坐标范围时作用，s==x t==y GL_REPEAT 重复
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT)
        //过滤（纹理像素映射到坐标点）：（GL_TEXTURE_MIN_FILTER缩小、GL_TEXTURE_MAG_FILTER放大：GL_LINEAR线性）
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        //创建fbo
        fbo = IntArray(1)
        GLES20.glGenBuffers(1, fbo, 0)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo[0])
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D,
            0,
            GLES20.GL_RGBA,
            screenWidth,
            screenHeight,
            0,
            GLES20.GL_RGBA,
            GLES20.GL_UNSIGNED_BYTE,
            null
        )
        GLES20.glFramebufferTexture2D(
            GLES20.GL_FRAMEBUFFER,
            GLES20.GL_COLOR_ATTACHMENT0,
            GLES20.GL_TEXTURE_2D,
            fboTextureId,
            0
        )
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)

        //创建SurfaceTexture所需的Texture
        val cameraTextureId = IntArray(1)
        GLES20.glGenTextures(1, cameraTextureId, 0)
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, cameraTextureId[0])
        //设置环绕，顶点坐标超出纹理坐标范围时作用，s==x t==y GL_REPEAT 重复
        GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT)
        GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT)
        //过滤（纹理像素映射到坐标点）：（GL_TEXTURE_MIN_FILTER缩小、GL_TEXTURE_MAG_FILTER放大：GL_LINEAR线性）
        GLES20.glTexParameteri(
            GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_LINEAR
        )
        GLES20.glTexParameteri(
            GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR
        )
        //创建SurfaceTexture跟Texture绑定且监听SurfaceTexture的可用数据
        surfaceTexture = SurfaceTexture(cameraTextureId[0])
        surfaceTexture.setOnFrameAvailableListener(this)
        surfaceTextureListener?.onCreate(surfaceTexture)
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, 0)

        //创建用于渲染到fbo的图片
        imgTextureId = loadTexture(R.mipmap.mimg)

        //获取shader属性
        program = ShaderUtil.linkProgram(
            context,
            R.raw.vertex_shader_camera,
            R.raw.fragment_shader_camera
        )
        vPosition = GLES20.glGetAttribLocation(program, "v_Position")
        fPosition = GLES20.glGetAttribLocation(program, "f_Position")
        uMatrix = GLES20.glGetUniformLocation(program, "u_Matrix")

        fboRender.onCreate()
        bwRender.onCreate()
    }

    override fun onSurfaceChanged(width: Int, height: Int) {
        this.mWidth = width
        this.mHeight = height
        GLES20.glViewport(0, 0, width, height)
        fboRender.onChange(width, height)
        bwRender.onChange(width, height)
        //通过矩阵改变摄像头的显示角度
        Matrix.setIdentityM(matrix, 0)
        val rotation = ScreenUtils.getScreenRotation(context as Activity)
        when (rotation) {
            0 -> Matrix.rotateM(matrix, 0, -90f, 0f, 0f, 1f)
//            90 -> Matrix.rotateM(matrix,0,-180f,0f,0f,1f)
//            180 -> Matrix.rotateM(matrix,0,90f,0f,0f,1f)
            270 -> Matrix.rotateM(matrix, 0, 180f, 0f, 0f, 1f)
        }
    }

    override fun onDrawFrame() {
        surfaceTexture.updateTexImage()
        GLES20.glClearColor(1f, 0f, 0f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(program)

        //绑定矩阵
        GLES20.glUniformMatrix4fv(uMatrix, 1, false, matrix, 0)
        //vbo加载顶点和纹理数据
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo[0])
        GLES20.glVertexAttribPointer(vPosition, 2, GLES20.GL_FLOAT, false, 8, 0)
        GLES20.glVertexAttribPointer(fPosition, 2, GLES20.GL_FLOAT, false, 8, vertexPoints.size * 4)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        //fbo
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo[0])
        //渲染fbo的texture生成一个图片
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fboTextureId)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        //绘制fbo中的texture
        if (isBlackWhite){
            bwRender.onDraw(fboTextureId)
        }else{
            fboRender.onDraw(fboTextureId)
        }

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

    interface SurfaceTextureListener {
        fun onCreate(surfaceTexture: SurfaceTexture)
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {

    }
    fun setBlackWhiteRender(){
        isBlackWhite = !isBlackWhite
    }
}
