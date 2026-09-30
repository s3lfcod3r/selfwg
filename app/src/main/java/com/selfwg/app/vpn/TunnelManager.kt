package com.selfwg.app.vpn

import android.content.Context
import com.wireguard.android.backend.Backend
import com.wireguard.android.backend.BackendException
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.BadConfigException
import com.wireguard.config.Config
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.InetAddress

/**
 * Verwaltet das WireGuard-Backend (GoBackend aus com.wireguard.android:tunnel).
 * Single source of truth für den Tunnel-Zustand.
 *
 * Alle nativen Operationen laufen serialisiert über [opMutex] — der native
 * Kern darf NICHT von zwei Threads gleichzeitig angefasst werden.
 */
object TunnelManager {
    private var backend: Backend? = null

    // Einziger Schreiber von _state ist die onStateChange-Callback.
    private val tunnel = SelfTunnel { st -> _state.value = st }

    private val opMutex = Mutex()

    private val _state = MutableStateFlow(Tunnel.State.DOWN)
    val state: StateFlow<Tunnel.State> = _state.asStateFlow()

    private val _serverIp = MutableStateFlow<String?>(null)
    val serverIp: StateFlow<String?> = _serverIp.asStateFlow()

    /**
     * Letzter Fehlerzustand für die UI. null = alles ok. Wird gesetzt, wenn das
     * native Backend nicht verfügbar ist oder eine Operation ins Timeout läuft.
     */
    private val _lastError = MutableStateFlow<TunnelError?>(null)
    val lastError: StateFlow<TunnelError?> = _lastError.asStateFlow()

    /** Kurze Fehlerart zu CONNECT_FAILED (Klassenname oder Backend-/BadConfig-Reason). */
    private val _failureDetail = MutableStateFlow<String?>(null)
    val failureDetail: StateFlow<String?> = _failureDetail.asStateFlow()

    /** IP, mit der der Tunnel zuletzt aufgebaut wurde (für Wechsel-Erkennung). */
    @Volatile
    var lastAppliedIp: String? = null
        private set

    fun init(ctx: Context) {
        if (backend != null) return
        try {
            backend = GoBackend(ctx.applicationContext)
            if (_lastError.value == TunnelError.BACKEND_UNAVAILABLE) _lastError.value = null
        } catch (e: Throwable) {
            // Fehlende/inkompatible libwg-go.so o. Ä. -> Backend bleibt null.
            android.util.Log.e(TAG, "GoBackend init failed", e)
            _lastError.value = TunnelError.BACKEND_UNAVAILABLE
        }
    }

    fun currentState(): Tunnel.State = try {
        backend?.getState(tunnel) ?: Tunnel.State.DOWN
    } catch (e: Exception) {
        Tunnel.State.DOWN
    }

    suspend fun up(config: Config) = withContext(Dispatchers.IO) {
        opMutex.withLock {
            // Timeout, damit ein hängender nativer Aufruf nicht opMutex dauerhaft blockiert.
            val ok = try {
                withTimeoutOrNull(OP_TIMEOUT_MS) {
                    runInterruptible { rawUp(config) }
                    true
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                connectFailed(shortFailureDetail(e))
                return@withLock
            }
            if (ok == null) onOpTimeout("up")
        }
    }

    suspend fun down(config: Config?) = withContext(Dispatchers.IO) {
        opMutex.withLock {
            val ok = withTimeoutOrNull(OP_TIMEOUT_MS) {
                runInterruptible { rawDown(config) }
                true
            }
            if (ok == null) onOpTimeout("down")
        }
    }

    /** Tunnel komplett neu aufbauen (z.B. nach Server-IP-Wechsel). Atomar. */
    suspend fun reconnect(config: Config) = withContext(Dispatchers.IO) {
        opMutex.withLock {
            val ok = try {
                withTimeoutOrNull(OP_TIMEOUT_MS) {
                    runInterruptible { rawDown(config) }
                    delay(400)
                    runInterruptible { rawUp(config) }
                    true
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                connectFailed(shortFailureDetail(e))
                return@withLock
            }
            if (ok == null) onOpTimeout("reconnect")
        }
    }

    private fun onOpTimeout(op: String) {
        android.util.Log.e(TAG, "Tunnel operation timed out: $op")
        _lastError.value = TunnelError.OP_TIMEOUT
        _failureDetail.value = null
    }

    /** Setzt den CONNECT_FAILED-Zustand (auch von außen, z. B. bei Parse-Fehlern). */
    fun connectFailed(detail: String) {
        _lastError.value = TunnelError.CONNECT_FAILED
        _failureDetail.value = detail
    }

    /** Kurze, UI-taugliche Fehlerart: nur Klassenname oder Backend-/BadConfig-Reason.
     * Keine Config-Inhalte oder Schlüssel. */
    fun shortFailureDetail(e: Exception): String = when (e) {
        is BackendException -> e.reason.name
        is BadConfigException -> "${e.section} (${e.location}): ${e.reason}"
        else -> e::class.simpleName ?: "Fehler"
    }

    private fun rawUp(config: Config) {
        val b = backend
        if (b == null) {
            _lastError.value = TunnelError.BACKEND_UNAVAILABLE
            return
        }
        b.setState(tunnel, Tunnel.State.UP, config)
        // _state wird über onStateChange gesetzt (einziger Schreiber).
        val ip = resolveBlocking(config)
        lastAppliedIp = ip
        _serverIp.value = ip
        // Erfolg -> vorherige Betriebsfehler löschen (Backend-Fehler bleibt).
        if (_lastError.value == TunnelError.OP_TIMEOUT || _lastError.value == TunnelError.CONNECT_FAILED) {
            _lastError.value = null
            _failureDetail.value = null
        }
    }

    private fun rawDown(config: Config?) {
        val b = backend ?: return
        try {
            // GoBackend benutzt config beim DOWN nicht; null ist hier ok.
            b.setState(tunnel, Tunnel.State.DOWN, config)
        } catch (e: Exception) {
            // bereits unten / kein gültiger Zustand -> ignorieren
        }
    }

    /** Frische, abbruchbare DNS-Auflösung mit Timeout (für den Wächter). */
    suspend fun resolveFresh(config: Config): String? = withTimeoutOrNull(5_000) {
        runInterruptible(Dispatchers.IO) { resolveBlocking(config) }
    }

    private fun resolveBlocking(config: Config): String? = try {
        val endpoint = config.peers.firstOrNull()?.endpoint?.orElse(null)
        if (endpoint == null) null else InetAddress.getByName(endpoint.host).hostAddress
    } catch (e: Exception) {
        null
    }

    private const val TAG = "TunnelManager"

    // Deckel für native JNI-Operationen. Der native Kern kann in seltenen
    // Fällen hängen; ohne Deckel würde opMutex dauerhaft blockiert.
    private const val OP_TIMEOUT_MS = 20_000L
}

/** Für die UI sichtbarer Fehlerzustand des Tunnel-Backends. */
enum class TunnelError {
    /** Natives VPN-Backend (libwg-go.so) fehlt oder ließ sich nicht laden. */
    BACKEND_UNAVAILABLE,

    /** Eine Tunnel-Operation lief ins Timeout (nativer Aufruf hing). */
    OP_TIMEOUT,

    /** Aufbauen/Reconnect ist fehlgeschlagen (z. B. Backend- oder Parse-Fehler). */
    CONNECT_FAILED,
}
