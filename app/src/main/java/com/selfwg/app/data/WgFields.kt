package com.selfwg.app.data

/**
 * Wandelt zwischen WireGuard-Config-Text und einzelnen Feldern hin und her,
 * damit der Editor beschriftete Felder statt eines Rohtext-Blocks zeigen kann.
 * Deckt die ueblichen WG-Easy-Felder ab; unbekannte Zeilen werden beim
 * Neu-Erzeugen weggelassen (fuer Standard-Configs unkritisch).
 */
object WgFields {

    data class Fields(
        // [Interface]
        val privateKey: String = "",
        val address: String = "",
        val dns: String = "",
        val mtu: String = "",
        // [Peer]
        val publicKey: String = "",
        val presharedKey: String = "",
        val endpoint: String = "",
        val allowedIps: String = "",
        val keepalive: String = ""
    )

    fun parse(conf: String): Fields {
        var section = ""
        val iface = HashMap<String, String>()
        val peer = HashMap<String, String>()
        for (rawLine in conf.lines()) {
            val line = rawLine.substringBefore('#').trim()
            if (line.isEmpty()) continue
            if (line.startsWith("[") && line.endsWith("]")) {
                section = line.lowercase()
                continue
            }
            val idx = line.indexOf('=')
            if (idx <= 0) continue
            val key = line.substring(0, idx).trim().lowercase()
            val value = line.substring(idx + 1).trim()
            when (section) {
                "[interface]" -> iface[key] = value
                "[peer]" -> peer[key] = value
            }
        }
        return Fields(
            privateKey = iface["privatekey"] ?: "",
            address = iface["address"] ?: "",
            dns = iface["dns"] ?: "",
            mtu = iface["mtu"] ?: "",
            publicKey = peer["publickey"] ?: "",
            presharedKey = peer["presharedkey"] ?: "",
            endpoint = peer["endpoint"] ?: "",
            allowedIps = peer["allowedips"] ?: "",
            keepalive = peer["persistentkeepalive"] ?: ""
        )
    }

    fun build(f: Fields): String {
        val sb = StringBuilder()
        sb.appendLine("[Interface]")
        sb.appendLine("PrivateKey = ${f.privateKey.trim()}")
        if (f.address.isNotBlank()) sb.appendLine("Address = ${f.address.trim()}")
        if (f.dns.isNotBlank()) sb.appendLine("DNS = ${f.dns.trim()}")
        if (f.mtu.isNotBlank()) sb.appendLine("MTU = ${f.mtu.trim()}")
        sb.appendLine()
        sb.appendLine("[Peer]")
        sb.appendLine("PublicKey = ${f.publicKey.trim()}")
        if (f.presharedKey.isNotBlank()) sb.appendLine("PresharedKey = ${f.presharedKey.trim()}")
        if (f.endpoint.isNotBlank()) sb.appendLine("Endpoint = ${f.endpoint.trim()}")
        if (f.allowedIps.isNotBlank()) sb.appendLine("AllowedIPs = ${f.allowedIps.trim()}")
        if (f.keepalive.isNotBlank()) sb.appendLine("PersistentKeepalive = ${f.keepalive.trim()}")
        return sb.toString().trim()
    }
}
