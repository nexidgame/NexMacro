package com.nex.macro.core

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.WindowManager

/**
 * Mesin eksekusi: mengambil [Step] dari sebuah sumber, menunggu jeda awal,
 * mengirim gestur (tap / swipe) lewat AccessibilityService, menunggu jeda akhir, lalu mengulang.
 * Semua dijalankan di main thread.
 */
class ClickEngine(private val service: AccessibilityService) {

    private val handler = Handler(Looper.getMainLooper())

    var running = false
        private set

    var onStepStart: ((Any?) -> Unit)? = null
    var onStepEnd: ((Any?) -> Unit)? = null
    var onStopped: (() -> Unit)? = null

    private var token = 0
    private var executed = 0
    private var startedAt = 0L
    private var stopCount = 0
    private var stopSeconds = 0
    private var source: (() -> Step?)? = null

    /** @param stopCount 0 = tanpa batas. @param stopSeconds 0 = tanpa batas. */
    fun start(stopCount: Int, stopSeconds: Int, source: () -> Step?) {
        if (running) return
        this.stopCount = stopCount
        this.stopSeconds = stopSeconds
        this.source = source
        executed = 0
        startedAt = SystemClock.elapsedRealtime()
        running = true
        token++
        loop(token)
    }

    fun stop() {
        if (!running) return
        running = false
        token++
        handler.removeCallbacksAndMessages(null)
        source = null
        onStopped?.invoke()
    }

    private fun limitReached(): Boolean {
        if (stopCount > 0 && executed >= stopCount) return true
        if (stopSeconds > 0 && SystemClock.elapsedRealtime() - startedAt >= stopSeconds * 1000L) return true
        return false
    }

    private fun loop(myToken: Int) {
        if (!running || myToken != token) return
        if (limitReached()) {
            stop(); return
        }
        val step = source?.invoke()
        if (step == null) {
            stop(); return
        }
        handler.postDelayed({
            if (!running || myToken != token) return@postDelayed
            onStepStart?.invoke(step.tag)
            dispatch(step) {
                if (running && myToken == token) {
                    executed++
                    onStepEnd?.invoke(step.tag)
                    handler.postDelayed({ loop(myToken) }, step.endDelay.coerceAtLeast(0))
                }
            }
        }, step.startDelay.coerceAtLeast(0))
    }

    private fun dispatch(step: Step, done: () -> Unit) {
        val bounds = service.getSystemService(WindowManager::class.java).currentWindowMetrics.bounds
        val maxX = (bounds.width() - 1).toFloat()
        val maxY = (bounds.height() - 1).toFloat()

        val path = Path().apply {
            moveTo(step.x.coerceIn(0f, maxX), step.y.coerceIn(0f, maxY))
            if (step.swipe) lineTo(step.x2.coerceIn(0f, maxX), step.y2.coerceIn(0f, maxY))
        }
        val duration = step.duration.coerceIn(1L, GestureDescription.getMaxGestureDuration())
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
            .build()

        val accepted = service.dispatchGesture(
            gesture,
            object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) = done()
                override fun onCancelled(gestureDescription: GestureDescription?) = done()
            },
            handler,
        )
        if (!accepted) handler.postDelayed({ done() }, 50)
    }
}
