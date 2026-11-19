package com.nex.macro.core

import org.json.JSONObject

enum class TargetType { CLICK, SWIPE }

/**
 * Satu target pada mode multi. Koordinat adalah titik tengah target di layar (piksel).
 * Untuk SWIPE, (x,y) adalah titik awal dan (x2,y2) titik akhir.
 */
class MacroTarget(
    var type: TargetType,
    var x: Int,
    var y: Int,
    var x2: Int = 0,
    var y2: Int = 0,
    var enabled: Boolean = true,
    var startDelay: Int = 0,
    var endDelay: Int = 100,
    var duration: Int = 25,
    var repeat: Int = 0,
    var disableAfter: Int = 0,
    var radius: Int = 0,
) {
    /** Jumlah eksekusi pada sesi berjalan (tidak disimpan). */
    var runCount: Int = 0

    /** True bila target dimatikan otomatis oleh "Disable After" (tidak disimpan). */
    var autoDisabled: Boolean = false

    fun toJson(): JSONObject = JSONObject().apply {
        put("type", type.name)
        put("x", x); put("y", y); put("x2", x2); put("y2", y2)
        put("enabled", enabled)
        put("startDelay", startDelay); put("endDelay", endDelay); put("duration", duration)
        put("repeat", repeat); put("disableAfter", disableAfter); put("radius", radius)
    }

    companion object {
        fun fromJson(o: JSONObject): MacroTarget = MacroTarget(
            type = runCatching { TargetType.valueOf(o.optString("type", "CLICK")) }
                .getOrDefault(TargetType.CLICK),
            x = o.optInt("x"), y = o.optInt("y"),
            x2 = o.optInt("x2"), y2 = o.optInt("y2"),
            enabled = o.optBoolean("enabled", true),
            startDelay = o.optInt("startDelay", 0),
            endDelay = o.optInt("endDelay", 100),
            duration = o.optInt("duration", 25),
            repeat = o.optInt("repeat", 0),
            disableAfter = o.optInt("disableAfter", 0),
            radius = o.optInt("radius", 0),
        )
    }
}

/** Satu langkah yang akan dieksekusi oleh [ClickEngine]. */
data class Step(
    val startDelay: Long,
    val endDelay: Long,
    val duration: Long,
    val swipe: Boolean,
    val x: Float,
    val y: Float,
    val x2: Float = 0f,
    val y2: Float = 0f,
    val tag: Any? = null,
)
