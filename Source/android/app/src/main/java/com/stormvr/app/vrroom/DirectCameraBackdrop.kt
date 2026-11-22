package com.stormvr.app.vrroom

import android.media.Image
import android.opengl.GLES20.*
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max

/** Displays the YUV image used for hand inference and keeps the virtual camera aligned with it. */
internal class DirectCameraBackdrop(private val program: Int) {
    private val textures = IntArray(3)
    private val buffers = arrayOfNulls<ByteBuffer>(3)
    private val uv = ByteBuffer.allocateDirect(32).order(ByteOrder.nativeOrder()).asFloatBuffer()
    private val quad = ByteBuffer.allocateDirect(32).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
        put(floatArrayOf(-1f,-1f,1f,-1f,-1f,1f,1f,1f)); position(0)
    }
    private val posLoc = glGetAttribLocation(program, "aPos")
    private val uvLoc = glGetAttribLocation(program, "aUV")
    private val samplers = IntArray(3) { glGetUniformLocation(program, arrayOf("yTex", "uTex", "vTex")[it]) }
    private var w = 0
    private var h = 0
    private var angle = 0
    private var focal = 1f
    var ready = false
        private set

    init {
        glGenTextures(3, textures, 0)
        for (t in textures) {
            glBindTexture(GL_TEXTURE_2D, t)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
        }
    }

    fun upload(im: Image, rotation: Int, fx: Float) {
        val resize = !ready || w != im.width || h != im.height
        w = im.width; h = im.height; angle = rotation; focal = fx
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1)
        for ((i, plane) in im.planes.withIndex()) {
            val cols = if (i == 0) w else (w + 1) / 2
            val rows = if (i == 0) h else (h + 1) / 2
            val dst = buffers[i]?.takeIf { it.capacity() >= rows * cols }
                ?: ByteBuffer.allocateDirect(rows * cols).also { buffers[i] = it }
            dst.clear()
            val src = plane.buffer.duplicate()
            val limit = src.limit()
            for (row in 0 until rows) {
                val begin = row * plane.rowStride
                if (plane.pixelStride == 1) {
                    src.limit(limit); src.position(begin); src.limit(begin + cols); dst.put(src)
                } else {
                    for (col in 0 until cols) dst.put(src.get(begin + col * plane.pixelStride))
                }
            }
            dst.flip()
            glBindTexture(GL_TEXTURE_2D, textures[i])
            if (resize) glTexImage2D(GL_TEXTURE_2D, 0, GL_LUMINANCE, cols, rows, 0, GL_LUMINANCE, GL_UNSIGNED_BYTE, dst)
            else glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, cols, rows, GL_LUMINANCE, GL_UNSIGNED_BYTE, dst)
        }
        glPixelStorei(GL_UNPACK_ALIGNMENT, 4)
        ready = true
    }

    fun project(out: FloatArray, eyeW: Int, eyeH: Int) {
        val rotated = angle == 90 || angle == 270
        val width = if (rotated) h else w
        val height = if (rotated) w else h
        val scale = max(eyeW.toFloat()/width, eyeH.toFloat()/height)
        val ex = eyeW / (2f * scale * focal)
        val ey = eyeH / (2f * scale * focal)
        Matrix.frustumM(out, 0, -ex*0.05f, ex*0.05f, -ey*0.05f, ey*0.05f, 0.05f, 60f)
        uv.clear()
        for (j in 0..3) {
            val x = (if (j % 2 == 0) -0.5f else 0.5f) * eyeW / (scale * width)
            val y = (if (j < 2) 0.5f else -0.5f) * eyeH / (scale * height)
            val u = when(angle) {90 -> y; 180 -> -x; 270 -> -y; else -> x}
            val v = when(angle) {90 -> -x; 180 -> -y; 270 -> x; else -> y}
            uv.put(0.5f + u); uv.put(0.5f + v)
        }
        uv.flip()
    }

    fun draw() {
        glDisable(GL_DEPTH_TEST); glDepthMask(false); glUseProgram(program)
        for (i in 0..2) {
            glActiveTexture(GL_TEXTURE0 + i); glBindTexture(GL_TEXTURE_2D, textures[i]); glUniform1i(samplers[i], i)
        }
        glEnableVertexAttribArray(posLoc); glEnableVertexAttribArray(uvLoc)
        glVertexAttribPointer(posLoc, 2, GL_FLOAT, false, 0, quad)
        glVertexAttribPointer(uvLoc, 2, GL_FLOAT, false, 0, uv)
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4)
        glActiveTexture(GL_TEXTURE0); glDepthMask(true); glEnable(GL_DEPTH_TEST)
    }

    companion object {
        const val SHADER = """precision mediump float;
            varying vec2 vUV;
            uniform sampler2D yTex; uniform sampler2D uTex; uniform sampler2D vTex;
            void main() {
                float y = 1.164 * (texture2D(yTex, vUV).r - 0.0625);
                float u = texture2D(uTex, vUV).r - 0.5;
                float v = texture2D(vTex, vUV).r - 0.5;
                gl_FragColor = vec4(y + 1.596*v, y - 0.392*u - 0.813*v, y + 2.017*u, 1.0);
            }"""
    }
}
