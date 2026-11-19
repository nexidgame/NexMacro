package com.nex.macro

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.widget.ImageViewCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nex.macro.databinding.ActivityMainBinding
import com.nex.macro.service.ClickerService
import com.nex.macro.ui.applySystemBarsPadding

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.root.applySystemBarsPadding()

        b.tvVersion.text = getString(R.string.version_fmt, BuildConfigVersion.name(this))

        b.btnStartSingle.setOnClickListener { startMode(ClickerService.MODE_SINGLE) }
        b.btnStartMulti.setOnClickListener { startMode(ClickerService.MODE_MULTI) }
        b.btnInfoSingle.setOnClickListener { openInstructions(ClickerService.MODE_SINGLE) }
        b.btnInfoMulti.setOnClickListener { openInstructions(ClickerService.MODE_MULTI) }
        b.btnSettingsSingle.setOnClickListener { openSettings(ClickerService.MODE_SINGLE) }
        b.btnSettingsMulti.setOnClickListener { openSettings(ClickerService.MODE_MULTI) }
        b.cardAutoStart.setOnClickListener { startActivity(Intent(this, AutoStartActivity::class.java)) }
        b.btnService.setOnClickListener { openAccessibilitySettings() }
    }

    override fun onResume() {
        super.onResume()
        renderStatus()
    }

    private fun renderStatus() {
        val on = ClickerService.instance != null
        b.tvStatusTitle.setText(if (on) R.string.status_on else R.string.status_off)
        b.tvStatusDesc.setText(if (on) R.string.status_on_desc else R.string.status_off_desc)
        b.btnService.setText(if (on) R.string.btn_service_settings else R.string.btn_enable_service)
        val color = ContextCompat.getColor(this, if (on) R.color.success else R.color.warning)
        ViewCompat.setBackgroundTintList(b.statusDot, android.content.res.ColorStateList.valueOf(color))
    }

    private fun startMode(mode: String) {
        val svc = ClickerService.instance
        if (svc == null) {
            showEnableDialog()
            return
        }
        if (mode == ClickerService.MODE_SINGLE) svc.showSingle() else svc.showMulti()
        Toast.makeText(this, R.string.toast_overlay_started, Toast.LENGTH_LONG).show()
    }

    private fun showEnableDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_service_title)
            .setMessage(R.string.dialog_service_msg)
            .setPositiveButton(R.string.btn_go_settings) { _, _ -> openAccessibilitySettings() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun openInstructions(mode: String) {
        startActivity(Intent(this, InstructionsActivity::class.java).putExtra(EXTRA_MODE, mode))
    }

    private fun openSettings(mode: String) {
        startActivity(Intent(this, SettingsActivity::class.java).putExtra(EXTRA_MODE, mode))
    }

    companion object {
        const val EXTRA_MODE = "mode"
    }
}

/** Mengambil versionName tanpa mengandalkan BuildConfig (dimatikan secara default di AGP 8+). */
object BuildConfigVersion {
    fun name(ctx: android.content.Context): String = runCatching {
        ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: ""
    }.getOrDefault("")
}
