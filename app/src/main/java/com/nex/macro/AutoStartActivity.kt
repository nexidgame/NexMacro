package com.nex.macro

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nex.macro.core.Prefs
import com.nex.macro.databinding.ActivityAutostartBinding
import com.nex.macro.databinding.ItemAppBinding
import com.nex.macro.ui.applySystemBarsPadding
import java.util.concurrent.Executors

/** Pilih aplikasi yang akan memunculkan overlay Nex Macro secara otomatis saat dibuka. */
class AutoStartActivity : AppCompatActivity() {

    private data class AppItem(val label: String, val pkg: String, val icon: Drawable)

    private lateinit var b: ActivityAutostartBinding
    private val selected = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityAutostartBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.root.applySystemBarsPadding()
        b.toolbar.setNavigationOnClickListener { finish() }

        b.swAutoClick.isChecked = Prefs.autoStartClick
        b.swAutoClick.setOnCheckedChangeListener { _, on -> Prefs.autoStartClick = on }

        selected.addAll(Prefs.autoStartPackages)
        b.list.layoutManager = LinearLayoutManager(this)

        Executors.newSingleThreadExecutor().execute {
            val pm = packageManager
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val apps = pm.queryIntentActivities(intent, 0)
                .asSequence()
                .filter { it.activityInfo.packageName != packageName }
                .map { AppItem(it.loadLabel(pm).toString(), it.activityInfo.packageName, it.loadIcon(pm)) }
                .distinctBy { it.pkg }
                .sortedBy { it.label.lowercase() }
                .toList()
            runOnUiThread {
                if (isDestroyed) return@runOnUiThread
                b.progress.visibility = View.GONE
                b.list.visibility = View.VISIBLE
                b.list.adapter = Adapter(apps)
            }
        }
    }

    private inner class Adapter(private val items: List<AppItem>) : RecyclerView.Adapter<Adapter.VH>() {

        inner class VH(val vb: ItemAppBinding) : RecyclerView.ViewHolder(vb.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(ItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            holder.vb.imgIcon.setImageDrawable(item.icon)
            holder.vb.tvLabel.text = item.label
            holder.vb.tvPackage.text = item.pkg
            holder.vb.check.isChecked = item.pkg in selected
            holder.vb.root.setOnClickListener {
                if (!selected.remove(item.pkg)) selected.add(item.pkg)
                Prefs.autoStartPackages = selected.toSet()
                holder.vb.check.isChecked = item.pkg in selected
            }
        }
    }
}
