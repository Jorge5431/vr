package com.stormvr.app.vrroom

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HandGripClassifierTest {
    private fun pose(curled: Boolean): FloatArray {
        val points = FloatArray(63)
        val fingers = arrayOf(intArrayOf(5, 6, 7, 8), intArrayOf(9, 10, 11, 12), intArrayOf(13, 14, 15, 16), intArrayOf(17, 18, 19, 20))
        for ((n, f) in fingers.withIndex()) {
            val x = n * 0.1f
            val coords = if (curled) arrayOf(floatArrayOf(x, 0f), floatArrayOf(x, 1f), floatArrayOf(x + 0.6f, 1.6f), floatArrayOf(x + 1.2f, 1f))
                else arrayOf(floatArrayOf(x, 0f), floatArrayOf(x, 1f), floatArrayOf(x, 2f), floatArrayOf(x, 3f))
            for (k in f.indices) { points[f[k] * 3] = coords[k][0]; points[f[k] * 3 + 1] = coords[k][1] }
        }
        return points
    }

    @Test fun openFingersDoNotLookLikeAGrip() {
        assertTrue(HandGripClassifier.score(pose(curled = false)) < 0.1f)
    }

    @Test fun curledFingersLookLikeAGrip() {
        assertTrue(HandGripClassifier.score(pose(curled = true)) > 0.9f)
    }

    @Test fun latchNeedsTwoFramesAndHysteresisToRelease() {
        val latch = GripLatch()
        assertFalse(latch.update(0.9f))
        assertTrue(latch.update(0.9f))
        assertTrue(latch.update(0.5f))
        assertTrue(latch.update(0.2f))
        assertFalse(latch.update(0.2f))
    }
}
