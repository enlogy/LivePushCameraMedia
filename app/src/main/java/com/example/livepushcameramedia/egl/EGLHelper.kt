package com.example.livepushcameramedia.egl

import android.opengl.EGL14.EGL_CONTEXT_CLIENT_VERSION
import android.util.Log
import android.view.Surface
import javax.microedition.khronos.egl.*

class EGLHelper {
    private var mEgl: EGL10? = null
    private var mEglDisplay: EGLDisplay? = null
    var mEglContext: EGLContext? = null
    private var mEglSurface: EGLSurface? = null
    private val mConfigSpec: IntArray = intArrayOf(
        EGL10.EGL_RED_SIZE, 8,
        EGL10.EGL_GREEN_SIZE, 8,
        EGL10.EGL_BLUE_SIZE, 8,
        EGL10.EGL_ALPHA_SIZE, 8,
        EGL10.EGL_DEPTH_SIZE, 8,
        EGL10.EGL_STENCIL_SIZE, 8,
        EGL10.EGL_RENDERABLE_TYPE, 4,
        EGL10.EGL_NONE
    )

    fun configure(surface: Surface, eglContext: EGLContext?) {

        //1.获取EGL实例
        mEgl = EGLContext.getEGL() as EGL10?
        //2.获取默认窗口
        mEglDisplay = mEgl!!.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)
        if (mEglDisplay === EGL10.EGL_NO_DISPLAY) {
            throw RuntimeException("eglGetDisplay failed")
        }

        //3.初始化egl
        val version = IntArray(2)
        if (!mEgl!!.eglInitialize(mEglDisplay, version)) {
            throw java.lang.RuntimeException("eglInitialize failed")
        }
        //4.配置参数
        val num_config = IntArray(1)
        require(
            mEgl!!.eglChooseConfig(
                mEglDisplay, mConfigSpec, null, 0,
                num_config
            )
        ) { "eglChooseConfig failed" }

        val numConfigs = num_config[0]

        require(numConfigs > 0) { "No configs match configSpec" }

        val configs =
            arrayOfNulls<EGLConfig>(numConfigs)
        require(
            mEgl!!.eglChooseConfig(
                mEglDisplay, mConfigSpec, configs, numConfigs,
                num_config
            )
        ) { "eglChooseConfig#2 failed" }

        //5.创建EglContext
        val attrib_list = intArrayOf(
            EGL_CONTEXT_CLIENT_VERSION, 2,
            EGL10.EGL_NONE
        )
        if (eglContext != null) {
            Log.i("EGLHelper","use eglContext")
            mEglContext = mEgl!!.eglCreateContext(
                mEglDisplay, configs[0], eglContext,
                attrib_list
            )
        } else {
            Log.i("EGLHelper","create new eglContext")
            mEglContext = mEgl!!.eglCreateContext(
                mEglDisplay, configs[0], EGL10.EGL_NO_CONTEXT,
                attrib_list
            )
        }
        //6.创建EglSurface
        try {
            mEglSurface = mEgl!!.eglCreateWindowSurface(mEglDisplay, configs[0], surface, null)
        } catch (e: IllegalArgumentException) {
            Log.e("EglHelper", "eglCreateWindowSurface", e)
        }
        //7.绑定EglContext和EglSurface到显示设备中
        if (mEglSurface == null) {
            throw java.lang.RuntimeException("mEglSurface == null")
        }
        if (!mEgl!!.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext)) {
            throw java.lang.RuntimeException("eglMakeCurrent fail")
        }
    }

    fun swapBuffers() {
        if (!mEgl!!.eglSwapBuffers(mEglDisplay, mEglSurface)) {
//            throw java.lang.RuntimeException("eglMakeCurrent fail")
        }
    }

    fun destoryEgl() {
        if (mEgl != null) {
            mEgl!!.eglMakeCurrent(
                mEglDisplay, EGL10.EGL_NO_SURFACE,
                EGL10.EGL_NO_SURFACE,
                EGL10.EGL_NO_CONTEXT
            )

            mEgl!!.eglDestroySurface(mEglDisplay, mEglSurface)
            mEglSurface = null

            mEgl!!.eglDestroyContext(mEglDisplay, mEglContext)
            mEglContext = null

            mEgl!!.eglTerminate(mEglDisplay)
            mEglDisplay = null

            mEgl = null
        }
    }
}