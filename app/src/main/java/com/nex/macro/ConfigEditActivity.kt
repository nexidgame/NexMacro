package com.nex.macro

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nex.macro.core.ConfigStore
import com.nex.macro.core.MacroTarget
import com.nex.macro.core.Prefs
import com.nex.macro.core.StopRule
import com.nex.macro.core.TargetType
import com.nex.macro.databinding.ActivityConfigEditBinding
import com.nex.macro.databinding.ItemActionBinding
import com.nex.macro.service.ClickerService
import com.nex.macro.ui.Dialogs
import com.nex.macro.ui.TargetDialog
import com.nex.macro.ui.UiKit
import com.nex.macro.ui.applySystemBarsPadding

/**
 * "Edit Configuration": ganti nama, Stop After, serta daftar aksi (lihat di layar, ubah,
 * salin, hapus, urutkan dengan tarik). Setiap perubahan langsung tersimpan.
 */
class ConfigEditActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_NAME = "name"
    }

    private lateinit var b: ActivityConfigEditBinding
    private lateinit var name: String
    private var stop = StopRule()
    private val targets = mutableListOf<MacroTarget>()
    private var previewIdx = -1
    private val adapter = Adapter()
    private lateinit var touch: ItemTouchHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val n = intent.getStringExtra(EXTRA_NAME)
        val cfg = n?.let { ConfigStore.load(this, it) }
        if (n == null || cfg == null) {
            finish()
            return
        }
        name = n
        stop = cfg.stop
        targets.addAll(cfg.targets)

        b = ActivityConfigEditBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.root.applySystemBarsPadding()
        b.toolbar.setNavigationOnClickListener { finish() }

        b.list.layoutManager = LinearLayoutManager(this)
        b.list.adapter = adapter
        touch = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder,
            ): Boolean {
                val from = vh.bindingAdapterPosition
                val to = target.bindingAdapterPosition
                if (from < 0 || to < 0) return false
                targets.add(to, targets.removeAt(from))
                adapter.notifyItemMoved(from, to)
                return true
            }

            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {}

            override fun isLongPressDragEnabled() = false

            override fun clearView(rv: RecyclerView, vh: RecyclerView.ViewHolder) {
                super.clearView(rv, vh)
                persist()
                resetPreview()
                rv.post { adapter.notifyDataSetChanged() }
            }
        })
        touch.attachToRecyclerView(b.list)

        b.rowName.setOnClickListener { rename() }
        b.rowStop.setOnClickListener {
            Dialogs.stopRule(this, false, stop) { r ->
                stop = r
                persist()
                render()
            }
        }
        b.fab.setOnClickListener { chooseNewAction() }
        render()
    }

    override fun onStop() {
        super.onStop()
        if (previewIdx >= 0) {
            ClickerService.instance?.clearPreview()
            previewIdx = -1
            adapter.notifyDataSetChanged()
        }
    }

    private fun render() {
        b.toolbar.title = getString(R.string.edit_config_fmt, name)
        b.tvName.text = name
        b.tvStop.text = UiKit.stopText(this, stop)
    }

    private fun persist() {
        ConfigStore.save(this, name, targets, stop)
    }

    private fun rename() {
        Dialogs.text(this, false, getString(R.string.cfg_name_prompt), initial = name) { newName ->
            if (newName == name) {
                // tidak ada perubahan
            } else if (ConfigStore.rename(this, name, newName)) {
                name = newName
                render()
            } else {
                Toast.makeText(this, R.string.cfg_name_exists, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun resetPreview() {
        if (previewIdx >= 0) {
            ClickerService.instance?.clearPreview()
            previewIdx = -1
        }
    }

    private fun chooseNewAction() {
        val items = listOf(getString(R.string.type_click), getString(R.string.type_swipe))
        Dialogs.items(this, false, getString(R.string.add_action_title), items) { i ->
            val swipe = i == 1
            val dm = resources.displayMetrics
            val cx = dm.widthPixels / 2
            val cy = dm.heightPixels / 2
            val off = UiKit.dp(this, 70)
            targets.add(
                MacroTarget(
                    type = if (swipe) TargetType.SWIPE else TargetType.CLICK,
                    x = if (swipe) cx - off else cx,
                    y = cy,
                    x2 = cx + off,
                    y2 = cy,
                    startDelay = Prefs.multiStartDelay,
                    endDelay = Prefs.multiEndDelay,
                    duration = if (swipe) maxOf(Prefs.multiDuration, 200) else Prefs.multiDuration,
                    radius = Prefs.multiRadius,
                )
            )
            persist()
            resetPreview()
            adapter.notifyDataSetChanged()
            b.list.scrollToPosition(targets.size - 1)
        }
    }

    private fun togglePreview(pos: Int) {
        val svc = ClickerService.instance
        if (svc == null) {
            Toast.makeText(this, R.string.need_service_toast, Toast.LENGTH_SHORT).show()
            return
        }
        if (previewIdx == pos) {
            svc.clearPreview()
            previewIdx = -1
        } else {
            svc.showPreview(listOf((pos + 1) to targets[pos]))
            previewIdx = pos
        }
        adapter.notifyItemRangeChanged(0, targets.size)
    }

    private fun editAction(pos: Int) {
        val t = targets[pos]
        TargetDialog.show(
            this, false, pos + 1, t, programmable = true,
            cb = object : TargetDialog.Callbacks {
                override fun onRemove() {
                    val i = targets.indexOf(t)
                    if (i >= 0) removeAt(i)
                }

                override fun onChanged() {
                    persist()
                    if (previewIdx == targets.indexOf(t) && previewIdx >= 0) {
                        ClickerService.instance?.showPreview(listOf((previewIdx + 1) to t))
                    }
                }
            },
            showCoordinates = true,
        )
    }

    private fun duplicateAt(pos: Int) {
        val copy = targets[pos].duplicate().also {
            val d = UiKit.dp(this, 24)
            it.x += d; it.y += d; it.x2 += d; it.y2 += d
        }
        targets.add(pos + 1, copy)
        persist()
        resetPreview()
        adapter.notifyDataSetChanged()
    }

    private fun removeAt(pos: Int) {
        targets.removeAt(pos)
        persist()
        resetPreview()
        adapter.notifyDataSetChanged()
    }

    private inner class Adapter : RecyclerView.Adapter<Adapter.VH>() {

        inner class VH(val vb: ItemActionBinding) : RecyclerView.ViewHolder(vb.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(ItemActionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun getItemCount() = targets.size

        @SuppressLint("ClickableViewAccessibility")
        override fun onBindViewHolder(holder: VH, position: Int) {
            val t = targets[position]
            val typeName = getString(if (t.type == TargetType.SWIPE) R.string.type_swipe else R.string.type_click)
            holder.vb.tvLabel.text = getString(R.string.action_type_fmt, position + 1, typeName)

            holder.vb.btnPreview.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    this@ConfigEditActivity,
                    if (previewIdx == position) R.color.primary else R.color.surface_high,
                )
            )
            holder.vb.btnPreview.setOnClickListener {
                holder.bindingAdapterPosition.takeIf { it >= 0 }?.let { togglePreview(it) }
            }
            holder.vb.btnEdit.setOnClickListener {
                holder.bindingAdapterPosition.takeIf { it >= 0 }?.let { editAction(it) }
            }
            holder.vb.btnCopy.setOnClickListener {
                holder.bindingAdapterPosition.takeIf { it >= 0 }?.let { duplicateAt(it) }
            }
            holder.vb.btnDelete.setOnClickListener {
                val p = holder.bindingAdapterPosition
                if (p >= 0) {
                    Dialogs.confirm(this@ConfigEditActivity, false, getString(R.string.delete_target_q)) {
                        removeAt(p)
                    }
                }
            }
            holder.vb.handle.setOnTouchListener { _, e ->
                if (e.actionMasked == MotionEvent.ACTION_DOWN) touch.startDrag(holder)
                false
            }
        }
    }
}
