package com.stormvr.app.vrroom

import kotlin.math.*

/** Filter translation separately from finger articulation, in a wrist-relative frame. */
class PalmPoseFilter {
    private val previous = FloatArray(63)
    private val local = FloatArray(63)
    private val wrist = FloatArray(3)
    private var last = Float.NaN
    fun reset() { last = Float.NaN }
    fun apply(points: FloatArray, time: Float, profile: Int) {
        if (points.size != 63 || points.any { !it.isFinite() }) return
        val dt = time-last
        if (!last.isFinite() || dt !in .001f..0.2f) {
            for (i in 0..2) wrist[i]=points[i]
            for (i in points.indices) local[i]=points[i]-points[i%3]
        } else {
            val speed = sqrt((0..2).sumOf { ((points[it]-previous[it]).toDouble()).pow(2) }).toFloat()/dt
            val base = when(profile) { 0->2f; 2->7f; else->3.5f }
            val alpha = 1f-exp(-2f*PI.toFloat()*(base+speed.coerceAtMost(2f)*7f)*dt)
            for (i in 0..2) wrist[i]+=alpha*(points[i]-wrist[i])
            for (i in 3 until 63) {
                val value=points[i]-points[i%3]
                val velocity=abs(value-(previous[i]-previous[i%3]))/dt
                val cutoff=base*.65f+velocity.coerceAtMost(1f)*12f
                val a=1f-exp(-2f*PI.toFloat()*cutoff*dt)
                local[i]+=a*(value-local[i])
            }
        }
        points.copyInto(previous);last=time
        for (i in points.indices) points[i]=wrist[i%3]+local[i]
    }
}
