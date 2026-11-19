package com.nex.macro

import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.nex.macro.core.Prefs
import com.nex.macro.core.StopRule
import com.nex.macro.databinding.ActivitySettingsBinding
import com.nex.macro.service.ClickerService
import com.nex.macro.ui.Dialogs
import com.nex.macro.ui.UiKit
import com.nex.macro.ui.applySystemBarsPadding

/** Pengaturan mode tunggal & multi target (dibangun programatik agar mudah dirawat). */
class SettingsActivity : AppCompatActivity() {

    private lateinit var b: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.root.applySystemBarsPadding()
        b.toolbar.setNavigationOnClickListener { finish() }

        if (intent.getStringExtra(MainActivity.EXTRA_MODE) == ClickerService.MODE_SINGLE) {
            b.toolbar.setTitle(R.string.settings_title_single)
            buildSingle()
        } else {
            b.toolbar.setTitle(R.string.settings_title_multi)
            buildMulti()
        }
    }

    private fun numberRow(
        title: Int, value: () -> Int, max: Int, set: (Int) -> Unit,
        display: (Int) -> String = { UiKit.ms(this, it) },
        hint: Int = R.string.hint_ms,
    ): UiKit.SettingRow {
        val row = UiKit.row(this, getString(title), display(value())) {}
        row.view.setOnClickListener {
            Dialogs.number(this, false, getString(title), getString(hint), value(), 0, max) { v ->
                set(v)
                row.setValue(display(v))
            }
        }
        return row
    }

    private fun stopRow(get: () -> StopRule, set: (StopRule) -> Unit): UiKit.SettingRow {
        val row = UiKit.row(this, getString(R.string.s_stop_after), UiKit.stopText(this, get())) {}
        row.view.setOnClickListener {
            Dialogs.stopRule(this, false, get()) { r ->
                set(r)
                row.setValue(UiKit.stopText(this, r))
            }
        }
        return row
    }

    private fun buildSingle() {
        val c = b.container
        c.addView(UiKit.header(this, getString(R.string.sec_basic)))
        c.addView(numberRow(R.string.s_time_between, { Prefs.singleInterval }, 3_600_000, { Prefs.singleInterval = it }).view)
        c.addView(numberRow(R.string.s_click_duration, { Prefs.singleDuration }, 60_000, { Prefs.singleDuration = it }).view)
        c.addView(stopRow({ Prefs.singleStop }, { Prefs.singleStop = it }).view)
        c.addView(UiKit.header(this, getString(R.string.sec_advanced)))
        c.addView(
            UiKit.switchRow(
                this, getString(R.string.s_pause_app), getString(R.string.s_pause_app_desc),
                Prefs.singlePauseOnAppChange
            ) { Prefs.singlePauseOnAppChange = it }
        )
        c.addView(speedWarning())
    }

    private fun buildMulti() {
        val c = b.container
        c.addView(UiKit.header(this, getString(R.string.sec_default)))
        c.addView(numberRow(R.string.s_start_delay, { Prefs.multiStartDelay }, 3_600_000, { Prefs.multiStartDelay = it }).view)
        c.addView(numberRow(R.string.s_end_delay, { Prefs.multiEndDelay }, 3_600_000, { Prefs.multiEndDelay = it }).view)
        c.addView(numberRow(R.string.s_swipe_duration, { Prefs.multiDuration }, 60_000, { Prefs.multiDuration = it }).view)
        c.addView(stopRow({ Prefs.multiStop }, { Prefs.multiStop = it }).view)

        c.addView(UiKit.header(this, getString(R.string.sec_randomization)))
        c.addView(
            numberRow(R.string.s_radius, { Prefs.multiRadius }, 500, { Prefs.multiRadius = it },
                display = { UiKit.px(this, it) }, hint = R.string.hint_px).view
        )

        c.addView(UiKit.header(this, getString(R.string.sec_advanced)))
        c.addView(
            UiKit.switchRow(
                this, getString(R.string.s_show_active), getString(R.string.s_show_active_desc),
                Prefs.multiShowActive
            ) { Prefs.multiShowActive = it }
        )
        c.addView(
            UiKit.switchRow(
                this, getString(R.string.s_programmability), getString(R.string.s_programmability_desc),
                Prefs.multiProgrammability
            ) { Prefs.multiProgrammability = it }
        )
        c.addView(
            UiKit.switchRow(
                this, getString(R.string.s_pause_app), getString(R.string.s_pause_app_desc),
                Prefs.multiPauseOnAppChange
            ) { Prefs.multiPauseOnAppChange = it }
        )

        val manage = MaterialButton(this).apply {
            setText(R.string.s_manage)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, UiKit.dp(this@SettingsActivity, 52)
            ).also {
                val m = UiKit.dp(this@SettingsActivity, 16)
                it.setMargins(m, m, m, 0)
            }
            setOnClickListener {
                startActivity(android.content.Intent(this@SettingsActivity, ConfigListActivity::class.java))
            }
        }
        c.addView(manage)
        c.addView(speedWarning())
    }

    private fun speedWarning() = android.widget.TextView(this).apply {
        text = getString(R.string.speed_warning)
        setTextColor(androidx.core.content.ContextCompat.getColor(this@SettingsActivity, R.color.on_surface_variant))
        textSize = 12f
        setPadding(UiKit.dp(context, 16), UiKit.dp(context, 16), UiKit.dp(context, 16), UiKit.dp(context, 8))
    }
}
