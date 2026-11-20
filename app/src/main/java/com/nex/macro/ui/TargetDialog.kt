package com.nex.macro.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nex.macro.R
import com.nex.macro.core.MacroTarget
import com.nex.macro.core.TargetType

/** Dialog "Edit Target" (hapus, aktif/nonaktif, jeda, durasi, ulangi, nonaktifkan setelah, radius acak). */
object TargetDialog {

    interface Callbacks {
        fun onRemove()
        fun onChanged()
    }

    fun show(
        ctx: Context, overlay: Boolean, number: Int, t: MacroTarget, programmable: Boolean, cb: Callbacks,
        showCoordinates: Boolean = false,
    ) {
        val swipe = t.type == TargetType.SWIPE
        val content = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(UiKit.dp(ctx, 8), UiKit.dp(ctx, 4), UiKit.dp(ctx, 8), 0)
        }

        // Tombol Hapus + Aktif
        val btnRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(UiKit.dp(ctx, 12), 0, UiKit.dp(ctx, 12), UiKit.dp(ctx, 4))
        }
        val btnRemove = MaterialButton(ctx).apply {
            setText(R.string.btn_remove)
            backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FEE2E2"))
            setTextColor(Color.parseColor("#B91C1C"))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                .also { it.marginEnd = UiKit.dp(ctx, 8) }
        }
        val btnEnabled = MaterialButton(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        fun renderEnabled() {
            btnEnabled.setText(if (t.enabled) R.string.btn_enabled else R.string.btn_disabled)
            btnEnabled.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(ctx, if (t.enabled) R.color.primary else R.color.outline)
            )
            btnEnabled.setTextColor(
                if (t.enabled) Color.WHITE else ContextCompat.getColor(ctx, R.color.on_surface_variant)
            )
        }
        renderEnabled()
        btnRow.addView(btnRemove)
        btnRow.addView(btnEnabled)
        content.addView(btnRow)

        lateinit var dialog: androidx.appcompat.app.AlertDialog

        btnEnabled.setOnClickListener {
            t.enabled = !t.enabled
            t.autoDisabled = false
            renderEnabled()
            cb.onChanged()
        }
        btnRemove.setOnClickListener {
            Dialogs.confirm(ctx, overlay, ctx.getString(R.string.delete_target_q)) {
                dialog.dismiss()
                cb.onRemove()
            }
        }

        if (showCoordinates) {
            content.addView(UiKit.header(ctx, ctx.getString(R.string.sec_position)))
            fun coord(title: Int, get: () -> Int, set: (Int) -> Unit) {
                val row = UiKit.row(ctx, ctx.getString(title), UiKit.px(ctx, get())) {}
                row.view.setOnClickListener {
                    Dialogs.number(ctx, overlay, ctx.getString(title), ctx.getString(R.string.hint_px), get(), 0, 10_000) { v ->
                        set(v); row.setValue(UiKit.px(ctx, v)); cb.onChanged()
                    }
                }
                content.addView(row.view)
            }
            coord(R.string.s_pos_x, { t.x }, { t.x = it })
            coord(R.string.s_pos_y, { t.y }, { t.y = it })
            if (swipe) {
                coord(R.string.s_pos_x2, { t.x2 }, { t.x2 = it })
                coord(R.string.s_pos_y2, { t.y2 }, { t.y2 = it })
            }
        }

        if (programmable) {
            content.addView(UiKit.header(ctx, ctx.getString(R.string.sec_basic)))
            val rStart = UiKit.row(ctx, ctx.getString(R.string.s_start_delay), UiKit.ms(ctx, t.startDelay)) {}
            val rEnd = UiKit.row(ctx, ctx.getString(R.string.s_end_delay), UiKit.ms(ctx, t.endDelay)) {}
            val rDur = UiKit.row(ctx, ctx.getString(R.string.s_swipe_duration), UiKit.ms(ctx, t.duration)) {}
            val rRepeat = UiKit.row(ctx, ctx.getString(R.string.s_repeat), repeatText(ctx, t.repeat)) {}
            val rDisable = UiKit.row(ctx, ctx.getString(R.string.s_disable_after), disableText(ctx, t.disableAfter)) {}
            val rRadius = UiKit.row(ctx, ctx.getString(R.string.s_radius), UiKit.px(ctx, t.radius)) {}

            fun bind(row: UiKit.SettingRow, title: Int, hint: Int, cur: () -> Int, max: Int,
                     set: (Int) -> Unit, text: (Int) -> String) {
                row.view.setOnClickListener {
                    Dialogs.number(ctx, overlay, ctx.getString(title), ctx.getString(hint), cur(), 0, max) { v ->
                        set(v); row.setValue(text(v)); cb.onChanged()
                    }
                }
            }
            bind(rStart, R.string.s_start_delay, R.string.hint_ms, { t.startDelay }, 3_600_000,
                { t.startDelay = it }) { UiKit.ms(ctx, it) }
            bind(rEnd, R.string.s_end_delay, R.string.hint_ms, { t.endDelay }, 3_600_000,
                { t.endDelay = it }) { UiKit.ms(ctx, it) }
            bind(rDur, R.string.s_swipe_duration, R.string.hint_ms, { t.duration }, 60_000,
                { t.duration = it }) { UiKit.ms(ctx, it) }
            bind(rRepeat, R.string.s_repeat, R.string.hint_repeat, { t.repeat }, 100_000,
                { t.repeat = it }) { repeatText(ctx, it) }
            bind(rDisable, R.string.s_disable_after, R.string.hint_disable_after, { t.disableAfter }, 10_000_000,
                { t.disableAfter = it }) { disableText(ctx, it) }
            bind(rRadius, R.string.s_radius, R.string.hint_px, { t.radius }, 500,
                { t.radius = it }) { UiKit.px(ctx, it) }

            content.addView(rStart.view)
            content.addView(rEnd.view)
            content.addView(rDur.view)
            content.addView(UiKit.header(ctx, ctx.getString(R.string.sec_advanced)))
            content.addView(rRepeat.view)
            content.addView(rDisable.view)
            content.addView(UiKit.header(ctx, ctx.getString(R.string.sec_randomization)))
            content.addView(rRadius.view)
        } else {
            content.addView(TextView(ctx).apply {
                text = ctx.getString(R.string.programmability_off_note)
                setTextColor(ContextCompat.getColor(ctx, R.color.on_surface_variant))
                textSize = 13f
                setPadding(UiKit.dp(ctx, 16), UiKit.dp(ctx, 12), UiKit.dp(ctx, 16), UiKit.dp(ctx, 8))
            })
        }

        val scroll = ScrollView(ctx).apply { addView(content) }
        dialog = MaterialAlertDialogBuilder(ctx)
            .setTitle(ctx.getString(if (swipe) R.string.edit_swipe_fmt else R.string.edit_target_fmt, number))
            .setView(scroll)
            .setPositiveButton(R.string.close, null)
            .create()
        Dialogs.show(dialog, overlay)
    }

    private fun repeatText(ctx: Context, v: Int) =
        if (v <= 0) ctx.getString(R.string.val_no_repeat) else ctx.getString(R.string.val_repeat_fmt, v)

    private fun disableText(ctx: Context, v: Int) =
        if (v <= 0) ctx.getString(R.string.val_no_disable) else ctx.getString(R.string.val_disable_after_fmt, v)
}
