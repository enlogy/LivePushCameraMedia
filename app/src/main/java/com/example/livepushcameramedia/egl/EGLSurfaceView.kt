package com.example.livepushcameramedia.egl

import android.content.Context
import android.util.AttributeSet
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.lang.ref.WeakReference
import javax.microedition.khronos.egl.EGLContext

open class EGLSurfaceView : SurfaceView, SurfaceHolder.Callback {
    companion object {
        const val RENDERMODE_WHEN_DIRTY = 0
        const val RENDERMODE_CONTINUOUSLY = 1
    }
    private var mGLThread: GLThread? = null
    private var mEGlContext: EGLContext? = null
    private var mSurface: Surface? = null
    private var mEGLSurfaceRender: EGLSurfaceRender? = null
    private var mRenderMode: Int = RENDERMODE_CONTINUOUSLY
    constructor(context: Context?) : this(context, null)
    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        mGLThread = GLThread(WeakReference(this))
        this.holder.addCallback(this)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        mGLThread?.width = width
        mGLThread?.height = height
        mGLThread?.isChange = true
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        destroy()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        if (mSurface == null)
            this.mSurface = holder.surface
        mGLThread?.isCreate = true
        mGLThread?.start()
    }

    fun setRender(render: EGLSurfaceRender) {
        this.mEGLSurfaceRender = render
    }

    fun requestRender() {
        mGLThread?.requestRender()
    }

    fun setSurfaceAndEGLContext(surface: Surface?, eglContext: EGLContext?) {
        this.mSurface = surface
        this.mEGlContext = eglContext
    }

    fun setRenderMode(mode: Int) {
        this.mRenderMode = mode
    }

    private fun destroy() {
        mGLThread!!.exit()
    }

    fun getEglContext(): EGLContext {
        return mGLThread!!.getEglContext()
    }

    class GLThread(var eglSurfaceViewWeakReference: WeakReference<EGLSurfaceView>) : Thread() {
        var isCreate = false
        var isChange = false
        var width = 0
        var height = 0
        private var lock: Object? = null
        private var eglHelper: EGLHelper? = null
        private var isExit = false
        override fun run() {
            super.run()
            lock = Object()
            eglHelper = EGLHelper()
            eglHelper!!.configure(
                eglSurfaceViewWeakReference.get()?.mSurface!!,
                eglSurfaceViewWeakReference.get()?.mEGlContext
            )
            while (true) {
                if (isExit) {
                    //release
                    eglHelper!!.destoryEgl()
                    break
                }
                onCreate()
                onChange()
                onDraw()
                when (eglSurfaceViewWeakReference.get()?.mRenderMode) {
                    RENDERMODE_WHEN_DIRTY -> {
                        synchronized(lock!!) {
                            lock!!.wait()
                        }
                    }
                    RENDERMODE_CONTINUOUSLY -> {
                        sleep(1000 / 60)
                    }
                    else -> {
                        throw RuntimeException("mRenderMode is error")
                    }
                }
            }
        }

        private fun onCreate() {
            if (isCreate) {
                val render = eglSurfaceViewWeakReference.get()?.mEGLSurfaceRender
                    ?: throw RuntimeException("mEGLSurfaceRender is null")
                render.onSurfaceCreated()
                isCreate = false
            }
        }

        private fun onChange() {
            if (isChange) {
                val render = eglSurfaceViewWeakReference.get()?.mEGLSurfaceRender
                    ?: throw RuntimeException("mEGLSurfaceRender is null")
                render.onSurfaceChanged(width, height)
                isChange = false
            }
        }

        private fun onDraw() {
            val render = eglSurfaceViewWeakReference.get()?.mEGLSurfaceRender
                ?: throw RuntimeException("mEGLSurfaceRender is null")
            render.onDrawFrame()
            eglHelper!!.swapBuffers()
        }

        fun requestRender() {
            synchronized(lock!!) {
                lock!!.notify()
            }
        }

        fun exit() {
            isExit = true
            synchronized(lock!!) {
                lock!!.notify()
            }
        }

        fun getEglContext(): EGLContext {
            return eglHelper!!.mEglContext!!
        }
    }
}