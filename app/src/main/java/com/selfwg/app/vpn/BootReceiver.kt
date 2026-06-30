package com.selfwg.app.vpn

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.selfwg.app.data.Prefs
import com.selfwg.app.data.TunnelStore

/** Verbindet nach Geraete-Neustart automatisch, wenn gewuenscht. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        if (!Prefs.autoConnectOnBoot(context)) return
        if (!Prefs.isIntendedUp(context)) return
        if (!TunnelStore.hasAny(context)) return
        runCatching { SelfWgService.start(context) }
    }
}
