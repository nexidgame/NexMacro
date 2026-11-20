package com.nex.macro.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.view.View
import kotlin.math.min

/** Lingkaran target bernomor dengan crosshair (seperti referensi), mendukung label start/end untuk swipe. */
class TargetView(context: Context) : View(context) {

    var label: String = "1"
        set(value) { field = value; invalidate() }
    var sub: String? = null
        set(value) { field = value; invalidate() }
    var targetEnabled: Boolean = true
        set(value) { field = value; invalidate() }
    var active: Boolean = false
        set(value) { field = value; invalidate() }
    var isEnd: Boolean = false
        set(value) { field = value; invalidate() }

    private val d = resources.displayMetrics.density
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 2.5f * d
    }
    private val cross = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 1.5f * d
    }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD
        setShadowLayer(3f * d, 0f, 0f, Color.BLACK)
    }
    private val subText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD
        setShadowLayer(3f * d, 0f, 0f, Color.BLACK)
    }

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val r = min(width, height) / 2f - stroke.strokeWidth

        val (f, s) = when {
            !targetEnabled -> Color.parseColor("#33888888") to Color.parseColor("#99AAAAAA")
            active -> Color.parseColor("#9922C55E") to Color.parseColor("#FF16A34A")
            isEnd -> Color.parseColor("#5560A5FA") to Color.parseColor("#FF1D4ED8")
            else -> Color.parseColor("#553B82F6") to Color.parseColor("#FF2563EB")
        }
        fill.color = f
        stroke.color = s
        cross.color = s
        canvas.drawCircle(cx, cy, r, fill)
        canvas.drawCircle(cx, cy, r, stroke)
        canvas.drawLine(cx - r, cy, cx + r, cy, cross)
        canvas.drawLine(cx, cy - r, cx, cy + r, cross)

        text.textSize = r * 0.85f
        val baseline = cy - (text.ascent() + text.descent()) / 2f - (if (sub != null) r * 0.12f else 0f)
        canvas.drawText(label, cx, baseline, text)
        sub?.let {
            subText.textSize = r * 0.32f
            canvas.drawText(it, cx, cy + r * 0.62f, subText)
        }
    }
}
