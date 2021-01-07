package com.example.livepushcameramedia.egl

import android.content.Context
import android.opengl.GLES20
import android.util.Log
import androidx.annotation.RawRes
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.StringBuilder

/**
 * status 0 is error
 */
class ShaderUtil {
    companion object{
        private const val TAG = "ShaderUtil"
        @JvmStatic
        fun linkProgram(context: Context,@RawRes vertexShaderRawId:Int,@RawRes fragmentShaderRawId:Int):Int{
            val vertexShader = loadShader(getShaderSource(context,vertexShaderRawId),GLES20.GL_VERTEX_SHADER)
            val fragmentShader = loadShader(getShaderSource(context,fragmentShaderRawId),GLES20.GL_FRAGMENT_SHADER)
            return linkProgram(vertexShader,fragmentShader)
        }

        private fun getShaderSource(context: Context,@RawRes rawId:Int):String{
            val inputStream = context.resources.openRawResource(rawId)
            val reader = BufferedReader(InputStreamReader(inputStream))
            var nextLine:String?
            val stringBuilder = StringBuilder()
            while (reader.readLine().apply { nextLine = this } != null){
                stringBuilder.append(nextLine)
                stringBuilder.append("\n")
            }
            reader.close()
            return stringBuilder.toString()
        }

        private fun linkProgram(vertexShader:Int,fragmentShader:Int):Int{
            val program = GLES20.glCreateProgram()
            if (program == 0){
                Log.e(TAG,"GLES20.glCreateProgram() fail")
                return 0
            }
            if(vertexShader != 0 && fragmentShader != 0)
            {
                Log.i(TAG,"success     vertexShader = $vertexShader , fragmentShader = $fragmentShader")
                GLES20.glAttachShader(program,vertexShader)
                GLES20.glAttachShader(program,fragmentShader)
                GLES20.glLinkProgram(program)
                val linkStatus = IntArray(1)
                GLES20.glGetProgramiv(program,GLES20.GL_LINK_STATUS,linkStatus,0)
                if (linkStatus[0] == 0){
                    Log.e(TAG,"GLES20.glLinkProgram(program) fail")
                    GLES20.glDeleteProgram(program)
                    return 0
                }
            }else{
                Log.e(TAG,"fail vertexShader = $vertexShader , fragmentShader = $fragmentShader")
            }
            return program
        }

        private fun loadShader(shaderSource:String,shaderType:Int):Int{
            val shader = GLES20.glCreateShader(shaderType)
            if (shader == 0){
                Log.e(TAG,"GLES20.glCreateShader($shaderType) fail, shader = 0")
                return 0
            }
            GLES20.glShaderSource(shader,shaderSource)
            GLES20.glCompileShader(shader)
            val compileStatus = IntArray(1)
            GLES20.glGetShaderiv(shader,GLES20.GL_COMPILE_STATUS,compileStatus,0)
            if (compileStatus[0] != GLES20.GL_TRUE){
                Log.v(
                    TAG, "Result of compile source \n" +
                            "$shaderSource \n" +
                            "---glShaderInfoLog---\n" +
                            GLES20.glGetShaderInfoLog(shader)
                )
                Log.e(TAG,"GLES20.glCompileShader(shader:$shader) fail")
                GLES20.glDeleteShader(shader)
                return 0
            }
            return shader
        }
    }

}