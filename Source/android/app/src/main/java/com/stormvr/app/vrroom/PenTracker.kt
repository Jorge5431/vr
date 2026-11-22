package com.stormvr.app.vrroom

import kotlin.math.*

/** Approximate dimensions of the photographed STABILO point 88, in metres. */
object PenModel {
    const val LENGTH = .16f
    const val BODY_RADIUS = .004f
    const val CAP_LENGTH = .027f
    const val TAIL_LENGTH = .007f
}

data class PenObservation(
    val hand: ObjectHand, val tipX: Float, val tipY: Float,
    val axisX: Float, val axisY: Float, val projectedLength: Float
)

/** Finds the orange six-sided barrel and distinguishes the long blue cap from the short blue tail. */
object PenTracker {
    private fun orange(c: Int): Boolean {
        val r = c ushr 16 and 255; val g = c ushr 8 and 255; val b = c and 255
        return r > 125 && g > 45 && r > g * 1.26f && g > b * 1.32f && r - b > 95
    }
    private fun blue(c: Int): Boolean {
        val r = c ushr 16 and 255; val g = c ushr 8 and 255; val b = c and 255
        return b > 70 && b > r * 1.35f && b > g * 1.15f && b - r > 35
    }

    fun find(rgb: IntArray, w: Int, h: Int, hands: List<ObjectHand>): PenObservation? {
        if (w <= 0 || h <= 0 || rgb.size < w * h) return null
        var best: PenObservation? = null
        var bestScore = 0f
        for (hand in hands) {
            if (hand.span < 8f || hand.depth <= 0f) continue
            val radius = (hand.span * 2.5f).roundToInt()
            val x0 = max(0, hand.x.roundToInt() - radius); val x1 = min(w - 1, hand.x.roundToInt() + radius)
            val y0 = max(0, hand.y.roundToInt() - radius); val y1 = min(h - 1, hand.y.roundToInt() + radius)
            val step = if (hand.span > 75f) 2 else 1
            val orangePoints = ArrayList<Pair<Float, Float>>()
            val bluePoints = ArrayList<Pair<Float, Float>>()
            for (y in y0..y1 step step) for (x in x0..x1 step step) {
                val c = rgb[y * w + x]
                if (orange(c)) orangePoints.add(Pair(x.toFloat(), y.toFloat()))
                else if (blue(c)) bluePoints.add(Pair(x.toFloat(), y.toFloat()))
            }
            if (orangePoints.size < 18 || bluePoints.size < 6) continue
            val meanX = orangePoints.sumOf { it.first.toDouble() }.toFloat() / orangePoints.size
            val meanY = orangePoints.sumOf { it.second.toDouble() }.toFloat() / orangePoints.size
            var xx = 0f; var yy = 0f; var xy = 0f
            for ((x, y) in orangePoints) {
                val dx = x - meanX; val dy = y - meanY
                xx += dx * dx; yy += dy * dy; xy += dx * dy
            }
            val theta = .5f * atan2(2f * xy, xx - yy)
            val ux = cos(theta); val uy = sin(theta)
            val vx = -uy; val vy = ux
            var minP = Float.POSITIVE_INFINITY; var maxP = Float.NEGATIVE_INFINITY
            var cross2 = 0f
            for ((x, y) in orangePoints) {
                val dx = x - meanX; val dy = y - meanY
                val p = dx * ux + dy * uy
                minP = min(minP, p); maxP = max(maxP, p)
                val q = dx * vx + dy * vy; cross2 += q * q
            }
            val bodyLength = maxP - minP
            val thickness = sqrt(cross2 / orangePoints.size)
            if (bodyLength < max(12f, hand.span * .45f) || bodyLength < thickness * 3f) continue
            val maxCross = max(5f, hand.span * .20f)
            val capLimit = max(10f, bodyLength * .45f)
            var lowMin = 0f; var highMax = 0f
            var lowCount = 0; var highCount = 0
            for ((x, y) in bluePoints) {
                val dx = x - meanX; val dy = y - meanY
                val p = dx * ux + dy * uy; val q = abs(dx * vx + dy * vy)
                if (q > maxCross) continue
                if (p in (minP - capLimit)..(minP + maxCross)) {
                    lowCount++; lowMin = if (lowCount == 1) p else min(lowMin, p)
                }
                if (p in (maxP - maxCross)..(maxP + capLimit)) {
                    highCount++; highMax = if (highCount == 1) p else max(highMax, p)
                }
            }
            val lowExtent = if (lowCount >= 4) minP - lowMin else 0f
            val highExtent = if (highCount >= 4) highMax - maxP else 0f
            if (max(lowExtent, highExtent) < max(3f, bodyLength * .07f)) continue
            if (max(lowExtent, highExtent) < min(lowExtent, highExtent) * 1.3f) continue
            val capHigh = highExtent > lowExtent
            val tipP = if (capHigh) highMax else lowMin
            val total = bodyLength + max(lowExtent, highExtent) + min(lowExtent, highExtent)
            val axisSign = if (capHigh) 1f else -1f
            val score = bodyLength * (1f + max(lowExtent, highExtent) / total)
            if (score > bestScore) {
                bestScore = score
                best = PenObservation(hand, meanX + tipP * ux, meanY + tipP * uy,
                    ux * axisSign, uy * axisSign, total)
            }
        }
        return best
    }
}
