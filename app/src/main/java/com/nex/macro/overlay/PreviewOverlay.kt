package com.nex.macro.overlay

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.View
import android.view.WindowManager
import com.nex.macro.R
import com.nex.macro.core.MacroTarget
import com.nex.macro.core.TargetType
import com.nex.macro.service.ClickerService
import com.nex.macro.ui.UiKit

/** Menampilkan target (nomor + posisi) di layar tanpa bisa disentuh — dipakai tombol "mata" di editor konfigurasi. */
class PreviewOverlay(private val svc: ClickerService) {

    private val ctx: Context = ContextThemeWrapper(svc, R.style.Theme_NexMacro)
    private val wm: WindowManager = svc.getSystemService(WindowManager::class.java)
    private val views = mutableListOf<View>()

    fun show(items: List<Pair<Int, MacroTarget>>) {
        hide()
        val size = UiKit.dp(ctx, 56)

        val line = LineView(ctx)
        val lineLp = OverlayUtils.params(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT, touchable = false,
        ).also { it.x = 0; it.y = 0 }
        line.setSegments(
            items.filter { it.second.type == TargetType.SWIPE }.map { (_, t) ->
                floatArrayOf(t.x.toFloat(), t.y.toFloat(), t.x2.toFloat(), t.y2.toFloat())
            }
        )
        wm.addView(line, lineLp)
        views += line

        fun add(number: Int, cx: Int, cy: Int, enabled: Boolean, isEnd: Boolean, swipe: Boolean) {
            val v = TargetView(ctx).apply {
                label = number.toString()
                targetEnabled = enabled
                this.isEnd = isEnd
                sub = if (swipe) ctx.getString(if (isEnd) R.string.label_end else R.string.label_start) else null
            }
            val lp = OverlayUtils.params(size, size, touchable = false)
            lp.x = cx - size / 2
            lp.y = cy - size / 2
            wm.addView(v, lp)
            views += v
        }

        for ((n, t) in items) {
            val swipe = t.type == TargetType.SWIPE
            add(n, t.x, t.y, t.enabled, false, swipe)
            if (swipe) add(n, t.x2, t.y2, t.enabled, true, true)
        }
    }

    fun hide() {
        for (v in views) {
            try {
                wm.removeViewImmediate(v)
            } catch (_: Exception) {
            }
        }
        views.clear()
    }
}
