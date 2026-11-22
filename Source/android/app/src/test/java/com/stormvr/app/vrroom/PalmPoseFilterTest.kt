package com.stormvr.app.vrroom
import org.junit.Assert.*
import org.junit.Test

class PalmPoseFilterTest {
    private fun pose(x: Float)=FloatArray(63) { i -> when(i%3) {0 -> x; 1 -> (i/3)*.006f; else -> -.4f} }
    @Test fun rigidTranslationDoesNotStretchTheHand() {
        val filter=PalmPoseFilter()
        repeat(80) { i ->
            val p=pose(i*.002f); filter.apply(p,i*.033f,1)
            assertEquals(.12f,p[61]-p[1],.00001f)
            for(j in 0..20) assertEquals(p[0],p[j*3],.00001f)
        }
    }
    @Test fun reducesStationaryJitterAndRecoversAfterGap() {
        val filter=PalmPoseFilter();var raw=0f;var smooth=0f
        repeat(100) { i ->
            val x=if(i%2==0) .008f else -.008f
            val p=pose(x);filter.apply(p,i/30f,0)
            if(i>20) { raw+=x*x;smooth+=p[0]*p[0] }
        }
        assertTrue("filtered jitter must be lower",smooth < raw*.65f)
        val p=pose(.5f);filter.apply(p,8f,0)
        assertEquals(.5f,p[0],.00001f)
    }
}
