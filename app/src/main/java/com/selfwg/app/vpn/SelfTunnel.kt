package com.selfwg.app.vpn

import com.wireguard.android.backend.Tunnel

/** Tunnel-Adapter; meldet Zustandswechsel an den TunnelManager zurueck. */
class SelfTunnel(private val onState: (Tunnel.State) -> Unit) : Tunnel {
    // Name muss 1-15 Zeichen aus [a-zA-Z0-9_=+.-] sein.
    override fun getName(): String = "SelfWG"

    override fun onStateChange(newState: Tunnel.State) {
        onState(newState)
    }
}
