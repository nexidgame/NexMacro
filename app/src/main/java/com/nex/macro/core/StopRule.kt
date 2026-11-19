package com.nex.macro.core

import org.json.JSONObject
import java.util.Calendar

/**
 * Pemicu "Stop After": tanpa batas, setelah N menit, pada jam tertentu, atau setelah N putaran.
 * Pada mode multi 1 putaran = satu kali melewati seluruh daftar target; pada mode tunggal = 1 klik.
 */
data class StopRule(
    var mode: Int = INDEFINITE,
    var minutes: Int = 2,
    var hour: Int = 12,
    var minute: Int = 0,
    var runs: Int = 50,
) {
    fun durationSeconds(): Int = if (mode == DURATION) minutes * 60 else 0

    fun runLimit(): Int = if (mode == RUN_COUNT) runs else 0

    /** Waktu henti (epoch ms) untuk mode AT_TIME: kemunculan berikutnya dari jam:menit tsb. */
    fun atMillis(): Long {
        if (mode != AT_TIME) return 0L
        val c = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (c.timeInMillis <= System.currentTimeMillis()) c.add(Calendar.DAY_OF_YEAR, 1)
        return c.timeInMillis
    }

    fun toJson(): JSONObject = JSONObject()
        .put("mode", mode).put("minutes", minutes)
        .put("hour", hour).put("minute", minute).put("runs", runs)

    companion object {
        const val INDEFINITE = 0
        const val DURATION = 1
        const val AT_TIME = 2
        const val RUN_COUNT = 3

        fun fromJson(o: JSONObject?): StopRule {
            if (o == null) return StopRule()
            return StopRule(
                mode = o.optInt("mode", INDEFINITE).coerceIn(0, 3),
                minutes = o.optInt("minutes", 2).coerceAtLeast(1),
                hour = o.optInt("hour", 12).coerceIn(0, 23),
                minute = o.optInt("minute", 0).coerceIn(0, 59),
                runs = o.optInt("runs", 50).coerceAtLeast(1),
            )
        }

        fun fromString(s: String?): StopRule =
            runCatching { fromJson(JSONObject(s ?: "")) }.getOrDefault(StopRule())
    }
}
