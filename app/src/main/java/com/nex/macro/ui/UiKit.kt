package com.nex.macro.ui

import android.content.Context
import android.util.TypedValue
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.materialswitch.MaterialSwitch
import com.nex.macro.R

/** Pembantu UI programatik agar tampilan baris pengaturan konsisten (activity & dialog overlay). */
object UiKit {

    fun dp(ctx: Context, v: Int): Int = (v * ctx.resources.displayMetrics.density + 0.5f).toInt()

    private fun c(ctx: Context, res: Int) = ContextCompat.getColor(ctx, res)

    class SettingRow(val view: View, private val valueView: TextView) {
        fun setValue(text: String) {
            valueView.text = text
        }
    }

    fun header(ctx: Context, text: String): TextView = TextView(ctx).apply {
        this.text = text
        setTextColor(c(ctx, R.color.secondary))
        textSize = 13f
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        setPadding(dp(ctx, 16), dp(ctx, 20), dp(ctx, 16), dp(ctx, 4))
    }

    fun row(ctx: Context, title: String, value: String, onClick: () -> Unit): SettingRow {
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(ctx, 16), dp(ctx, 12), dp(ctx, 16), dp(ctx, 12))
            val tv = TypedValue()
            ctx.theme.resolveAttribute(android.R.attr.selectableItemBackground, tv, true)
            setBackgroundResource(tv.resourceId)
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
        }
        val t = TextView(ctx).apply {
            text = title
            setTextColor(c(ctx, R.color.on_surface))
            textSize = 16f
        }
        val v = TextView(ctx).apply {
            text = value
            setTextColor(c(ctx, R.color.on_surface_variant))
            textSize = 13f
        }
        root.addView(t)
        root.addView(v)
        return SettingRow(root, v)
    }

    fun switchRow(ctx: Context, title: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit): View {
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(ctx, 16), dp(ctx, 10), dp(ctx, 16), dp(ctx, 10))
            val tv = TypedValue()
            ctx.theme.resolveAttribute(android.R.attr.selectableItemBackground, tv, true)
            setBackgroundResource(tv.resourceId)
            isClickable = true
            isFocusable = true
        }
        val col = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        col.addView(TextView(ctx).apply {
            text = title
            setTextColor(c(ctx, R.color.on_surface))
            textSize = 16f
        })
        col.addView(TextView(ctx).apply {
            text = desc
            setTextColor(c(ctx, R.color.on_surface_variant))
            textSize = 13f
        })
        val sw = MaterialSwitch(ctx).apply {
            isChecked = checked
            setOnCheckedChangeListener { _, isOn -> onChange(isOn) }
        }
        root.addView(col)
        root.addView(sw)
        root.setOnClickListener { sw.toggle() }
        return root
    }

    fun ms(ctx: Context, v: Int): String = ctx.getString(R.string.val_ms, v)
    fun px(ctx: Context, v: Int): String = ctx.getString(R.string.val_px, v)
    fun onOff(ctx: Context, on: Boolean): String =
        ctx.getString(if (on) R.string.val_enabled else R.string.val_disabled)

    fun stopText(ctx: Context, rule: com.nex.macro.core.StopRule): String = when (rule.mode) {
        com.nex.macro.core.StopRule.DURATION -> ctx.getString(R.string.val_minutes, rule.minutes)
        com.nex.macro.core.StopRule.AT_TIME -> String.format(java.util.Locale.US, "%02d:%02d", rule.hour, rule.minute)
        com.nex.macro.core.StopRule.RUN_COUNT -> ctx.getString(R.string.val_runs, rule.runs)
        else -> ctx.getString(R.string.val_infinite)
    }
}

/** Tambahkan padding sebesar system bar (edge-to-edge, wajib untuk targetSdk 35+). */
fun View.applySystemBarsPadding() {
    val l = paddingLeft
    val t = paddingTop
    val r = paddingRight
    val b = paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val bars = insets.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        )
        v.updatePadding(left = l + bars.left, top = t + bars.top, right = r + bars.right, bottom = b + bars.bottom)
        insets
    }
    ViewCompat.requestApplyInsets(this)
}
