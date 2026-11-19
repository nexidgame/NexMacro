package com.nex.macro.ui

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nex.macro.R
import com.nex.macro.core.ConfigStore
import com.nex.macro.core.LoadedConfig
import com.nex.macro.core.MacroTarget
import com.nex.macro.core.Prefs

/** Dialog "Pengaturan" overlay multi target: simpan / muat / perbarui konfigurasi + Stop After. */
object ConfigDialogs {

    fun showSettings(
        ctx: Context,
        overlay: Boolean,
        getTargets: () -> List<MacroTarget>,
        onLoaded: (LoadedConfig) -> Unit,
    ) {
        val box = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(UiKit.dp(ctx, 16), UiKit.dp(ctx, 4), UiKit.dp(ctx, 16), 0)
        }

        val title = TextView(ctx).apply {
            text = ctx.getString(R.string.cfg_current)
            setTextColor(ContextCompat.getColor(ctx, R.color.on_surface))
            textSize = 16f
        }
        val nameTv = TextView(ctx).apply {
            setTextColor(ContextCompat.getColor(ctx, R.color.on_surface_variant))
            textSize = 13f
        }
        val col = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            addView(title); addView(nameTv)
        }
        val btnLoad = MaterialButton(ctx).apply { setText(R.string.load) }
        val top = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(col); addView(btnLoad)
        }

        val btnSave = MaterialButton(ctx).apply {
            setText(R.string.save)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                .also { it.marginEnd = UiKit.dp(ctx, 8) }
        }
        val btnUpdate = MaterialButton(ctx).apply {
            setText(R.string.update)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val actions = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, UiKit.dp(ctx, 8), 0, 0)
            addView(btnSave); addView(btnUpdate)
        }

        val stopRow = UiKit.row(
            ctx, ctx.getString(R.string.s_stop_after),
            UiKit.stopText(ctx, Prefs.multiStopCount, Prefs.multiStopSeconds)
        ) {}
        stopRow.view.setOnClickListener {
            Dialogs.stopAfter(ctx, overlay, Prefs.multiStopCount, Prefs.multiStopSeconds) { c, s ->
                Prefs.multiStopCount = c
                Prefs.multiStopSeconds = s
                stopRow.setValue(UiKit.stopText(ctx, c, s))
            }
        }

        box.addView(top)
        box.addView(actions)
        box.addView(UiKit.header(ctx, ctx.getString(R.string.sec_advanced)))
        box.addView(stopRow.view)

        fun refresh() {
            val cur = Prefs.currentConfig
            val has = cur.isNotEmpty() && ConfigStore.exists(ctx, cur)
            nameTv.text = if (has) cur else ctx.getString(R.string.cfg_none)
            btnUpdate.visibility = if (has) View.VISIBLE else View.GONE
            stopRow.setValue(UiKit.stopText(ctx, Prefs.multiStopCount, Prefs.multiStopSeconds))
        }
        refresh()

        btnLoad.setOnClickListener {
            pickAndLoad(ctx, overlay, onLoaded) { refresh() }
        }
        btnSave.setOnClickListener {
            Dialogs.text(ctx, overlay, ctx.getString(R.string.cfg_name_prompt)) { name ->
                ConfigStore.save(ctx, name, getTargets(), Prefs.multiStopCount, Prefs.multiStopSeconds)
                Prefs.currentConfig = name
                Toast.makeText(ctx, R.string.cfg_saved, Toast.LENGTH_SHORT).show()
                refresh()
            }
        }
        btnUpdate.setOnClickListener {
            val name = Prefs.currentConfig
            if (name.isNotEmpty()) {
                ConfigStore.save(ctx, name, getTargets(), Prefs.multiStopCount, Prefs.multiStopSeconds)
                Toast.makeText(ctx, R.string.cfg_updated, Toast.LENGTH_SHORT).show()
            }
        }

        val d = MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.cfg_title)
            .setView(box)
            .setPositiveButton(R.string.close, null)
            .create()
        Dialogs.show(d, overlay)
    }

    private fun pickAndLoad(ctx: Context, overlay: Boolean, onLoaded: (LoadedConfig) -> Unit, after: () -> Unit) {
        val names = ConfigStore.names(ctx)
        if (names.isEmpty()) {
            Toast.makeText(ctx, R.string.cfg_empty, Toast.LENGTH_SHORT).show()
            return
        }
        Dialogs.items(ctx, overlay, ctx.getString(R.string.load), names) { i ->
            val name = names[i]
            val d = MaterialAlertDialogBuilder(ctx)
                .setTitle(name)
                .setMessage(ctx.getString(R.string.cfg_load_confirm, name))
                .setPositiveButton(R.string.load) { _, _ ->
                    ConfigStore.load(ctx, name)?.let { cfg ->
                        Prefs.currentConfig = name
                        Prefs.multiStopCount = cfg.stopCount
                        Prefs.multiStopSeconds = cfg.stopSeconds
                        onLoaded(cfg)
                        Toast.makeText(ctx, R.string.cfg_loaded, Toast.LENGTH_SHORT).show()
                        after()
                    }
                }
                .setNegativeButton(R.string.delete) { _, _ ->
                    ConfigStore.delete(ctx, name)
                    if (Prefs.currentConfig == name) Prefs.currentConfig = ""
                    Toast.makeText(ctx, R.string.cfg_deleted, Toast.LENGTH_SHORT).show()
                    after()
                }
                .setNeutralButton(R.string.cancel, null)
                .create()
            Dialogs.show(d, overlay)
        }
    }

    /** Dari layar pengaturan: lihat & hapus konfigurasi tersimpan. */
    fun showManage(ctx: Context) {
        val names = ConfigStore.names(ctx)
        if (names.isEmpty()) {
            Toast.makeText(ctx, R.string.cfg_empty, Toast.LENGTH_SHORT).show()
            return
        }
        Dialogs.items(ctx, false, ctx.getString(R.string.cfg_manage_title), names) { i ->
            val name = names[i]
            Dialogs.confirm(ctx, false, ctx.getString(R.string.cfg_delete_q, name)) {
                ConfigStore.delete(ctx, name)
                if (Prefs.currentConfig == name) Prefs.currentConfig = ""
                Toast.makeText(ctx, R.string.cfg_deleted, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
