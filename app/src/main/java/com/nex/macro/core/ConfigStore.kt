package com.nex.macro.core

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

data class LoadedConfig(
    val targets: List<MacroTarget>,
    val stop: StopRule,
)

/** Menyimpan / memuat konfigurasi multi target bernama (JSON di SharedPreferences). */
object ConfigStore {
    private fun sp(ctx: Context) =
        ctx.applicationContext.getSharedPreferences("nex_macro_configs", Context.MODE_PRIVATE)

    fun names(ctx: Context): List<String> = sp(ctx).all.keys.sortedBy { it.lowercase() }

    fun exists(ctx: Context, name: String): Boolean = sp(ctx).contains(name)

    fun save(ctx: Context, name: String, targets: List<MacroTarget>, stop: StopRule) {
        val arr = JSONArray()
        targets.forEach { arr.put(it.toJson()) }
        val obj = JSONObject().put("targets", arr).put("stop", stop.toJson())
        sp(ctx).edit { putString(name, obj.toString()) }
    }

    fun load(ctx: Context, name: String): LoadedConfig? {
        val raw = sp(ctx).getString(name, null) ?: return null
        return runCatching {
            val obj = JSONObject(raw)
            val arr = obj.optJSONArray("targets") ?: JSONArray()
            val list = (0 until arr.length()).map { MacroTarget.fromJson(arr.getJSONObject(it)) }
            LoadedConfig(list, StopRule.fromJson(obj.optJSONObject("stop")))
        }.getOrNull()
    }

    fun delete(ctx: Context, name: String) {
        sp(ctx).edit { remove(name) }
    }

    /** @return false bila nama baru sudah dipakai konfigurasi lain. */
    fun rename(ctx: Context, old: String, new: String): Boolean {
        if (old == new) return true
        if (exists(ctx, new)) return false
        val raw = sp(ctx).getString(old, null) ?: return false
        sp(ctx).edit {
            putString(new, raw)
            remove(old)
        }
        if (Prefs.currentConfig == old) Prefs.currentConfig = new
        return true
    }

    /** @return false bila konfigurasi asal tidak ada atau nama salinan sudah dipakai. */
    fun copy(ctx: Context, name: String, newName: String): Boolean {
        if (exists(ctx, newName)) return false
        val raw = sp(ctx).getString(name, null) ?: return false
        sp(ctx).edit { putString(newName, raw) }
        return true
    }

    /** @return false bila nama sudah dipakai. */
    fun create(ctx: Context, name: String): Boolean {
        if (exists(ctx, name)) return false
        save(ctx, name, emptyList(), StopRule())
        return true
    }
}
