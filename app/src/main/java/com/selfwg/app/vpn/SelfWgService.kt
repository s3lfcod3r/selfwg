package com.selfwg.app.vpn

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.selfwg.app.MainActivity
import com.selfwg.app.R
import com.selfwg.app.SelfWgApp
import com.selfwg.app.data.Prefs
import com.selfwg.app.data.TunnelStore
import com.wireguard.android.backend.Tunnel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Vordergrund-Dienst: hält den Prozess wach und betreibt den Reconnect-
 * Wächter. Kernidee: regelmäßig prüfen, ob die Heim-IP des Servers
 * gewechselt hat (Zwangstrennung) und dann den Tunnel mit frischer IP neu
 * aufbauen. Zusätzlich ein Doze-Weckalarm (über [AlarmReceiver]) als
 * Sicherheitsnetz.
 */
class SelfWgService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var watchJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification())
        when (intent?.action) {
            ACTION_STOP -> {
                stopEverything()
                return START_NOT_STICKY
            }
            ACTION_CHECK -> {
                scope.launch { watchTick() }
                scheduleAlarm()
            }
            ACTION_SWITCH -> {
                // Aktiven Tunnel gewechselt: alten runter, neuen rauf.
                scope.launch {
                    runCatching {
                        TunnelManager.down(null)
                        val cfg = TunnelStore.activeConfig(this@SelfWgService)
                        if (cfg != null) TunnelManager.up(cfg)
                    }
                }
            }
            else -> { // ACTION_START
                Prefs.setIntendedUp(this, true)
                startWatchdog()
                scheduleAlarm()
                scope.launch { ensureUp() }
            }
        }
        return START_STICKY
    }

    private suspend fun ensureUp() {
        val cfg = runCatching { TunnelStore.activeConfig(this) }.getOrNull() ?: return
        runCatching { TunnelManager.up(cfg) }
    }

    private fun startWatchdog() {
        if (watchJob?.isActive == true) return
        watchJob = scope.launch {
            while (isActive) {
                delay(CHECK_INTERVAL_MS)
                runCatching { watchTick() }
            }
        }
    }

    /** Kernlogik: Tunnel tot oder neue Server-IP? -> neu aufbauen. */
    private suspend fun watchTick() {
        if (!Prefs.isIntendedUp(this)) return
        val cfg = runCatching { TunnelStore.activeConfig(this) }.getOrNull() ?: return

        if (TunnelManager.currentState() != Tunnel.State.UP) {
            runCatching { TunnelManager.up(cfg) }
            return
        }

        val newIp = TunnelManager.resolveFresh(cfg) ?: return
        val applied = TunnelManager.lastAppliedIp
        if (applied != null && newIp != applied) {
            // Heim-IP hat gewechselt -> Tunnel mit frischer IP neu aufbauen.
            runCatching { TunnelManager.reconnect(cfg) }
        }
    }

    private fun stopEverything() {
        Prefs.setIntendedUp(this, false)
        watchJob?.cancel()
        cancelAlarm()
        val cfg = runCatching { TunnelStore.activeConfig(this) }.getOrNull()
        scope.launch {
            try {
                runCatching { TunnelManager.down(cfg) }
            } finally {
                withContext(Dispatchers.Main) {
                    stopForegroundCompat()
                    stopSelf()
                }
            }
        }
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    private fun buildNotification() =
        NotificationCompat.Builder(this, SelfWgApp.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_vpn)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(getString(R.string.notif_text))
            .setOngoing(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()

    private fun scheduleAlarm() {
        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val trigger = SystemClock.elapsedRealtime() + ALARM_INTERVAL_MS
        // allow-while-idle: feuert auch im Doze-Modus (ohne Spezialrechte).
        am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, trigger, alarmPi())
    }

    private fun cancelAlarm() {
        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(alarmPi())
    }

    // Weckalarm geht an einen BroadcastReceiver: nur von dort darf im
    // Hintergrund/Doze ein Vordergrund-Dienst gestartet werden.
    private fun alarmPi(): PendingIntent {
        val i = Intent(this, AlarmReceiver::class.java)
        return PendingIntent.getBroadcast(
            this, 1, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    companion object {
        const val ACTION_START = "com.selfwg.app.START"
        const val ACTION_STOP = "com.selfwg.app.STOP"
        const val ACTION_CHECK = "com.selfwg.app.CHECK"
        const val ACTION_SWITCH = "com.selfwg.app.SWITCH"

        private const val NOTIF_ID = 4711
        private const val CHECK_INTERVAL_MS = 60_000L
        private const val ALARM_INTERVAL_MS = 15 * 60_000L

        fun start(ctx: Context) {
            val i = Intent(ctx, SelfWgService::class.java).setAction(ACTION_START)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(i)
            } else {
                ctx.startService(i)
            }
        }

        fun stop(ctx: Context) {
            val i = Intent(ctx, SelfWgService::class.java).setAction(ACTION_STOP)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(i)
            } else {
                ctx.startService(i)
            }
        }

        fun check(ctx: Context) {
            val i = Intent(ctx, SelfWgService::class.java).setAction(ACTION_CHECK)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(i)
            } else {
                ctx.startService(i)
            }
        }

        /** Auf den aktuell aktiven Tunnel umschalten (alten runter, neuen rauf). */
        fun switchTunnel(ctx: Context) {
            val i = Intent(ctx, SelfWgService::class.java).setAction(ACTION_SWITCH)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(i)
            } else {
                ctx.startService(i)
            }
        }
    }
}
