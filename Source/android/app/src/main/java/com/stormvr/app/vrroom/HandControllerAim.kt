package com.stormvr.app.vrroom

import kotlin.math.sqrt

/** Palm direction for a virtual controller. Fingertip motion must not steer its aim. */
object HandControllerAim {
    fun direction(points: FloatArray, out: FloatArray): Boolean {
        if (points.size < 63 || out.size < 3) return false
        val x = (points[15] + 2f * points[27] + points[39]) * .25f - points[0]
        val y = (points[16] + 2f * points[28] + points[40]) * .25f - points[1]
        val z = (points[17] + 2f * points[29] + points[41]) * .25f - points[2]
        val length = sqrt(x * x + y * y + z * z)
        if (!length.isFinite() || length < .015f) return false
        out[0] = x / length
        out[1] = y / length
        out[2] = z / length
        return true
    }
}
