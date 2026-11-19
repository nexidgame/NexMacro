package com.nex.macro

import android.app.Application
import com.nex.macro.core.Prefs

class NexApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
    }
}
