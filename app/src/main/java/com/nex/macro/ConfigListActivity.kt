package com.nex.macro

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nex.macro.core.ConfigStore
import com.nex.macro.databinding.ActivityConfigListBinding
import com.nex.macro.databinding.ItemConfigBinding
import com.nex.macro.service.ClickerService
import com.nex.macro.ui.Dialogs
import com.nex.macro.ui.applySystemBarsPadding

/** "Saved Configurations": lihat di layar, ubah, salin, hapus, dan buat konfigurasi baru. */
class ConfigListActivity : AppCompatActivity() {

    private lateinit var b: ActivityConfigListBinding
    private val names = mutableListOf<String>()
    private var previewName: String? = null
    private val adapter = Adapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityConfigListBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.root.applySystemBarsPadding()
        b.toolbar.setNavigationOnClickListener { finish() }

        b.list.layoutManager = LinearLayoutManager(this)
        b.list.adapter = adapter
        b.list.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        b.fab.setOnClickListener { createNew() }
    }

    override fun onResume() {
        super.onResume()
        reload()
    }

    override fun onStop() {
        super.onStop()
        ClickerService.instance?.clearPreview()
        if (previewName != null) {
            previewName = null
            adapter.notifyDataSetChanged()
        }
    }

    private fun reload() {
        names.clear()
        names.addAll(ConfigStore.names(this))
        adapter.notifyDataSetChanged()
        b.tvEmpty.visibility = if (names.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun toast(res: Int) = Toast.makeText(this, res, Toast.LENGTH_SHORT).show()

    private fun createNew() {
        Dialogs.text(this, false, getString(R.string.cfg_name_prompt)) { name ->
            if (ConfigStore.create(this, name)) {
                startActivity(Intent(this, ConfigEditActivity::class.java).putExtra(ConfigEditActivity.EXTRA_NAME, name))
            } else {
                toast(R.string.cfg_name_exists)
            }
        }
    }

    private fun togglePreview(name: String) {
        val svc = ClickerService.instance
        if (svc == null) {
            toast(R.string.need_service_toast)
            return
        }
        if (previewName == name) {
            svc.clearPreview()
            previewName = null
        } else {
            val cfg = ConfigStore.load(this, name) ?: return
            svc.showPreview(cfg.targets.mapIndexed { i, t -> (i + 1) to t })
            previewName = name
        }
        adapter.notifyItemRangeChanged(0, names.size)
    }

    private fun copy(name: String) {
        Dialogs.text(
            this, false, getString(R.string.cfg_copy_prompt),
            initial = "$name (copy)", positive = R.string.btn_copy,
        ) { newName ->
            if (ConfigStore.copy(this, name, newName)) reload() else toast(R.string.cfg_name_exists)
        }
    }

    private fun delete(name: String) {
        Dialogs.confirm(this, false, getString(R.string.cfg_delete_this_q)) {
            ConfigStore.delete(this, name)
            if (com.nex.macro.core.Prefs.currentConfig == name) com.nex.macro.core.Prefs.currentConfig = ""
            if (previewName == name) {
                ClickerService.instance?.clearPreview()
                previewName = null
            }
            reload()
        }
    }

    private inner class Adapter : RecyclerView.Adapter<Adapter.VH>() {

        inner class VH(val vb: ItemConfigBinding) : RecyclerView.ViewHolder(vb.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(ItemConfigBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun getItemCount() = names.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val name = names[position]
            holder.vb.tvName.text = name
            val active = previewName == name
            holder.vb.btnPreview.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(this@ConfigListActivity, if (active) R.color.primary else R.color.surface_high)
            )
            holder.vb.btnPreview.setOnClickListener { togglePreview(name) }
            holder.vb.btnEdit.setOnClickListener {
                startActivity(
                    Intent(this@ConfigListActivity, ConfigEditActivity::class.java)
                        .putExtra(ConfigEditActivity.EXTRA_NAME, name)
                )
            }
            holder.vb.btnCopy.setOnClickListener { copy(name) }
            holder.vb.btnDelete.setOnClickListener { delete(name) }
        }
    }
}
