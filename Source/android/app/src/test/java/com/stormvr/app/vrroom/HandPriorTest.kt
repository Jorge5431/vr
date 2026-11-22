package com.stormvr.app.vrroom

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.sqrt

class HandPriorTest {
    private fun open(): FloatArray = FloatArray(63).also { p ->
        for (base in intArrayOf(1, 5, 9, 13, 17)) for (k in 0..3) {
            p[(base+k)*3] = (base-9)*0.003f
            p[(base+k)*3+1] = .06f + k*.025f
        }
    }
    @Test fun collapsedAndReversedBonesRemainFiniteAndKeepLength() {
        val prior = HandPrior()
        repeat(12) { prior.enforce(open(), true) }
        val p=open()
        for (j in 0..2) p[7*3+j]=p[6*3+j]
        p[8*3+1]=p[6*3+1]-.06f
        prior.enforce(p,false)
        assertTrue(p.all { it.isFinite() })
        for (a in 5..7) {
            var n=0f
            for (j in 0..2) { val d=p[(a+1)*3+j]-p[a*3+j]; n+=d*d }
            assertEquals(.025f,sqrt(n),.0001f)
        }
        val a=FloatArray(3){p[7*3+it]-p[6*3+it]}
        val b=FloatArray(3){p[8*3+it]-p[7*3+it]}
        val cosine=(0..2).sumOf { (a[it]*b[it]).toDouble() } / (.025*.025)
        assertTrue("reversed joint must respect 105 degrees",cosine >= -.259)
    }
}
