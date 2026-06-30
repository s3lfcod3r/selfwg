package com.selfwg.app.vpn

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.selfwg.app.data.Prefs

/**
 * Empfaengt den Doze-Weckalarm. Ein BroadcastReceiver bekommt beim Aufwachen
 * ein kurzes Zeitfenster, in dem er einen Vordergrund-Dienst starten darf —
 * im Gegensatz zu einem direkt vom AlarmManager gestarteten Dienst, der unter
 * Android 12+ blockiert wuerde.
 */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (!Prefs.isIntendedUp(context)) return
        runCatching { SelfWgService.check(context) }
    }
}
