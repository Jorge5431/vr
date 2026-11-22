package com.stormvr.app.vrroom

import org.junit.Assert.*
import org.junit.Test

class ObjectAppearanceTest {
    private val w = 96
    private val h = 80
    private val hand = ObjectHand(1, 40f, 40f, 24f, .45f)
    private fun photo(x: Int, y: Int, rotated: Boolean = false): CameraPhoto {
        val pixels = IntArray(w*h) { 0xff222222.toInt() }
        for (dy in -4..4) for (dx in -4..4) {
            val u = if (rotated) dy else dx
            val v = if (rotated) -dx else dy
            pixels[(y+dy)*w + x+dx] = when {
                u < 0 && v < 0 -> 0xffcc1122.toInt()
                u < 0 -> 0xffee9911.toInt()
                v < 0 -> 0xff2222dd.toInt()
                else -> 0xff117733.toInt()
            }
        }
        return CameraPhoto(pixels, w, h, listOf(hand))
    }
    @Test fun registersAndFindsRotatedMovedDetail() {
        val model = ObjectAppearance.register(photo(46, 38), 46, 38)!!
        val restored = ObjectAppearance.decode(model.encode())!!
        val other = photo(54, 35, true)
        val hit = restored.find(other.rgb,w,h,other.hands)!!
        assertEquals(54f, hit.x, 1.1f)
        assertEquals(35f, hit.y, 1.1f)
        assertEquals(1, hit.hand.slot)
    }
    @Test fun rejectsFlatObjectAndNoHand() {
        val frame = photo(50, 38)
        assertNull(ObjectAppearance.register(frame, 20, 20))
        val pattern = ObjectAppearance.register(frame, 50, 38)!!
        assertNull(pattern.find(frame.rgb,w,h,emptyList()))
        assertNull(pattern.find(IntArray(w*h),w,h,frame.hands))
        assertNull(ObjectAppearance.decode("0,1"))
    }
    @Test fun selectionRequiresTwoFramesAndExpiresWhenLost() {
        val frame = photo(48,40)
        val controller = ObjectController()
        controller.enabled = true
        controller.pattern = ObjectAppearance.register(frame,48,40)
        val pose = FloatArray(16) { if(it % 5 == 0) 1f else 0f }
        fun tick(t: Long) = controller.update(frame.rgb,w,h,frame.hands,100f,100f,48f,40f,pose,t)
        tick(1_000_000L)
        assertNull(controller.live(1_000_000L))
        tick(21_000_000L)
        assertEquals(-.45f, controller.live(21_000_000L)!!.z, .001f)
        assertNull(controller.live(250_000_000L))
    }
}
