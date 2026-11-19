package com.nex.macro.overlay

import android.annotation.SuppressLint
import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import kotlin.math.hypot

/**
 * Menggeser window overlay dengan jari. Tap (tanpa geser melewati slop) memanggil [onTap].
 * [limits] memberi batas posisi kiri-atas window (dalam piksel layar).
 */
class DragTouch(
    private val wm: WindowManager,
    private val view: View,
    private val lp: WindowManager.LayoutParams,
    private val limits: () -> Rect,
    private val canDrag: () -> Boolean = { true },
    private val onMove: () -> Unit = {},
    private val onTap: () -> Unit = {},
    private val onEnd: () -> Unit = {},
) : View.OnTouchListener {

    private var downX = 0f
    private var downY = 0f
    private var startX = 0
    private var startY = 0
    private var moved = false
    private val slop = ViewConfiguration.get(view.context).scaledTouchSlop

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = e.rawX; downY = e.rawY
                startX = lp.x; startY = lp.y
                moved = false
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = e.rawX - downX
                val dy = e.rawY - downY
                if (!moved && hypot(dx, dy) > slop) moved = true
                if (moved && canDrag()) {
                    val b = limits()
                    lp.x = (startX + dx).toInt().coerceIn(b.left, b.right.coerceAtLeast(b.left))
                    lp.y = (startY + dy).toInt().coerceIn(b.top, b.bottom.coerceAtLeast(b.top))
                    try {
                        wm.updateViewLayout(view, lp)
                    } catch (_: IllegalArgumentException) {
                    }
                    onMove()
                }
            }
            MotionEvent.ACTION_UP -> {
                if (!moved) onTap() else onEnd()
            }
            MotionEvent.ACTION_CANCEL -> if (moved) onEnd()
        }
        return true
    }
}
