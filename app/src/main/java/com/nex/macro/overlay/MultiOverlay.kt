package com.nex.macro.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.ContextThemeWrapper
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import com.nex.macro.R
import com.nex.macro.core.ConfigStore
import com.nex.macro.core.LoadedConfig
import com.nex.macro.core.MacroTarget
import com.nex.macro.core.Prefs
import com.nex.macro.core.Step
import com.nex.macro.core.TargetType
import com.nex.macro.service.ClickerService
import com.nex.macro.ui.ConfigDialogs
import com.nex.macro.ui.TargetDialog
import com.nex.macro.ui.UiKit
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Overlay mode multi target.
 *
 * Panel: ▶ (mulai/jeda) · ＋ (tap: target klik, tahan: target swipe) · － (tap: hapus terakhir, tahan: hapus semua)
 *        · ⚙ (konfigurasi) · › (tap: ciutkan, tahan: pindahkan panel)
 * Tap sebuah target membuka dialog pengaturannya. Target dijalankan berurutan lalu berulang dari awal.
 */
class MultiOverlay(private val svc: ClickerService) {

    private val ctx: Context = ContextThemeWrapper(svc, R.style.Theme_NexMacro)
    private val wm: WindowManager = svc.getSystemService(WindowManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private val rnd = java.util.Random()

    private val targetSize = UiKit.dp(ctx, 56)
    private val panelW = UiKit.dp(ctx, 56)

    private class Handle(val view: TargetView, val lp: WindowManager.LayoutParams)

    private class Entry(val model: MacroTarget) {
        lateinit var start: Handle
        var end: Handle? = null
    }

    private val entries = mutableListOf<Entry>()

    private lateinit var panel: LinearLayout
    private lateinit var panelLp: WindowManager.LayoutParams
    private lateinit var panelBg: GradientDrawable
    private lateinit var lineView: LineView
    private lateinit var lineLp: WindowManager.LayoutParams
    private lateinit var btnPlay: ImageButton
    private lateinit var btnAdd: ImageButton
    private lateinit var btnRemove: ImageButton
    private lateinit var btnSettings: ImageButton
    private lateinit var btnClose: ImageButton
    private lateinit var btnToggle: ImageButton

    private var collapsed = false
    private var running = false
    private var attached = false

    // ---- state eksekusi ----
    private var cursor = 0
    private var current: MacroTarget? = null
    private var repeatLeft = 0

    // =============================================================== tampil / sembunyi

    fun show(autoLoad: Boolean = false) {
        lineView = LineView(ctx)
        lineLp = OverlayUtils.params(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT, false)
        lineLp.x = 0
        lineLp.y = 0
        wm.addView(lineView, lineLp)

        buildPanel()
        wm.addView(panel, panelLp)
        attached = true
        applyState()

        if (autoLoad && Prefs.currentConfig.isNotEmpty()) {
            ConfigStore.load(ctx, Prefs.currentConfig)?.let { cfg ->
                Prefs.multiStopCount = cfg.stopCount
                Prefs.multiStopSeconds = cfg.stopSeconds
                replaceTargets(cfg.targets)
            }
        }
    }

    fun hide() {
        attached = false
        removeAllEntries()
        if (::panel.isInitialized) {
            try {
                wm.removeViewImmediate(panel)
            } catch (_: Exception) {
            }
        }
        if (::lineView.isInitialized) {
            try {
                wm.removeViewImmediate(lineView)
            } catch (_: Exception) {
            }
        }
    }

    // =============================================================== panel

    @SuppressLint("ClickableViewAccessibility")
    private fun buildPanel() {
        panelBg = OverlayUtils.roundBg(NexColors.SURFACE, NexColors.PRIMARY, 18, ctx)
        panel = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = panelBg
            val p = UiKit.dp(ctx, 4)
            setPadding(p, p, p, p)
        }

        btnPlay = OverlayUtils.iconButton(ctx, R.drawable.ic_play, 48).apply {
            setOnClickListener { if (running) stopRun() else startRun() }
        }
        btnAdd = OverlayUtils.iconButton(ctx, R.drawable.ic_add, 48).apply {
            setOnClickListener { if (!running) addTarget(false) }
            setOnLongClickListener {
                if (!running) {
                    performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    addTarget(true)
                }
                true
            }
        }
        btnRemove = OverlayUtils.iconButton(ctx, R.drawable.ic_remove, 48).apply {
            setOnClickListener { if (!running) removeLast() }
            setOnLongClickListener {
                if (!running) {
                    performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    removeAllEntries()
                }
                true
            }
        }
        btnSettings = OverlayUtils.iconButton(ctx, R.drawable.ic_settings, 48).apply {
            setOnClickListener {
                if (!running) {
                    ConfigDialogs.showSettings(
                        ctx, true,
                        getTargets = { entries.map { it.model } },
                        onLoaded = { cfg: LoadedConfig -> replaceTargets(cfg.targets) },
                    )
                }
            }
        }
        btnClose = OverlayUtils.iconButton(ctx, R.drawable.ic_close, 48).apply {
            setOnClickListener { svc.hideOverlays() }
        }
        btnToggle = OverlayUtils.iconButton(ctx, R.drawable.ic_chevron_right, 48)
        attachToggleTouch(btnToggle)

        panel.addView(btnPlay)
        panel.addView(btnAdd)
        panel.addView(btnRemove)
        panel.addView(btnSettings)
        panel.addView(btnClose)
        panel.addView(btnToggle)

        val s = OverlayUtils.screen(wm)
        panelLp = OverlayUtils.params(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
        )
        panelLp.x = if (Prefs.panelX >= 0) Prefs.panelX else s.width() - panelW - UiKit.dp(ctx, 12)
        panelLp.y = if (Prefs.panelY >= 0) Prefs.panelY else s.height() / 4
        clampPanel()
    }

    private fun clampPanel() {
        val s = OverlayUtils.screen(wm)
        panelLp.x = panelLp.x.coerceIn(0, (s.width() - panelW).coerceAtLeast(0))
        panelLp.y = panelLp.y.coerceIn(0, (s.height() - UiKit.dp(ctx, 120)).coerceAtLeast(0))
    }

    /** Tap = ciutkan/bentangkan. Tahan lalu geser = pindahkan panel. */
    @SuppressLint("ClickableViewAccessibility")
    private fun attachToggleTouch(v: View) {
        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        var dragMode = false
        var moved = false
        val slop = ViewConfiguration.get(ctx).scaledTouchSlop
        val longPress = Runnable {
            if (!running) {
                dragMode = true
                v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
        }
        v.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = e.rawX; downY = e.rawY
                    startX = panelLp.x; startY = panelLp.y
                    dragMode = false; moved = false
                    handler.postDelayed(longPress, 450)
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - downX
                    val dy = e.rawY - downY
                    if (dragMode) {
                        panelLp.x = (startX + dx).toInt()
                        panelLp.y = (startY + dy).toInt()
                        clampPanel()
                        try {
                            wm.updateViewLayout(panel, panelLp)
                        } catch (_: IllegalArgumentException) {
                        }
                    } else if (hypot(dx, dy) > slop) {
                        moved = true
                        handler.removeCallbacks(longPress)
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    handler.removeCallbacks(longPress)
                    if (dragMode) {
                        Prefs.panelX = panelLp.x
                        Prefs.panelY = panelLp.y
                    } else if (!moved && e.actionMasked == MotionEvent.ACTION_UP) {
                        toggleCollapse()
                    }
                    dragMode = false
                }
            }
            true
        }
    }

    private fun toggleCollapse() {
        collapsed = !collapsed
        applyState()
    }

    /** Menyinkronkan tampilan panel & target dengan state (running / collapsed). */
    private fun applyState() {
        if (!attached) return
        btnPlay.setImageResource(if (running) R.drawable.ic_pause else R.drawable.ic_play)
        btnToggle.setImageResource(if (collapsed) R.drawable.ic_chevron_left else R.drawable.ic_chevron_right)
        btnPlay.visibility = if (collapsed) View.GONE else View.VISIBLE
        btnAdd.visibility = if (collapsed) View.GONE else View.VISIBLE
        btnRemove.visibility = if (collapsed) View.GONE else View.VISIBLE
        btnSettings.visibility = if (collapsed) View.GONE else View.VISIBLE
        btnClose.visibility = if (collapsed) View.VISIBLE else View.GONE

        for (b in listOf(btnAdd, btnRemove, btnSettings)) {
            b.alpha = if (running) 0.35f else 1f
        }
        panelBg.setColor(if (running) NexColors.SUCCESS_BG else NexColors.SURFACE)
        panelBg.setStroke(UiKit.dp(ctx, 2), if (running) NexColors.SUCCESS else NexColors.PRIMARY)

        val visible = !collapsed && (!running || Prefs.multiShowActive)
        for (e in entries) {
            for (h in listOfNotNull(e.start, e.end)) {
                OverlayUtils.setTouchable(h.lp, !running)
                h.view.visibility = if (visible) View.VISIBLE else View.INVISIBLE
                updateLayout(h.view, h.lp)
            }
        }
        lineView.visibility = if (visible) View.VISIBLE else View.INVISIBLE
        updateLayout(panel, panelLp)
    }

    private fun updateLayout(v: View, lp: WindowManager.LayoutParams) {
        try {
            wm.updateViewLayout(v, lp)
        } catch (_: IllegalArgumentException) {
        }
    }

    // =============================================================== target

    private fun limits(): Rect {
        val s = OverlayUtils.screen(wm)
        val half = targetSize / 2
        return Rect(-half, -half, s.width() - half, s.height() - half)
    }

    private fun addTarget(swipe: Boolean) {
        val s = OverlayUtils.screen(wm)
        val step = (entries.size % 6) * UiKit.dp(ctx, 26)
        val cx = s.width() / 2 - UiKit.dp(ctx, 60) + step
        val cy = s.height() / 2 - UiKit.dp(ctx, 60) + step
        val m = MacroTarget(
            type = if (swipe) TargetType.SWIPE else TargetType.CLICK,
            x = if (swipe) cx - UiKit.dp(ctx, 70) else cx,
            y = cy,
            x2 = cx + UiKit.dp(ctx, 70),
            y2 = cy,
            startDelay = Prefs.multiStartDelay,
            endDelay = Prefs.multiEndDelay,
            duration = if (swipe) maxOf(Prefs.multiDuration, 200) else Prefs.multiDuration,
            radius = Prefs.multiRadius,
        )
        createEntry(m)
    }

    private fun createEntry(model: MacroTarget): Entry {
        val e = Entry(model)
        e.start = makeHandle(e, false)
        if (model.type == TargetType.SWIPE) e.end = makeHandle(e, true)
        entries.add(e)
        refreshLabels()
        refreshLines()
        applyState()
        return e
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun makeHandle(e: Entry, isEnd: Boolean): Handle {
        val view = TargetView(ctx).apply {
            this.isEnd = isEnd
            targetEnabled = e.model.enabled
        }
        val lp = OverlayUtils.params(targetSize, targetSize)
        val cx = if (isEnd) e.model.x2 else e.model.x
        val cy = if (isEnd) e.model.y2 else e.model.y
        lp.x = cx - targetSize / 2
        lp.y = cy - targetSize / 2
        view.setOnTouchListener(
            DragTouch(
                wm = wm, view = view, lp = lp,
                limits = ::limits,
                canDrag = { !running },
                onMove = {
                    if (isEnd) {
                        e.model.x2 = lp.x + targetSize / 2
                        e.model.y2 = lp.y + targetSize / 2
                    } else {
                        e.model.x = lp.x + targetSize / 2
                        e.model.y = lp.y + targetSize / 2
                    }
                    refreshLines()
                },
                onTap = { if (!running) editEntry(e) },
            )
        )
        wm.addView(view, lp)
        return Handle(view, lp)
    }

    private fun removeEntry(e: Entry) {
        for (h in listOfNotNull(e.start, e.end)) {
            try {
                wm.removeViewImmediate(h.view)
            } catch (_: Exception) {
            }
        }
        entries.remove(e)
        refreshLabels()
        refreshLines()
    }

    private fun removeLast() {
        entries.lastOrNull()?.let { removeEntry(it) }
    }

    private fun removeAllEntries() {
        for (e in entries.toList()) removeEntry(e)
        entries.clear()
        if (::lineView.isInitialized) refreshLines()
    }

    private fun replaceTargets(list: List<MacroTarget>) {
        removeAllEntries()
        val s = OverlayUtils.screen(wm)
        for (m in list) {
            m.x = m.x.coerceIn(0, s.width()); m.y = m.y.coerceIn(0, s.height())
            m.x2 = m.x2.coerceIn(0, s.width()); m.y2 = m.y2.coerceIn(0, s.height())
            createEntry(m)
        }
    }

    private fun refreshLabels() {
        entries.forEachIndexed { i, e ->
            val n = (i + 1).toString()
            e.start.view.label = n
            e.start.view.targetEnabled = e.model.enabled
            e.start.view.sub = if (e.model.type == TargetType.SWIPE) ctx.getString(R.string.label_start) else null
            e.end?.view?.let {
                it.label = n
                it.sub = ctx.getString(R.string.label_end)
                it.targetEnabled = e.model.enabled
            }
        }
    }

    private fun refreshLines() {
        val segs = entries.filter { it.model.type == TargetType.SWIPE }.map {
            floatArrayOf(it.model.x.toFloat(), it.model.y.toFloat(), it.model.x2.toFloat(), it.model.y2.toFloat())
        }
        lineView.setSegments(segs)
    }

    private fun editEntry(e: Entry) {
        val number = entries.indexOf(e) + 1
        TargetDialog.show(
            ctx, true, number, e.model, Prefs.multiProgrammability,
            object : TargetDialog.Callbacks {
                override fun onRemove() = removeEntry(e)
                override fun onChanged() = refreshLabels()
            },
        )
    }

    // =============================================================== eksekusi

    fun startIfIdle() {
        if (!running && attached) startRun()
    }

    private fun startRun() {
        if (running) return
        if (entries.none { it.model.enabled }) {
            Toast.makeText(ctx, R.string.toast_add_target_first, Toast.LENGTH_SHORT).show()
            return
        }
        for (e in entries) {
            e.model.runCount = 0
            if (e.model.autoDisabled) {
                e.model.enabled = true
                e.model.autoDisabled = false
            }
        }
        refreshLabels()
        cursor = 0
        current = null
        repeatLeft = 0
        running = true
        applyState()
        svc.beginRun()

        val engine = svc.engine
        engine.onStepStart = { tag -> setActive(tag, true) }
        engine.onStepEnd = { tag -> setActive(tag, false) }
        engine.onStopped = {
            running = false
            for (e in entries) {
                e.start.view.active = false
                e.end?.view?.active = false
            }
            if (attached) {
                refreshLabels()
                applyState()
            }
        }
        engine.start(Prefs.multiStopCount, Prefs.multiStopSeconds) { nextStep() }
    }

    private fun stopRun() {
        svc.engine.stop()
    }

    private fun setActive(tag: Any?, on: Boolean) {
        val m = tag as? MacroTarget ?: return
        entries.firstOrNull { it.model === m }?.let {
            it.start.view.active = on
            it.end?.view?.active = on
        }
    }

    /** Mengambil langkah berikutnya: menghormati Repeat, Disable After, dan target nonaktif. */
    private fun nextStep(): Step? {
        val prog = Prefs.multiProgrammability
        val list = entries.map { it.model }
        if (list.isEmpty()) return null

        val cur = current
        if (cur != null && repeatLeft > 0 && cur.enabled && list.contains(cur)) {
            repeatLeft--
            return execute(cur, prog)
        }
        val n = list.size
        for (i in 0 until n) {
            val t = list[(cursor + i) % n]
            if (t.enabled) {
                cursor = (cursor + i + 1) % n
                current = t
                repeatLeft = if (prog) t.repeat.coerceAtLeast(0) else 0
                return execute(t, prog)
            }
        }
        return null
    }

    private fun execute(t: MacroTarget, prog: Boolean): Step {
        t.runCount++
        val step = buildStep(t, prog)
        if (prog && t.disableAfter > 0 && t.runCount >= t.disableAfter) {
            t.enabled = false
            t.autoDisabled = true
            refreshLabels()
        }
        return step
    }

    private fun buildStep(t: MacroTarget, prog: Boolean): Step {
        val startDelay = if (prog) t.startDelay else Prefs.multiStartDelay
        val endDelay = if (prog) t.endDelay else Prefs.multiEndDelay
        val duration = if (prog) t.duration else Prefs.multiDuration
        val radius = if (prog) t.radius else Prefs.multiRadius
        val swipe = t.type == TargetType.SWIPE
        val (x, y) = jitter(t.x, t.y, radius)
        val (x2, y2) = if (swipe) jitter(t.x2, t.y2, radius) else (0f to 0f)
        return Step(
            startDelay = startDelay.toLong(),
            endDelay = endDelay.toLong(),
            duration = duration.toLong(),
            swipe = swipe,
            x = x, y = y, x2 = x2, y2 = y2,
            tag = t,
        )
    }

    /** Titik acak di dalam lingkaran berradius [r] piksel. */
    private fun jitter(x: Int, y: Int, r: Int): Pair<Float, Float> {
        if (r <= 0) return x.toFloat() to y.toFloat()
        val angle = rnd.nextDouble() * Math.PI * 2
        val dist = sqrt(rnd.nextDouble()) * r
        return (x + (cos(angle) * dist).toFloat()) to (y + (sin(angle) * dist).toFloat())
    }
}
