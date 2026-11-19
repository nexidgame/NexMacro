package com.nex.macro.ui

import android.app.Dialog
import android.content.Context
import android.text.InputType
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.nex.macro.R

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

    fun stopAfter(ctx: Context, overlay: Boolean, count: Int, seconds: Int, onOk: (Int, Int) -> Unit) {
        val (til1, et1) = inputBox(ctx, ctx.getString(R.string.hint_stop_count), count.toString(), true)
        val (til2, et2) = inputBox(ctx, ctx.getString(R.string.hint_stop_seconds), seconds.toString(), true)
        val note = TextView(ctx).apply {
            text = ctx.getString(R.string.zero_infinite)
            setTextColor(ContextCompat.getColor(ctx, R.color.on_surface_variant))
            textSize = 12f
            setPadding(0, UiKit.dp(ctx, 8), 0, 0)
        }
        val d = MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.s_stop_after)
            .setView(wrap(ctx, til1, til2, note))
            .setPositiveButton(R.string.ok) { _, _ ->
                onOk(
                    (et1.text.toString().toIntOrNull() ?: 0).coerceIn(0, 10_000_000),
                    (et2.text.toString().toIntOrNull() ?: 0).coerceIn(0, 86_400),
                )
            }
            .setNegativeButton(R.string.cancel, null)
            .create()
        show(d, overlay)
    }

    fun text(ctx: Context, overlay: Boolean, title: String, onOk: (String) -> Unit) {
        val (til, et) = inputBox(ctx, "", "", false)
        val d = MaterialAlertDialogBuilder(ctx)
            .setTitle(title)
            .setView(wrap(ctx, til))
            .setPositiveButton(R.string.save) { _, _ ->
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
