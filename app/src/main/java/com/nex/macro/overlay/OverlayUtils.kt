package com.nex.macro.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.content.res.ColorStateList
import android.view.Gravity
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
import com.nex.macro.ui.UiKit

object NexColors {
    // Latar putih ~90% opak (agak transparan), garis biru lembut, ikon biru tua
    val SURFACE: Int = Color.parseColor("#E6FFFFFF")
    val PRIMARY: Int = Color.parseColor("#F23B82F6")
    val SECONDARY: Int = Color.parseColor("#2563EB")
    val SUCCESS: Int = Color.parseColor("#22C55E")
    val SUCCESS_BG: Int = Color.parseColor("#E6DCFCE7")
    val ICON: Int = Color.parseColor("#1E3A8A")
    val MUTED: Int = Color.parseColor("#7C8DB0")
}

object OverlayUtils {

    /** Parameter window overlay (TYPE_ACCESSIBILITY_OVERLAY: tidak butuh izin "tampil di atas aplikasi lain"). */
    fun params(w: Int, h: Int, touchable: Boolean = true): WindowManager.LayoutParams {
        var flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        if (!touchable) flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        return WindowManager.LayoutParams(
            w, h,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            flags,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }
    }

    fun setTouchable(lp: WindowManager.LayoutParams, touchable: Boolean) {
        lp.flags = if (touchable) {
            lp.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        } else {
            lp.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }
    }

    fun screen(wm: WindowManager): Rect = wm.currentWindowMetrics.bounds

    fun roundBg(fill: Int, stroke: Int, radiusDp: Int, ctx: Context): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = UiKit.dp(ctx, radiusDp).toFloat()
            setColor(fill)
            setStroke(UiKit.dp(ctx, 2), stroke)
        }

    fun iconButton(ctx: Context, iconRes: Int, sizeDp: Int): ImageButton = ImageButton(ctx).apply {
        setImageResource(iconRes)
        scaleType = ImageView.ScaleType.CENTER_INSIDE
        imageTintList = ColorStateList.valueOf(NexColors.ICON)
        val pad = UiKit.dp(ctx, 12)
        setPadding(pad, pad, pad, pad)
        background = RippleDrawable(
            ColorStateList.valueOf(Color.parseColor("#333B82F6")),
            null,
            GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.WHITE) },
        )
        layoutParams = android.widget.LinearLayout.LayoutParams(UiKit.dp(ctx, sizeDp), UiKit.dp(ctx, sizeDp))
    }
}
