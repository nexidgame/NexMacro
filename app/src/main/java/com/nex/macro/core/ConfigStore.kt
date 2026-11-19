package com.nex.macro.core

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

data class LoadedConfig(
    val targets: List<MacroTarget>,
    val stopCount: Int,
    val stopSeconds: Int,
)

/** Menyimpan / memuat konfigurasi multi target bernama (JSON di SharedPreferences). */
object ConfigStore {
    private fun sp(ctx: Context) =
        ctx.applicationContext.getSharedPreferences("nex_macro_configs", Context.MODE_PRIVATE)

    fun names(ctx: Context): List<String> = sp(ctx).all.keys.sortedBy { it.lowercase() }

    fun exists(ctx: Context, name: String): Boolean = sp(ctx).contains(name)

    fun save(ctx: Context, name: String, targets: List<MacroTarget>, stopCount: Int, stopSeconds: Int) {
        val arr = JSONArray()
        targets.forEach { arr.put(it.toJson()) }
        val obj = JSONObject()
            .put("targets", arr)
            .put("stopCount", stopCount)
            .put("stopSeconds", stopSeconds)
        sp(ctx).edit { putString(name, obj.toString()) }
    }

    fun load(ctx: Context, name: String): LoadedConfig? {
        val raw = sp(ctx).getString(name, null) ?: return null
        return runCatching {
            val obj = JSONObject(raw)
            val arr = obj.optJSONArray("targets") ?: JSONArray()
            val list = (0 until arr.length()).map { MacroTarget.fromJson(arr.getJSONObject(it)) }
            LoadedConfig(list, obj.optInt("stopCount", 0), obj.optInt("stopSeconds", 0))
        }.getOrNull()
    }

    fun delete(ctx: Context, name: String) {
        sp(ctx).edit { remove(name) }
    }
}
