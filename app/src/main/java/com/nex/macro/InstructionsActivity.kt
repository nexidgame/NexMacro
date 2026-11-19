package com.nex.macro

import android.os.Bundle
import android.text.Html
import androidx.appcompat.app.AppCompatActivity
import com.nex.macro.databinding.ActivityInstructionsBinding
import com.nex.macro.service.ClickerService
import com.nex.macro.ui.applySystemBarsPadding

class InstructionsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val b = ActivityInstructionsBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.root.applySystemBarsPadding()
        b.toolbar.setNavigationOnClickListener { finish() }

        val single = intent.getStringExtra(MainActivity.EXTRA_MODE) == ClickerService.MODE_SINGLE
        val html = getString(if (single) R.string.instr_single_html else R.string.instr_multi_html)
        b.tvBody.text = Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT)
    }
}
