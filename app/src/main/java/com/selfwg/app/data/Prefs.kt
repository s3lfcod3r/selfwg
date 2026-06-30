package com.selfwg.app.data

import android.content.Context

/** Einfache (nicht geheime) App-Einstellungen. */
object Prefs {
    private const val FILE = "selfwg_prefs"
    private fun p(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** Soll der Tunnel an sein? (überlebt Neustart/Prozess-Kill) */
    fun isIntendedUp(ctx: Context): Boolean = p(ctx).getBoolean("intended_up", false)
    fun setIntendedUp(ctx: Context, value: Boolean) =
        p(ctx).edit().putBoolean("intended_up", value).apply()

    /** Nach Geräte-Neustart automatisch verbinden. */
    fun autoConnectOnBoot(ctx: Context): Boolean = p(ctx).getBoolean("auto_boot", true)
    fun setAutoConnectOnBoot(ctx: Context, value: Boolean) =
        p(ctx).edit().putBoolean("auto_boot", value).apply()

    /** App beim Start mit Fingerabdruck/Gerätesperre schützen. */
    fun biometricEnabled(ctx: Context): Boolean = p(ctx).getBoolean("biometric", false)
    fun setBiometricEnabled(ctx: Context, value: Boolean) =
        p(ctx).edit().putBoolean("biometric", value).apply()

    /** Sprache der Oberflaeche: "de" oder "en". */
    fun language(ctx: Context): String = p(ctx).getString("lang", "de") ?: "de"
    fun setLanguage(ctx: Context, value: String) =
        p(ctx).edit().putString("lang", value).apply()
}
