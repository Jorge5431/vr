package com.stormvr.app.vrroom

import org.junit.Assert.*
import org.junit.Test

class HandControllerAimTest {
    @Test fun fingerBendsDoNotSteerController() {
        val hand = FloatArray(63)
        for (joint in intArrayOf(5, 9, 13)) hand[joint * 3 + 1] = .08f
        val out = FloatArray(3)
        assertTrue(HandControllerAim.direction(hand, out))
        hand[8 * 3] = .3f
        hand[12 * 3 + 2] = -.2f
        hand[16 * 3 + 1] = -.1f
        assertTrue(HandControllerAim.direction(hand, out))
        assertEquals(0f, out[0], 1e-6f)
        assertEquals(1f, out[1], 1e-6f)
        assertEquals(0f, out[2], 1e-6f)
    }

    @Test fun rejectsDegenerateAndNonFinitePalm() {
        val out = FloatArray(3)
        assertFalse(HandControllerAim.direction(FloatArray(63), out))
        val hand = FloatArray(63)
        hand[15] = Float.NaN
        assertFalse(HandControllerAim.direction(hand, out))
    }
}
