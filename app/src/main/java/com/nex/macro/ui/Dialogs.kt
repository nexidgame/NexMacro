package com.nex.macro.ui

import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Context
import android.text.InputType
import android.text.format.DateFormat
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.radiobutton.MaterialRadioButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.nex.macro.R
import com.nex.macro.core.StopRule
import java.util.Locale

/**
 * Dialog yang bisa ditampilkan dari Activity biasa maupun dari overlay AccessibilityService
 * (overlay = true -> window bertipe TYPE_ACCESSIBILITY_OVERLAY, tanpa izin tambahan).
 */
object Dialogs {

    fun show(d: Dialog, overlay: Boolean) {
        if (overlay) d.window?.setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY)
        d.window?.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE or
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )
        d.show()
    }

    private fun inputBox(ctx: Context, hint: String, value: String, numeric: Boolean): Pair<TextInputLayout, EditText> {
        val til = TextInputLayout(ctx).apply { this.hint = hint }
        val et = TextInputEditText(til.context).apply {
            inputType = if (numeric) InputType.TYPE_CLASS_NUMBER else InputType.TYPE_CLASS_TEXT
            setText(value)
            setSelectAllOnFocus(true)
        }
        til.addView(et)
        return til to et
    }

    private fun wrap(ctx: Context, vararg children: android.view.View): FrameLayout {
        val box = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            val p = UiKit.dp(ctx, 24)
            setPadding(p, UiKit.dp(ctx, 8), p, 0)
        }
        children.forEach { box.addView(it) }
        return FrameLayout(ctx).apply { addView(box) }
    }

    fun number(
        ctx: Context, overlay: Boolean, title: String, hint: String,
        current: Int, min: Int, max: Int, onOk: (Int) -> Unit,
    ) {
        val (til, et) = inputBox(ctx, hint, current.toString(), true)
        val d = MaterialAlertDialogBuilder(ctx)
            .setTitle(title)
            .setView(wrap(ctx, til))
            .setPositiveButton(R.string.ok) { _, _ ->
                onOk((et.text.toString().toIntOrNull() ?: min).coerceIn(min, max))
            }
            .setNegativeButton(R.string.cancel, null)
            .create()
        show(d, overlay)
    }

    /** Dialog "Stop After" dengan 4 pemicu (radio). Perubahan langsung dilaporkan lewat [onChange]. */
    fun stopRule(ctx: Context, overlay: Boolean, initial: StopRule, onChange: (StopRule) -> Unit) {
        val rule = initial.copy()
        val box = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(UiKit.dp(ctx, 8), 0, UiKit.dp(ctx, 8), 0)
        }
        box.addView(TextView(ctx).apply {
            text = ctx.getString(R.string.stop_desc)
            setTextColor(ContextCompat.getColor(ctx, R.color.on_surface_variant))
            textSize = 14f
            setPadding(UiKit.dp(ctx, 16), 0, UiKit.dp(ctx, 16), UiKit.dp(ctx, 8))
        })

        val radios = mutableMapOf<Int, MaterialRadioButton>()
        val subs = mutableMapOf<Int, TextView>()

        fun valueText(mode: Int): String = when (mode) {
            StopRule.DURATION -> ctx.getString(R.string.val_minutes, rule.minutes)
            StopRule.AT_TIME -> String.format(Locale.US, "%02d:%02d", rule.hour, rule.minute)
            StopRule.RUN_COUNT -> ctx.getString(R.string.val_runs, rule.runs)
            else -> ctx.getString(R.string.val_infinite)
        }

        fun refresh() {
            radios.forEach { (m, r) -> r.isChecked = rule.mode == m }
            subs.forEach { (m, t) -> t.text = valueText(m) }
        }

        fun edit(mode: Int) {
            when (mode) {
                StopRule.DURATION -> number(
                    ctx, overlay, ctx.getString(R.string.stop_duration), ctx.getString(R.string.hint_minutes),
                    rule.minutes, 1, 10_080,
                ) { rule.minutes = it; refresh(); onChange(rule.copy()) }

                StopRule.RUN_COUNT -> number(
                    ctx, overlay, ctx.getString(R.string.stop_runs), ctx.getString(R.string.hint_runs),
                    rule.runs, 1, 10_000_000,
                ) { rule.runs = it; refresh(); onChange(rule.copy()) }

                StopRule.AT_TIME -> {
                    val d = TimePickerDialog(
                        ctx,
                        { _, h, m -> rule.hour = h; rule.minute = m; refresh(); onChange(rule.copy()) },
                        rule.hour, rule.minute, DateFormat.is24HourFormat(ctx),
                    )
                    show(d, overlay)
                }
            }
        }

        val options = listOf(
            StopRule.INDEFINITE to R.string.stop_indefinite,
            StopRule.DURATION to R.string.stop_duration,
            StopRule.AT_TIME to R.string.stop_at_time,
            StopRule.RUN_COUNT to R.string.stop_runs,
        )
        for ((mode, titleRes) in options) {
            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(UiKit.dp(ctx, 16), UiKit.dp(ctx, 10), UiKit.dp(ctx, 16), UiKit.dp(ctx, 10))
                val tv = TypedValue()
                ctx.theme.resolveAttribute(android.R.attr.selectableItemBackground, tv, true)
                setBackgroundResource(tv.resourceId)
                isClickable = true
                isFocusable = true
            }
            val col = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            col.addView(TextView(ctx).apply {
                text = ctx.getString(titleRes)
                setTextColor(ContextCompat.getColor(ctx, R.color.on_surface))
                textSize = 16f
            })
            val sub = TextView(ctx).apply {
                setTextColor(ContextCompat.getColor(ctx, R.color.on_surface_variant))
                textSize = 13f
            }
            col.addView(sub)
            val radio = MaterialRadioButton(ctx).apply {
                isClickable = false
                isFocusable = false
            }
            radios[mode] = radio
            subs[mode] = sub
            row.addView(col)
            row.addView(radio)
            row.setOnClickListener {
                rule.mode = mode
                refresh()
                onChange(rule.copy())
                edit(mode)
            }
            box.addView(row)
        }
        refresh()

        val d = MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.s_stop_after)
            .setView(ScrollView(ctx).apply { addView(box) })
            .setPositiveButton(R.string.close, null)
            .create()
        show(d, overlay)
    }

    fun text(
        ctx: Context, overlay: Boolean, title: String,
        initial: String = "", positive: Int = R.string.save,
        onOk: (String) -> Unit,
    ) {
        val (til, et) = inputBox(ctx, "", initial, false)
        val d = MaterialAlertDialogBuilder(ctx)
            .setTitle(title)
            .setView(wrap(ctx, til))
            .setPositiveButton(positive) { _, _ ->
                val v = et.text.toString().trim()
                if (v.isNotEmpty()) onOk(v)
            }
            .setNegativeButton(R.string.close, null)
            .create()
        show(d, overlay)
    }

    fun confirm(ctx: Context, overlay: Boolean, message: String, onYes: () -> Unit) {
        val d = MaterialAlertDialogBuilder(ctx)
            .setMessage(message)
            .setPositiveButton(R.string.yes) { _, _ -> onYes() }
            .setNegativeButton(R.string.cancel, null)
            .create()
        show(d, overlay)
    }

    fun items(ctx: Context, overlay: Boolean, title: String, items: List<String>, onPick: (Int) -> Unit) {
        val d = MaterialAlertDialogBuilder(ctx)
            .setTitle(title)
            .setItems(items.toTypedArray()) { _, which -> onPick(which) }
            .setNegativeButton(R.string.close, null)
            .create()
        show(d, overlay)
    }
}
