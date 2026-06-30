package com.selfwg.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.selfwg.app.vpn.TunnelManager

class SelfWgApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Hinweis: Android nutzt den nativen netd-Resolver; die frische
        // Aufloesung im Waechter geht ueber InetAddress mit kurzem Timeout und
        // greift, sobald der DDNS-Eintrag (niedrige TTL) den IP-Wechsel zeigt.
        TunnelManager.init(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            // Alten Kanal (mit Badge) aufraeumen.
            runCatching { nm.deleteNotificationChannel("selfwg_status") }
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notif_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setShowBadge(false)   // kein „1"-Abzeichen am App-Icon
            }
            nm.createNotificationChannel(channel)
        }
    }

    companion object {
        // Neue ID, damit setShowBadge(false) auch bei bestehenden Installationen greift
        // (Kanal-Einstellungen sind nach dem Anlegen unveraenderlich).
        const val CHANNEL_ID = "selfwg_status2"
    }
}
