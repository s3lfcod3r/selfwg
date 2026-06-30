package com.selfwg.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.wireguard.config.Config
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.StringReader
import java.util.UUID

/**
 * Ein gespeicherter Tunnel/„Konto".
 * @param appMode "all" = alle Apps, "include" = nur diese, "exclude" = alle ausser diese
 * @param apps Paketnamen fuer Split-Tunnel nach App
 */
data class TunnelEntry(
    val id: String,
    val name: String,
    val conf: String,
    val appMode: String = "all",
    val apps: List<String> = emptyList()
)

/**
 * Verschluesselter Mehrfach-Tunnel-Speicher. Genau ein Tunnel ist „aktiv".
 */
object TunnelStore {
    private const val FILE = "selfwg_secure"
    private const val KEY_TUNNELS = "tunnels"
    private const val KEY_ACTIVE = "active_id"
    private const val OLD_CONF = "conf"
    private const val OLD_LABEL = "label"

    @Volatile
    private var cached: SharedPreferences? = null

    private fun prefs(ctx: Context): SharedPreferences {
        cached?.let { return it }
        return synchronized(this) {
            cached ?: EncryptedSharedPreferences.create(
                FILE,
                MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
                ctx.applicationContext,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            ).also { cached = it; migrate(it) }
        }
    }

    private fun migrate(p: SharedPreferences) {
        if (p.contains(KEY_TUNNELS)) return
        val oldConf = p.getString(OLD_CONF, null)
        if (!oldConf.isNullOrBlank()) {
            val name = p.getString(OLD_LABEL, "SelfWG") ?: "SelfWG"
            val id = UUID.randomUUID().toString()
            val arr = JSONArray().put(toJson(TunnelEntry(id, name, oldConf)))
            p.edit().putString(KEY_TUNNELS, arr.toString()).putString(KEY_ACTIVE, id)
                .remove(OLD_CONF).remove(OLD_LABEL).apply()
        } else {
            p.edit().putString(KEY_TUNNELS, "[]").apply()
        }
    }

    private fun toJson(e: TunnelEntry) = JSONObject().apply {
        put("id", e.id)
        put("name", e.name)
        put("conf", e.conf)
        put("appMode", e.appMode)
        put("apps", JSONArray(e.apps))
    }

    private fun fromJson(o: JSONObject): TunnelEntry {
        val appsArr = o.optJSONArray("apps") ?: JSONArray()
        val apps = (0 until appsArr.length()).map { appsArr.getString(it) }
        return TunnelEntry(
            id = o.getString("id"),
            name = o.getString("name"),
            conf = o.getString("conf"),
            appMode = o.optString("appMode", "all"),
            apps = apps
        )
    }

    fun list(ctx: Context): List<TunnelEntry> {
        val raw = prefs(ctx).getString(KEY_TUNNELS, "[]") ?: "[]"
        val arr = JSONArray(raw)
        return (0 until arr.length()).map { fromJson(arr.getJSONObject(it)) }
    }

    private fun save(ctx: Context, items: List<TunnelEntry>) {
        val arr = JSONArray()
        items.forEach { arr.put(toJson(it)) }
        prefs(ctx).edit().putString(KEY_TUNNELS, arr.toString()).apply()
    }

    fun activeId(ctx: Context): String? = prefs(ctx).getString(KEY_ACTIVE, null)

    fun setActive(ctx: Context, id: String?) {
        val e = prefs(ctx).edit()
        if (id == null) e.remove(KEY_ACTIVE) else e.putString(KEY_ACTIVE, id)
        e.apply()
    }

    fun active(ctx: Context): TunnelEntry? {
        val items = list(ctx)
        val id = activeId(ctx)
        return items.firstOrNull { it.id == id } ?: items.firstOrNull()
    }

    fun byId(ctx: Context, id: String): TunnelEntry? = list(ctx).firstOrNull { it.id == id }

    fun hasAny(ctx: Context): Boolean = list(ctx).isNotEmpty()

    /** Aktive Config inkl. eingesetzter App-Ein-/Ausschluss-Zeilen. */
    fun activeConfig(ctx: Context): Config? {
        val e = active(ctx) ?: return null
        return parse(injectApps(e.conf, e.appMode, e.apps))
    }

    fun add(ctx: Context, name: String, conf: String): TunnelEntry {
        val e = TunnelEntry(UUID.randomUUID().toString(), name.trim().ifBlank { "Tunnel" }, conf.trim())
        save(ctx, list(ctx) + e)
        if (activeId(ctx) == null) setActive(ctx, e.id)
        return e
    }

    fun update(ctx: Context, id: String, name: String, conf: String, appMode: String, apps: List<String>) {
        save(ctx, list(ctx).map {
            if (it.id == id) it.copy(
                name = name.trim().ifBlank { "Tunnel" },
                conf = conf.trim(),
                appMode = appMode,
                apps = apps
            ) else it
        })
    }

    fun delete(ctx: Context, id: String) {
        val remaining = list(ctx).filterNot { it.id == id }
        save(ctx, remaining)
        if (activeId(ctx) == id) setActive(ctx, remaining.firstOrNull()?.id)
    }

    fun parse(conf: String): Config = Config.parse(BufferedReader(StringReader(conf)))

    /** Setzt IncludedApplications/ExcludedApplications passend in die [Interface]-Sektion. */
    private fun injectApps(conf: String, mode: String, apps: List<String>): String {
        val cleaned = conf.lineSequence().filterNot {
            val t = it.trim().lowercase()
            t.startsWith("includedapplications") || t.startsWith("excludedapplications")
        }.toList()
        if (mode == "all" || apps.isEmpty()) return cleaned.joinToString("\n")
        val key = if (mode == "include") "IncludedApplications" else "ExcludedApplications"
        val line = "$key = ${apps.joinToString(", ")}"
        val out = ArrayList<String>(cleaned.size + 1)
        var inserted = false
        for (l in cleaned) {
            out.add(l)
            if (!inserted && l.trim().equals("[Interface]", ignoreCase = true)) {
                out.add(line)
                inserted = true
            }
        }
        if (!inserted) {
            out.add("[Interface]")
            out.add(line)
        }
        return out.joinToString("\n")
    }
}
