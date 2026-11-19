package com.nex.macro.core

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/** Penyimpanan pengaturan aplikasi (SharedPreferences). */
object Prefs {
    internal lateinit var sp: SharedPreferences

    fun init(context: Context) {
        if (!::sp.isInitialized) {
            sp = context.applicationContext
                .getSharedPreferences("nex_macro_prefs", Context.MODE_PRIVATE)
        }
    }

    // ---- Mode target tunggal ----
    var singleInterval by IntPref("s_interval", 100)
    var singleDuration by IntPref("s_duration", 25)
    var singleStopCount by IntPref("s_stop_count", 0)
    var singleStopSeconds by IntPref("s_stop_seconds", 0)
    var singlePauseOnAppChange by BoolPref("s_pause_app", false)
    var singleCorner by IntPref("s_corner", 0)
    var singleX by IntPref("s_x", -1)
    var singleY by IntPref("s_y", -1)

    // ---- Mode multi target ----
    var multiStartDelay by IntPref("m_start_delay", 0)
    var multiEndDelay by IntPref("m_end_delay", 100)
    var multiDuration by IntPref("m_duration", 25)
    var multiStopCount by IntPref("m_stop_count", 0)
    var multiStopSeconds by IntPref("m_stop_seconds", 0)
    var multiRadius by IntPref("m_radius", 0)
    var multiShowActive by BoolPref("m_show_active", true)
    var multiProgrammability by BoolPref("m_programmability", true)
    var multiPauseOnAppChange by BoolPref("m_pause_app", false)
    var panelX by IntPref("m_panel_x", -1)
    var panelY by IntPref("m_panel_y", -1)
    var currentConfig by StringPref("m_current_config", "")

    // ---- Umum / Auto Start ----
    var lastMode by StringPref("last_mode", "multi")
    var autoStartClick by BoolPref("auto_click", false)

    var autoStartPackages: Set<String>
        get() = sp.getStringSet("auto_pkgs", emptySet())?.toSet() ?: emptySet()
        set(value) = sp.edit { putStringSet("auto_pkgs", value.toSet()) }
}

private class IntPref(private val key: String, private val def: Int) : ReadWriteProperty<Any?, Int> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Int = Prefs.sp.getInt(key, def)
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
        Prefs.sp.edit { putInt(key, value) }
    }
}

private class BoolPref(private val key: String, private val def: Boolean) : ReadWriteProperty<Any?, Boolean> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Boolean = Prefs.sp.getBoolean(key, def)
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
        Prefs.sp.edit { putBoolean(key, value) }
    }
}

private class StringPref(private val key: String, private val def: String) : ReadWriteProperty<Any?, String> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): String = Prefs.sp.getString(key, def) ?: def
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
        Prefs.sp.edit { putString(key, value) }
    }
}
