package com.stormvr.app.vrroom

import java.util.concurrent.atomic.AtomicReference
import kotlin.math.sqrt

data class ObjectPose(val slot: Int, val x: Float, val y: Float, val z: Float, val at: Long,
                      val pen: Boolean = false, val dirX: Float = 0f, val dirY: Float = 0f, val dirZ: Float = 0f)

/** Camera worker publishes an immutable scene-space pose for the UI and PC sender. */
class ObjectController {
    @Volatile var enabled = false
    @Volatile var penMode = false
    @Volatile var pattern: ObjectAppearance? = null
    @Volatile var alternatePattern: ObjectAppearance? = null
    val capture = AtomicReference<((CameraPhoto) -> Unit)?>(null)
    @Volatile private var locked: ObjectPose? = null
    private var previous: ObjectPose? = null
    private var match: ObjectMatch? = null
    private var misses = 0
    private var imageWidth = 0
    private var imageHeight = 0
    private var registered: ObjectAppearance? = null
    private var registeredAlternate: ObjectAppearance? = null
    @Volatile var status = "Cadastre uma marca com contraste"
    @Volatile var processingMs = 0L

    fun live(now: Long = System.nanoTime()): ObjectPose? =
        if (enabled) locked?.takeIf { now - it.at in 0..120_000_000L } else null

    fun clear() { locked = null; previous = null; match = null; misses = 0 }

    fun update(rgb: IntArray, w: Int, h: Int, hands: List<ObjectHand>, fx: Float, fy: Float,
               cx: Float, cy: Float, pose: FloatArray, now: Long) {
        if (hands.isNotEmpty()) capture.getAndSet(null)?.invoke(CameraPhoto(rgb.copyOf(), w, h, hands))
        if (!enabled) { clear(); return }
        val model = pattern
        if (!penMode && model == null) { clear(); return }
        val alternate = alternatePattern
        if (imageWidth != w || imageHeight != h || registered !== model || registeredAlternate !== alternate) {
            clear(); imageWidth=w; imageHeight=h; registered=model; registeredAlternate=alternate
        }
        val begin=System.nanoTime()
        val pen = if (penMode) PenTracker.find(rgb, w, h, hands) else null
        val found = if (pen == null) model?.find(rgb, w, h, hands, match)
            ?: alternate?.find(rgb, w, h, hands, match) else null
        processingMs=(System.nanoTime()-begin)/1_000_000L
        if (pen == null && found == null) {
            previous = null; misses++
            if (misses >= 3) match=null
            status=if (hands.isEmpty()) "Mostre a mão que segura o objeto" else if (penMode)
                "Caneta não vista: mostre o corpo laranja e a tampa azul" else "Marca perdida: melhore a luz ou recadastre"
            return
        }
        if (found != null) match=found
        misses=0
        status=if (pen != null) "Caneta 3D aproximada • ${processingMs} ms" else "Marca localizada • ${processingMs} ms"
        val hand = pen?.hand ?: found!!.hand
        val imageX = pen?.tipX ?: found!!.x
        val imageY = pen?.tipY ?: found!!.y
        val z = hand.depth
        val x = (imageX - cx) / fx * z
        val y = -(imageY - cy) / fy * z
        var dirX = 0f; var dirY = 0f; var dirZ = 0f
        if (pen != null) {
            var dx = pen.axisX * pen.projectedLength / fx * z
            var dy = -pen.axisY * pen.projectedLength / fy * z
            val lateral = sqrt(dx*dx + dy*dy)
            val limit = PenModel.LENGTH * .98f
            if (lateral > limit) { dx *= limit/lateral; dy *= limit/lateral }
            val dz = sqrt((PenModel.LENGTH*PenModel.LENGTH - dx*dx - dy*dy).coerceAtLeast(0f))
            dirX = pose[0]*dx + pose[4]*dy - pose[8]*dz
            dirY = pose[1]*dx + pose[5]*dy - pose[9]*dz
            dirZ = pose[2]*dx + pose[6]*dy - pose[10]*dz
            val norm = sqrt(dirX*dirX + dirY*dirY + dirZ*dirZ).coerceAtLeast(.001f)
            dirX /= norm; dirY /= norm; dirZ /= norm
        }
        val raw = ObjectPose(hand.slot, pose[0]*x + pose[4]*y - pose[8]*z + pose[12],
            pose[1]*x + pose[5]*y - pose[9]*z + pose[13],
            pose[2]*x + pose[6]*y - pose[10]*z + pose[14], now, pen != null, dirX, dirY, dirZ)
        val prior = previous
        previous = raw
        if (prior == null || prior.slot != raw.slot || prior.pen != raw.pen || now - prior.at > 260_000_000L) return
        val dx = raw.x - prior.x; val dy = raw.y - prior.y; val dz = raw.z - prior.z
        if (sqrt(dx*dx + dy*dy + dz*dz) > .18f) return
        val old = live(now)
        if (old == null || old.slot != raw.slot || old.pen != raw.pen) { locked = raw; return }
        val dt=((now-old.at)/1e9f).coerceIn(.001f,.12f)
        val speed=sqrt(dx*dx + dy*dy + dz*dz)/dt
        val alpha=1f-kotlin.math.exp(-dt/(.045f/(1f+speed)))
        var ax = old.dirX + alpha*(raw.dirX - old.dirX)
        var ay = old.dirY + alpha*(raw.dirY - old.dirY)
        var az = old.dirZ + alpha*(raw.dirZ - old.dirZ)
        if (raw.pen) {
            val norm = sqrt(ax*ax + ay*ay + az*az).coerceAtLeast(.001f)
            ax /= norm; ay /= norm; az /= norm
        }
        locked = ObjectPose(raw.slot, old.x + alpha*(raw.x - old.x), old.y + alpha*(raw.y - old.y),
            old.z + alpha*(raw.z - old.z), now, raw.pen, ax, ay, az)
    }
}
