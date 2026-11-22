package com.stormvr.app.vrroom

import kotlin.math.*

/** All image coordinates are in the upright RGB camera frame. */
data class ObjectHand(val slot: Int, val x: Float, val y: Float, val span: Float, val depth: Float)
data class ObjectMatch(val hand: ObjectHand, val x: Float, val y: Float, val error: Float)
data class CameraPhoto(val rgb: IntArray, val w: Int, val h: Int, val hands: List<ObjectHand>)
data class ObjectAppearance(val pixels: IntArray) {
    companion object {
        private const val GRID = 5
        private const val HALF = 2
        private fun spacing(span: Float) = max(1f, span * .065f)
        private val angles = Array(8) {
            val a = it * PI / 4
            floatArrayOf(cos(a).toFloat(), sin(a).toFloat())
        }

        fun register(photo: CameraPhoto, x: Int, y: Int): ObjectAppearance? {
            val hand = photo.hands.filter { hypot(x - it.x, y - it.y) <= it.span * 1.65f }
                .minByOrNull { hypot(x - it.x, y - it.y) / it.span } ?: return null
            val step = spacing(hand.span)
            if (x < 2 * step || y < 2 * step || x + 2 * step >= photo.w || y + 2 * step >= photo.h) return null
            val pixels = IntArray(GRID * GRID) { i ->
                val px = (x + (i % GRID - HALF) * step).roundToInt().coerceIn(0, photo.w - 1)
                val py = (y + (i / GRID - HALF) * step).roundToInt().coerceIn(0, photo.h - 1)
                photo.rgb[py * photo.w + px]
            }
            // A flat patch cannot constrain location; ask for a tip, edge or visible marking.
            val spread = (0..2).maxOf { channel ->
                val values = pixels.map { (it ushr (channel * 8)) and 255 }
                values.maxOrNull()!! - values.minOrNull()!!
            }
            return if (spread >= 32) ObjectAppearance(pixels) else null
        }

        fun decode(saved: String?): ObjectAppearance? {
            val values = saved?.split(',') ?: return null
            if (values.size != GRID * GRID) return null
            val rgb = IntArray(values.size)
            for (i in values.indices) rgb[i] = values[i].toIntOrNull() ?: return null
            return ObjectAppearance(rgb)
        }
    }

    fun encode() = pixels.joinToString(",")

    fun find(rgb: IntArray, w: Int, h: Int, hands: List<ObjectHand>, previous: ObjectMatch? = null): ObjectMatch? {
        var best: ObjectMatch? = null
        for (hand in hands) {
            if (hand.span < 8f || (previous != null && hand.slot != previous.hand.slot)) continue
            val spacing = spacing(hand.span)
            val hint = previous?.takeIf { it.hand.slot == hand.slot }
            val centerX = hint?.let { it.x + hand.x-it.hand.x } ?: hand.x
            val centerY = hint?.let { it.y + hand.y-it.hand.y } ?: hand.y
            val radius = (hand.span * if (hint != null) .65f else 1.65f).roundToInt()
            val step = max(1, (hand.span * 1.65f / 16f).roundToInt())
            val border = ceil(4 * spacing).toInt() + 1
            val x0 = max(border, centerX.toInt() - radius)
            val x1 = min(w - border - 1, centerX.toInt() + radius)
            val y0 = max(border, centerY.toInt() - radius)
            val y1 = min(h - border - 1, centerY.toInt() + radius)
            if (x1 < x0 || y1 < y0) continue
            val offsets = floatArrayOf(.8f, 1f, 1.25f).flatMap { scale -> angles.map { a -> IntArray(GRID * GRID) { i ->
                val dx = i % GRID - HALF
                val dy = i / GRID - HALF
                ((dx * a[1] + dy * a[0]) * spacing * scale).roundToInt() * w +
                    ((dx * a[0] - dy * a[1]) * spacing * scale).roundToInt()
            } } }
            // Compensate exposure changes without erasing the patch's color structure.
            val templateMean = pixels.sumOf { ((it and 255) + ((it ushr 8) and 255) + ((it ushr 16) and 255)).toDouble() }.toFloat()/75f
            fun score(x: Int, y: Int, limit: Float): Float {
                val base = y*w+x
                var bestSum = limit*75f
                for (rotation in offsets) {
                    var mean=0f; var low=255; var high=0
                    for (offset in rotation) {
                        val c=rgb[base+offset]
                        val value=((c and 255)+((c ushr 8) and 255)+((c ushr 16) and 255))/3
                        mean+=value;low=min(low,value);high=max(high,value)
                    }
                    if (high-low < 16) continue
                    val shift=(mean/25f-templateMean).coerceIn(-80f,80f)
                    var sum=0f
                    for (i in pixels.indices) {
                        val a=pixels[i];val b=rgb[base+rotation[i]]
                        sum+=abs((a and 255)-(b and 255)+shift)+
                            abs(((a ushr 8) and 255)-((b ushr 8) and 255)+shift)+
                            abs(((a ushr 16) and 255)-((b ushr 16) and 255)+shift)
                        if (sum >= bestSum) break
                    }
                    if(sum<bestSum)bestSum=sum
                }
                return bestSum/75f
            }
            var candidateX = x0
            var candidateY = y0
            var error = 45f
            for (y in y0..y1 step step) for (x in x0..x1 step step) {
                val ex = x - centerX; val ey = y - centerY
                if (ex * ex + ey * ey > radius * radius) continue
                val value = score(x, y, error)
                if (value + .02f < error) { error = value; candidateX = x; candidateY = y }
            }
            if (error >= 45f) continue
            val coarseX = candidateX
            val coarseY = candidateY
            for (y in max(y0, coarseY - step)..min(y1, coarseY + step)) {
                for (x in max(x0, coarseX - step)..min(x1, coarseX + step)) {
                    val value = score(x, y, error)
                    if (value + .02f < error) { error = value; candidateX = x; candidateY = y }
                }
            }
            if (error <= 24f && (best == null || error < best.error)) {
                best = ObjectMatch(hand, candidateX.toFloat(), candidateY.toFloat(), error)
            }
        }
        return best
    }
}
