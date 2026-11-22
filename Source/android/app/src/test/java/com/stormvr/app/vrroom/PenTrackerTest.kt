package com.stormvr.app.vrroom

import org.junit.Assert.*
import org.junit.Test

class PenTrackerTest {
    private val w = 160
    private val h = 100
    private val hand = ObjectHand(1, 75f, 50f, 36f, .45f)
    private fun frame(capOnRight: Boolean = true): IntArray {
        val rgb = IntArray(w*h) { 0xff777777.toInt() }
        for (y in 47..53) for (x in 42..107) rgb[y*w+x] = 0xffefa01c.toInt()
        val cap = if (capOnRight) 108..129 else 20..41
        val tail = if (capOnRight) 35..41 else 108..114
        for (y in 46..54) for (x in cap) rgb[y*w+x] = 0xff194ac7.toInt()
        for (y in 48..52) for (x in tail) rgb[y*w+x] = 0xff194ac7.toInt()
        return rgb
    }

    @Test fun pointsTowardLongBlueCap() {
        val right = PenTracker.find(frame(), w, h, listOf(hand))!!
        assertEquals(1, right.hand.slot)
        assertTrue(right.axisX > .9f)
        assertEquals(129f, right.tipX, 2f)
        val left = PenTracker.find(frame(false), w, h, listOf(hand))!!
        assertTrue(left.axisX < -.9f)
        assertEquals(20f, left.tipX, 2f)
    }

    @Test fun requiresBothBarrelAndCapNearHand() {
        assertNull(PenTracker.find(IntArray(w*h) { 0xffefa01c.toInt() }, w, h, listOf(hand)))
        assertNull(PenTracker.find(frame(), w, h, emptyList()))
        assertNull(PenTracker.find(frame(), w, h, listOf(hand.copy(x = 0f, y = 0f, span = 10f))))
    }

    @Test fun controllerPublishesThreeDimensionalPenDirectionAfterTwoFrames() {
        val controller = ObjectController().apply { enabled = true; penMode = true }
        val pose = FloatArray(16) { if (it % 5 == 0) 1f else 0f }
        val rgb = frame()
        controller.update(rgb, w, h, listOf(hand), 250f, 250f, 80f, 50f, pose, 1_000_000L)
        assertNull(controller.live(1_000_000L))
        controller.update(rgb, w, h, listOf(hand), 250f, 250f, 80f, 50f, pose, 21_000_000L)
        val result = controller.live(21_000_000L)!!
        assertTrue(result.pen)
        assertTrue(result.dirX > 0f)
        assertTrue(result.dirZ < 0f)
    }
}
