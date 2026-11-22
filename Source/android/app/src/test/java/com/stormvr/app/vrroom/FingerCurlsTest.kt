package com.stormvr.app.vrroom

import org.junit.Assert.*
import org.junit.Test

class FingerCurlsTest {
    private fun pose(): FloatArray = FloatArray(63).also { p ->
        for (base in intArrayOf(1,5,9,13,17)) for (j in 0..3) {
            p[(base+j)*3]=base*.005f
            p[(base+j)*3+1]=j*.02f
        }
    }
    @Test fun straightAndCurledIndexRemainIndependent() {
        val p=pose()
        val straight=FingerCurls.pack(p)
        assertEquals(0xA,straight ushr 20)
        assertEquals(0,(straight ushr 4) and 15)
        p[7*3]=p[6*3]+.02f; p[7*3+1]=p[6*3+1]
        p[8*3]=p[5*3]+.005f; p[8*3+1]=p[5*3+1]+.005f
        val curled=FingerCurls.pack(p)
        assertTrue(((curled ushr 4) and 15)>((straight ushr 4) and 15))
        assertEquals(straight and 15,curled and 15)
        assertEquals((straight ushr 8) and 15,(curled ushr 8) and 15)
    }
}
