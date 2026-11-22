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
            val radius = (hand.span * 3.2f).roundToInt()
            val x0 = max(0, hand.x.roundToInt() - radius); val x1 = min(w - 1, hand.x.roundToInt() + radius)
            val y0 = max(0, hand.y.roundToInt() - radius); val y1 = min(h - 1, hand.y.roundToInt() + radius)
            val step = if (hand.span > 75f) 2 else 1
            val gridW = (x1 - x0) / step + 1
            val gridH = (y1 - y0) / step + 1
            val mask = ByteArray(gridW * gridH)
            val queue = IntArray(mask.size)
            val bluePoints = ArrayList<Pair<Float, Float>>()
            for (gy in 0 until gridH) for (gx in 0 until gridW) {
                val x = x0 + gx * step; val y = y0 + gy * step
                val c = rgb[y * w + x]
                val index = gy * gridW + gx
                if (orange(c)) mask[index] = 1
                else if (blue(c)) bluePoints.add(Pair(x.toFloat(), y.toFloat()))
            }
            if (bluePoints.size < 4) continue
            for (seed in mask.indices) {
                if (mask[seed].toInt() != 1) continue
                var count = 1; var head = 0
                queue[0] = seed; mask[seed] = 2
                var sumX = 0.0; var sumY = 0.0
                while (head < count) {
                    val index = queue[head++]
                    val gx = index % gridW; val gy = index / gridW
                    sumX += x0 + gx * step; sumY += y0 + gy * step
                    for (ny in max(0, gy - 1)..min(gridH - 1, gy + 1)) {
                        for (nx in max(0, gx - 1)..min(gridW - 1, gx + 1)) {
                            val neighbor = ny * gridW + nx
                            if (mask[neighbor].toInt() == 1) {
                                mask[neighbor] = 2
                                queue[count++] = neighbor
                            }
                        }
                    }
                }
                if (count < 8) continue
                val meanX = (sumX / count).toFloat(); val meanY = (sumY / count).toFloat()
                var xx = 0f; var yy = 0f; var xy = 0f
                for (i in 0 until count) {
                    val index = queue[i]
                    val dx = x0 + (index % gridW) * step - meanX
                    val dy = y0 + (index / gridW) * step - meanY
                    xx += dx * dx; yy += dy * dy; xy += dx * dy
                }
                val theta = .5f * atan2(2f * xy, xx - yy)
                val ux = cos(theta); val uy = sin(theta)
                val vx = -uy; val vy = ux
                var minP = Float.POSITIVE_INFINITY; var maxP = Float.NEGATIVE_INFINITY
                var cross2 = 0f
                for (i in 0 until count) {
                    val index = queue[i]
                    val dx = x0 + (index % gridW) * step - meanX
                    val dy = y0 + (index / gridW) * step - meanY
                    val p = dx * ux + dy * uy
                    minP = min(minP, p); maxP = max(maxP, p)
                    val q = dx * vx + dy * vy; cross2 += q * q
                }
                val bodyLength = maxP - minP
                val thickness = sqrt(cross2 / count)
                if (bodyLength < max(10f, hand.span * .25f) || bodyLength < thickness * 4f) continue
                val maxCross = max(5f, hand.span * .14f)
                val capLimit = max(12f, bodyLength * .55f)
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
                val lowExtent = if (lowCount >= 3) minP - lowMin else 0f
                val highExtent = if (highCount >= 3) highMax - maxP else 0f
                if (max(lowExtent, highExtent) < max(2f, bodyLength * .05f)) continue
                if (max(lowExtent, highExtent) < min(lowExtent, highExtent) * 1.2f &&
                    max(lowCount, highCount) < min(lowCount, highCount) * 1.3f) continue
                // A mão pode esconder parte do corpo e criar um vão antes da cauda curta.
                // Compare a área azul visível antes de usar a distância ao corpo.
                val capHigh = when {
                    highCount > lowCount * 1.3f -> true
                    lowCount > highCount * 1.3f -> false
                    else -> highExtent > lowExtent
                }
                val tipP = if (capHigh) highMax else lowMin
                val total = bodyLength + max(lowExtent, highExtent) + min(lowExtent, highExtent)
                val axisSign = if (capHigh) 1f else -1f
                val handDistance = hypot(meanX - hand.x, meanY - hand.y)
                val capExtent = if (capHigh) highExtent else lowExtent
                val score = bodyLength * (1f + capExtent / total) /
                    (1f + handDistance / hand.span * .25f)
                if (score > bestScore) {
                    bestScore = score
                    best = PenObservation(hand, meanX + tipP * ux, meanY + tipP * uy,
                        ux * axisSign, uy * axisSign, total)
                }
            }
        }
        return best
    }
}
