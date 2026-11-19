package com.nex.macro.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import com.nex.macro.core.ClickEngine
import com.nex.macro.core.Prefs
import com.nex.macro.overlay.MultiOverlay
import com.nex.macro.overlay.SingleOverlay

/**
 * Jantung Nex Macro. Layanan aksesibilitas ini:
 *  - mengirim gestur tap/swipe (canPerformGestures),
 *  - menampilkan overlay lewat TYPE_ACCESSIBILITY_OVERLAY (tanpa izin "tampil di atas aplikasi lain"),
 *  - memantau aplikasi di latar depan untuk "Pause On App Change" dan "Auto Start".
 */
class ClickerService : AccessibilityService() {

    companion object {
        const val MODE_SINGLE = "single"
        const val MODE_MULTI = "multi"

        @Volatile
        var instance: ClickerService? = null
            private set
    }

    lateinit var engine: ClickEngine
        private set

    private val handler = Handler(Looper.getMainLooper())
    private var single: SingleOverlay? = null
    private var multi: MultiOverlay? = null

    var foregroundPkg: String? = null
        private set
    private var runPkg: String? = null
    private var receiverRegistered = false

    /** Layar mati / terkunci -> hentikan (emergency stop). */
    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) engine.stop()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Prefs.init(this)
        engine = ClickEngine(this)
        instance = this
        if (!receiverRegistered) {
            ContextCompat.registerReceiver(
                this, screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            receiverRegistered = true
        }
    }

    val overlayShown: Boolean get() = single != null || multi != null

    fun showSingle() {
        hideOverlays()
        Prefs.lastMode = MODE_SINGLE
        single = SingleOverlay(this).also { it.show() }
    }

    fun showMulti(autoLoad: Boolean = false) {
        hideOverlays()
        Prefs.lastMode = MODE_MULTI
        multi = MultiOverlay(this).also { it.show(autoLoad) }
    }

    fun hideOverlays() {
        engine.onStopped = null
        engine.onStepStart = null
        engine.onStepEnd = null
        engine.stop()
        single?.hide()
        multi?.hide()
        single = null
        multi = null
    }

    /** Dipanggil overlay saat mulai menjalankan klik. */
    fun beginRun() {
        runPkg = foregroundPkg
    }

    // ----------------------------------------------------------------- event

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName || pkg == "com.android.systemui" || pkg == "android" || isIme(pkg)) return
        if (pkg == foregroundPkg) return
        foregroundPkg = pkg
        onForegroundChanged(pkg)
    }

    private fun isIme(pkg: String): Boolean = runCatching {
        getSystemService(InputMethodManager::class.java).enabledInputMethodList.any { it.packageName == pkg }
    }.getOrDefault(false)

    private fun onForegroundChanged(pkg: String) {
        // Pause On App Change
        if (engine.running) {
            val pauseOnChange = if (multi != null) Prefs.multiPauseOnAppChange else Prefs.singlePauseOnAppChange
            if (pauseOnChange && runPkg != null && runPkg != pkg) engine.stop()
        }
        // Auto Start
        if (pkg in Prefs.autoStartPackages) {
            if (!overlayShown) {
                if (Prefs.lastMode == MODE_SINGLE) showSingle() else showMulti(autoLoad = true)
            }
            if (Prefs.autoStartClick && !engine.running) {
                handler.postDelayed({
                    if (foregroundPkg == pkg && !engine.running) {
                        single?.startIfIdle()
                        multi?.startIfIdle()
                    }
                }, 800)
            }
        }
    }

    override fun onInterrupt() {
        engine.stop()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        cleanup()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        cleanup()
        super.onDestroy()
    }

    private fun cleanup() {
        if (::engine.isInitialized) hideOverlays()
        if (receiverRegistered) {
            runCatching { unregisterReceiver(screenOffReceiver) }
            receiverRegistered = false
        }
        handler.removeCallbacksAndMessages(null)
        instance = null
    }
}
