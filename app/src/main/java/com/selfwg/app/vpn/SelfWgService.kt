package com.selfwg.app.vpn

import android.app.AlarmManager
import android.app.NotificationManager
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
import com.wireguard.config.BadConfigException
import com.wireguard.config.Config
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

    /** Anzahl aufeinanderfolgender fehlgeschlagener Verbindungsversuche. */
    @Volatile
    private var failureCount = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification())
        when (intent?.action) {
            ACTION_STOP -> {
                stopEverything()
                return START_NOT_STICKY
            }
            ACTION_CHECK -> {
                // Nach Doze-Alarm: Wächter-Schleife wieder starten, wenn der
                // Tunnel an sein soll. startWatchdog() startet sie nur einmal
                // (watchJob?.isActive), das bleibt so.
                if (Prefs.isIntendedUp(this)) startWatchdog()
                scheduleAlarm()
            }
            ACTION_SWITCH -> {
                // Aktiven Tunnel gewechselt: alten runter, neuen rauf.
                scope.launch {
                    TunnelManager.down(null)
                    val cfg = activeConfig()
                    if (cfg != null) TunnelManager.up(cfg)
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
        val cfg = activeConfig() ?: return
        TunnelManager.up(cfg)
    }

    /** Aktive Config; Parse-Fehler werden als CONNECT_FAILED gemeldet,
     * nicht mehr per runCatching verschluckt. */
    private fun activeConfig(): Config? {
        try {
            return TunnelStore.activeConfig(this)
        } catch (e: BadConfigException) {
            TunnelManager.connectFailed(TunnelManager.shortFailureDetail(e))
            return null
        }
    }

    private fun startWatchdog() {
        if (watchJob?.isActive == true) return
        watchJob = scope.launch {
            while (isActive) {
                // Bei wiederholtem Fehlschlag exponentiell länger warten,
                // sonst normales Prüfintervall.
                delay(nextDelayMs())
                runCatching { watchTick() }
            }
        }
    }

    /** Wartezeit bis zum nächsten Versuch: normal, oder Backoff bei Fehlern. */
    private fun nextDelayMs(): Long {
        if (failureCount <= 0) return CHECK_INTERVAL_MS
        val idx = (failureCount - 1).coerceAtMost(BACKOFF_STEPS_MS.lastIndex)
        return BACKOFF_STEPS_MS[idx]
    }

    /**
     * Kernlogik: Tunnel tot oder neue Server-IP? -> neu aufbauen.
     * Zählt Fehlversuche für den Backoff und zeigt bei anhaltendem Fehlschlag
     * einen Hinweis in der Vordergrund-Notification.
     */
    private suspend fun watchTick() {
        if (!Prefs.isIntendedUp(this)) return
        val cfg = activeConfig() ?: return

        if (TunnelManager.currentState() != Tunnel.State.UP) {
            runCatching { TunnelManager.up(cfg) }
            if (TunnelManager.currentState() == Tunnel.State.UP) {
                onConnectSuccess()
            } else {
                onConnectFailure()
            }
            return
        }

        // Tunnel ist oben -> als Erfolg werten (Zähler/Notification zurücksetzen).
        onConnectSuccess()

        val newIp = TunnelManager.resolveFresh(cfg) ?: return
        val applied = TunnelManager.lastAppliedIp
        if (applied != null && newIp != applied) {
            // Heim-IP hat gewechselt -> Tunnel mit frischer IP neu aufbauen.
            runCatching { TunnelManager.reconnect(cfg) }
        }
    }

    private fun onConnectSuccess() {
        if (failureCount != 0) {
            failureCount = 0
            updateNotification(failed = false)
        }
    }

    private fun onConnectFailure() {
        failureCount++
        updateNotification(failed = true)
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

    private fun buildNotification(failed: Boolean = false) =
        NotificationCompat.Builder(this, SelfWgApp.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_vpn)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(notificationText(failed))
            .setOngoing(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()

    /** Normaler Status oder Fehlschlag-Hinweis inkl. Wartezeit bis zum Retry. */
    private fun notificationText(failed: Boolean): String =
        if (failed) {
            val minutes = (nextDelayMs() / 60_000L).coerceAtLeast(1)
            getString(R.string.notif_retry, minutes)
        } else {
            getString(R.string.notif_text)
        }

    private fun updateNotification(failed: Boolean) {
        runCatching {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(NOTIF_ID, buildNotification(failed))
        }
    }

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

        // Exponentieller Backoff bei wiederholtem Verbindungsfehler: 1/2/5/15 min.
        private val BACKOFF_STEPS_MS = longArrayOf(
            1 * 60_000L, 2 * 60_000L, 5 * 60_000L, 15 * 60_000L
        )

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
