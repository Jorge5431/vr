package com.stormvr.app.vrroom

import kotlin.math.sqrt

/** Estimates a grasp from MediaPipe's 21 hand landmarks. This does not identify the object's category. */
object HandGripClassifier {
    private val fingers = arrayOf(
        intArrayOf(5, 6, 7, 8),
        intArrayOf(9, 10, 11, 12),
        intArrayOf(13, 14, 15, 16),
        intArrayOf(17, 18, 19, 20),
    )

    /** Returns a 0..1 curl score from the ratio of each finger's chord to its joint path. */
    fun score(points: FloatArray): Float {
        if (points.size < 63) return 0f
        var total = 0f
        for (finger in fingers) {
            val a = finger[0]; val b = finger[1]; val c = finger[2]; val d = finger[3]
            val path = distance(points, a, b) + distance(points, b, c) + distance(points, c, d)
            if (path <= 1e-5f) continue
            val straightness = distance(points, a, d) / path
            total += ((0.88f - straightness) / 0.30f).coerceIn(0f, 1f)
        }
        return total / fingers.size
    }

    private fun distance(p: FloatArray, a: Int, b: Int): Float {
        val i = a * 3; val j = b * 3
        val x = p[i] - p[j]; val y = p[i + 1] - p[j + 1]; val z = p[i + 2] - p[j + 2]
        return sqrt(x * x + y * y + z * z)
    }
}

/** Hysteresis prevents brief landmark noise from repeatedly pressing and releasing a grip. */
class GripLatch(private val onThreshold: Float = 0.62f, private val offThreshold: Float = 0.38f) {
    var isGripping = false
        private set
    private var onFrames = 0
    private var offFrames = 0

    fun update(score: Float): Boolean {
        if (score >= onThreshold) {
            onFrames++
            offFrames = 0
            if (onFrames >= 2) isGripping = true
        } else if (score <= offThreshold) {
            offFrames++
            onFrames = 0
            if (offFrames >= 2) isGripping = false
        } else {
            onFrames = 0
            offFrames = 0
        }
        return isGripping
    }

    fun reset() { isGripping = false; onFrames = 0; offFrames = 0 }
}
