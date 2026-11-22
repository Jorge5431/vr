package com.stormvr.app.vrroom

import java.util.concurrent.atomic.AtomicReference
import kotlin.math.sqrt

data class ObjectPose(val slot: Int, val x: Float, val y: Float, val z: Float, val at: Long)

/** Camera worker publishes an immutable scene-space pose for the UI and PC sender. */
class ObjectController {
    @Volatile var enabled = false
    @Volatile var pattern: ObjectAppearance? = null
    val capture = AtomicReference<((CameraPhoto) -> Unit)?>(null)
    @Volatile private var locked: ObjectPose? = null
    private var previous: ObjectPose? = null

    fun live(now: Long = System.nanoTime()): ObjectPose? =
        if (enabled) locked?.takeIf { now - it.at in 0..180_000_000L } else null

    fun clear() { locked = null; previous = null }

    fun update(rgb: IntArray, w: Int, h: Int, hands: List<ObjectHand>, fx: Float, fy: Float,
               cx: Float, cy: Float, pose: FloatArray, now: Long) {
        if (hands.isNotEmpty()) capture.getAndSet(null)?.invoke(CameraPhoto(rgb.copyOf(), w, h, hands))
        if (!enabled) { clear(); return }
        val model = pattern ?: run { clear(); return }
        val found = model.find(rgb, w, h, hands)
        if (found == null) { previous = null; return }
        val z = found.hand.depth
        val x = (found.x - cx) / fx * z
        val y = -(found.y - cy) / fy * z
        val raw = ObjectPose(found.hand.slot, pose[0]*x + pose[4]*y - pose[8]*z + pose[12],
            pose[1]*x + pose[5]*y - pose[9]*z + pose[13],
            pose[2]*x + pose[6]*y - pose[10]*z + pose[14], now)
        val prior = previous
        previous = raw
        if (prior == null || prior.slot != raw.slot || now - prior.at > 260_000_000L) return
        val dx = raw.x - prior.x; val dy = raw.y - prior.y; val dz = raw.z - prior.z
        if (sqrt(dx*dx + dy*dy + dz*dz) > .18f) return
        val old = live(now)
        if (old == null || old.slot != raw.slot) { locked = raw; return }
        val alpha = (0.42f + 3f * sqrt(dx*dx + dy*dy + dz*dz)).coerceAtMost(.92f)
        locked = ObjectPose(raw.slot, old.x + alpha*(raw.x - old.x), old.y + alpha*(raw.y - old.y),
            old.z + alpha*(raw.z - old.z), now)
    }
}
