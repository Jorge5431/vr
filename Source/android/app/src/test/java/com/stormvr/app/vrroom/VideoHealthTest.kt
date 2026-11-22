package com.stormvr.app.vrroom
import com.stormvr.app.moonlight.VideoHealth
import org.junit.Assert.*
import org.junit.Test
class VideoHealthTest {
    @Test fun waitsForActualFramesAndDoesNotFloodKeyframes() {
        val h=VideoHealth(0)
        assertEquals(0,h.update(1000,0))
        assertEquals(VideoHealth.REQUEST_KEYFRAME,h.update(2000,0))
        assertEquals(0,h.update(2500,0))
        assertEquals(VideoHealth.VIDEO_READY,h.update(3000,1))
        for(i in 1..20) assertEquals(0,h.update(3000+i*40L,1+i.toLong()))
    }
    @Test fun reportsStallAndClearsWarningOnRecovery() {
        val h=VideoHealth(100)
        assertTrue(h.update(8200,0) and VideoHealth.NO_VIDEO != 0)
        assertEquals(0,h.update(8400,0))
        assertEquals(VideoHealth.VIDEO_READY,h.update(8500,1))
    }
}
