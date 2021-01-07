package com.example.livepushcameramedia.egl

interface EGLSurfaceRender {
     fun onSurfaceCreated()
     fun onSurfaceChanged(width: Int, height: Int)
     fun onDrawFrame()
}