package com.nex.macro.overlay

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import com.nex.macro.R
import com.nex.macro.core.Prefs
import com.nex.macro.core.Step
import com.nex.macro.service.ClickerService
import com.nex.macro.ui.UiKit

/**
 * Overlay mode target tunggal: bar [sisi | ▶ | ✕ | sisi].
 * - Geser sisi bar  : pindahkan overlay
 * - Tap sisi bar    : ganti sudut titik klik (kiri-atas → kanan-atas → kanan-bawah → kiri-bawah)
 * - ▶ / ⏸          : mulai / jeda (bar berubah hijau saat berjalan)
 * - ✕              : tutup overlay
 * Titik klik berada tepat di luar sudut bar yang bertanda (segitiga), ditandai cincin biru.
 */
class SingleOverlay(private val svc: ClickerService) {

    private val ctx: Context = ContextThemeWrapper(svc, R.style.Theme_NexMacro)
    private val wm: WindowManager = svc.getSystemService(WindowManager::class.java)

    private val barW = UiKit.dp(ctx, 200)
    private val barH = UiKit.dp(ctx, 56)
    private val edge = UiKit.dp(ctx, 14)          // jarak titik klik dari sudut bar
    private val dotSize = UiKit.dp(ctx, 30)

    private lateinit var root: FrameLayout
    private lateinit var lp: WindowManager.LayoutParams
    private lateinit var dot: View
    private lateinit var dotLp: WindowManager.LayoutParams
    private lateinit var bg: GradientDrawable
    private lateinit var dotBg: GradientDrawable
    private lateinit var btnPlay: ImageButton
    private lateinit var marker: CornerMarker

    private var corner = Prefs.singleCorner.coerceIn(0, 3)
    private var running = false
    private var attached = false

    private fun limits(): Rect {
        val s = OverlayUtils.screen(wm)
        return Rect(edge, edge, s.width() - barW - edge, s.height() - barH - edge)
    }

    fun show() {
        val s = OverlayUtils.screen(wm)
        lp = OverlayUtils.params(barW, barH)
        lp.x = if (Prefs.singleX >= 0) Prefs.singleX else (s.width() - barW) / 2
        lp.y = if (Prefs.singleY >= 0) Prefs.singleY else (s.height() * 0.65f).toInt()
        val b = limits()
        lp.x = lp.x.coerceIn(b.left, b.right)
        lp.y = lp.y.coerceIn(b.top, b.bottom)

        buildBar()
        buildDot()

        wm.addView(dot, dotLp)
        wm.addView(root, lp)
        attached = true
        updatePointer()
        render()
    }

    private fun buildBar() {
        bg = OverlayUtils.roundBg(NexColors.SURFACE, NexColors.PRIMARY, 18, ctx)
        root = FrameLayout(ctx).apply { background = bg }

        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = FrameLayout.LayoutParams(barW, barH)
        }

        fun side(): View {
            val v = ImageView(ctx).apply {
                setImageResource(R.drawable.ic_drag)
                imageTintList = ColorStateList.valueOf(NexColors.MUTED)
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                setPadding(UiKit.dp(ctx, 12), UiKit.dp(ctx, 10), UiKit.dp(ctx, 12), UiKit.dp(ctx, 10))
                layoutParams = LinearLayout.LayoutParams(UiKit.dp(ctx, 48), barH)
            }
            v.setOnTouchListener(
                DragTouch(
                    wm = wm, view = root, lp = lp,
                    limits = ::limits,
                    canDrag = { !running },
                    onMove = { updatePointer() },
                    onTap = { if (!running) cycleCorner() },
                    onEnd = { Prefs.singleX = lp.x; Prefs.singleY = lp.y },
                )
            )
            return v
        }

        btnPlay = OverlayUtils.iconButton(ctx, R.drawable.ic_play, 52).apply {
            setOnClickListener { if (running) stop() else start() }
        }
        val btnClose = OverlayUtils.iconButton(ctx, R.drawable.ic_close, 52).apply {
            setOnClickListener { svc.hideOverlays() }
        }

        row.addView(side())
        row.addView(btnPlay)
        row.addView(btnClose)
        row.addView(side())
        marker = CornerMarker(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(barW, barH)
            this.corner = this@SingleOverlay.corner
        }
        root.addView(row)
        root.addView(marker)
    }

    private fun buildDot() {
        dotBg = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor("#3322D3EE"))
            setStroke(UiKit.dp(ctx, 3), NexColors.SECONDARY)
        }
        dot = View(ctx).apply { background = dotBg }
        dotLp = OverlayUtils.params(dotSize, dotSize, touchable = false)
    }

    private fun clickPoint(): Point {
        val x = lp.x
        val y = lp.y
        return when (corner) {
            0 -> Point(x - edge, y - edge)
            1 -> Point(x + barW + edge, y - edge)
            2 -> Point(x + barW + edge, y + barH + edge)
            else -> Point(x - edge, y + barH + edge)
        }
    }

    private fun updatePointer() {
        val p = clickPoint()
        dotLp.x = p.x - dotSize / 2
        dotLp.y = p.y - dotSize / 2
        if (attached) {
            try {
                wm.updateViewLayout(dot, dotLp)
            } catch (_: IllegalArgumentException) {
            }
        }
    }

    private fun cycleCorner() {
        corner = (corner + 1) % 4
        Prefs.singleCorner = corner
        marker.corner = corner
        updatePointer()
    }

    private fun render() {
        btnPlay.setImageResource(if (running) R.drawable.ic_pause else R.drawable.ic_play)
        bg.setColor(if (running) NexColors.SUCCESS_BG else NexColors.SURFACE)
        bg.setStroke(UiKit.dp(ctx, 2), if (running) NexColors.SUCCESS else NexColors.PRIMARY)
        dotBg.setStroke(UiKit.dp(ctx, 3), if (running) NexColors.SUCCESS else NexColors.SECONDARY)
        dotBg.setColor(Color.parseColor(if (running) "#3334D399" else "#3322D3EE"))
        root.invalidate()
    }

    fun startIfIdle() {
        if (!running && attached) start()
    }

    private fun start() {
        if (running) return
        running = true
        render()
        svc.beginRun()
        val engine = svc.engine
        engine.onStepStart = null
        engine.onStepEnd = null
        engine.onStopped = {
            running = false
            if (attached) render()
        }
        engine.start(Prefs.singleStopCount, Prefs.singleStopSeconds) {
            val p = clickPoint()
            Step(
                startDelay = 0,
                endDelay = Prefs.singleInterval.toLong(),
                duration = Prefs.singleDuration.toLong(),
                swipe = false,
                x = p.x.toFloat(),
                y = p.y.toFloat(),
            )
        }
    }

    private fun stop() {
        svc.engine.stop()
    }

    fun hide() {
        attached = false
        for (v in listOf(root, dot)) {
            try {
                wm.removeViewImmediate(v)
            } catch (_: Exception) {
            }
        }
    }

    /** Segitiga kecil di sudut bar yang menandakan arah titik klik. */
    private class CornerMarker(context: Context) : View(context) {
        var corner: Int = 0
            set(value) { field = value; invalidate() }
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#22D3EE")
            style = Paint.Style.FILL
        }

        override fun onDraw(canvas: Canvas) {
            val t = 14f * resources.displayMetrics.density
            val w = width.toFloat()
            val h = height.toFloat()
            val path = Path()
            when (corner) {
                0 -> { path.moveTo(0f, 0f); path.lineTo(t, 0f); path.lineTo(0f, t) }
                1 -> { path.moveTo(w, 0f); path.lineTo(w - t, 0f); path.lineTo(w, t) }
                2 -> { path.moveTo(w, h); path.lineTo(w - t, h); path.lineTo(w, h - t) }
                else -> { path.moveTo(0f, h); path.lineTo(t, h); path.lineTo(0f, h - t) }
            }
            path.close()
            canvas.drawPath(path, paint)
        }
    }
}
