package com.selfwg.app.vpn

import android.content.Context
import com.wireguard.android.backend.Backend
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
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
 * Single source of truth fuer den Tunnel-Zustand.
 *
 * Alle nativen Operationen laufen serialisiert ueber [opMutex] — der native
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

    /** IP, mit der der Tunnel zuletzt aufgebaut wurde (fuer Wechsel-Erkennung). */
    @Volatile
    var lastAppliedIp: String? = null
        private set

    fun init(ctx: Context) {
        if (backend == null) backend = GoBackend(ctx.applicationContext)
    }

    fun currentState(): Tunnel.State = try {
        backend?.getState(tunnel) ?: Tunnel.State.DOWN
    } catch (e: Exception) {
        Tunnel.State.DOWN
    }

    suspend fun up(config: Config) = withContext(Dispatchers.IO) {
        opMutex.withLock { rawUp(config) }
    }

    suspend fun down(config: Config?) = withContext(Dispatchers.IO) {
        opMutex.withLock { rawDown(config) }
    }

    /** Tunnel komplett neu aufbauen (z.B. nach Server-IP-Wechsel). Atomar. */
    suspend fun reconnect(config: Config) = withContext(Dispatchers.IO) {
        opMutex.withLock {
            rawDown(config)
            delay(400)
            rawUp(config)
        }
    }

    private fun rawUp(config: Config) {
        val b = backend ?: return
        b.setState(tunnel, Tunnel.State.UP, config)
        // _state wird ueber onStateChange gesetzt (einziger Schreiber).
        val ip = resolveBlocking(config)
        lastAppliedIp = ip
        _serverIp.value = ip
    }

    private fun rawDown(config: Config?) {
        val b = backend ?: return
        try {
            // GoBackend benutzt config beim DOWN nicht; null ist hier ok.
            b.setState(tunnel, Tunnel.State.DOWN, config)
        } catch (e: Exception) {
            // bereits unten / kein gueltiger Zustand -> ignorieren
        }
    }

    /** Frische, abbruchbare DNS-Aufloesung mit Timeout (fuer den Waechter). */
    suspend fun resolveFresh(config: Config): String? = withTimeoutOrNull(5_000) {
        runInterruptible(Dispatchers.IO) { resolveBlocking(config) }
    }

    private fun resolveBlocking(config: Config): String? = try {
        val endpoint = config.peers.firstOrNull()?.endpoint?.orElse(null)
        if (endpoint == null) null else InetAddress.getByName(endpoint.host).hostAddress
    } catch (e: Exception) {
        null
    }
}
