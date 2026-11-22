package com.stormvr.app.vrroom

import kotlin.math.sqrt

/** Five quantized finger bends in the unused three bytes of each existing hand packet. */
object FingerCurls {
    private val chains = arrayOf(intArrayOf(1,2,3,4), intArrayOf(5,6,7,8),
        intArrayOf(9,10,11,12), intArrayOf(13,14,15,16), intArrayOf(17,18,19,20))
    private fun distance(p: FloatArray, a: Int, b: Int): Float {
        val i=a*3; val j=b*3
        val x=p[i]-p[j]; val y=p[i+1]-p[j+1]; val z=p[i+2]-p[j+2]
        return sqrt(x*x+y*y+z*z)
    }
    fun pack(p: FloatArray): Int {
        if (p.size < 63) return 0
        var bits=0
        for (finger in 0..4) {
            val c=chains[finger]
            val path=distance(p,c[0],c[1])+distance(p,c[1],c[2])+distance(p,c[2],c[3])
            val straight=if(path>1e-5f) distance(p,c[0],c[3])/path else 1f
            val curl=((.95f-straight)/.47f).coerceIn(0f,1f)
            bits=bits or ((curl*15f+.5f).toInt() shl (finger*4))
        }
        return bits or (0xA shl 20)
    }
}
