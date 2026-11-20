package com.nex.macro.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** Layer layar penuh (tidak bisa disentuh) yang menggambar garis + panah untuk target swipe. */
class LineView(context: Context) : View(context) {

    /** Setiap elemen: [x1, y1, x2, y2] (koordinat layar). */
    private var segments: List<FloatArray> = emptyList()
    private val d = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CC2563EB")
        style = Paint.Style.STROKE
        strokeWidth = 3f * d
        strokeCap = Paint.Cap.ROUND
    }

    fun setSegments(list: List<FloatArray>) {
        segments = list
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        for (s in segments) {
            canvas.drawLine(s[0], s[1], s[2], s[3], paint)
            val ang = atan2((s[3] - s[1]).toDouble(), (s[2] - s[0]).toDouble())
            val len = 14f * d
            for (side in doubleArrayOf(2.6, -2.6)) {
                val a = ang + side
                canvas.drawLine(
                    s[2], s[3],
                    s[2] + (len * cos(a)).toFloat(), s[3] + (len * sin(a)).toFloat(),
                    paint,
                )
            }
        }
    }
}
