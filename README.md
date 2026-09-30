<div align="center">

<img src="assets/logo.png" width="240" alt="SelfWG logo" />


**Standalone WireGuard® client for Android with an auto-reconnect watchdog — survives the daily ISP IP change, so your tunnel just stays up.**

[![Download](https://img.shields.io/badge/download-latest%20APK-33A78C?logo=android&logoColor=white)](../../releases/latest)
![Version](https://img.shields.io/badge/version-1.2.4-33A78C)
![License](https://img.shields.io/badge/license-GPL--2.0-8A9CAA)
![Stack](https://img.shields.io/badge/stack-Kotlin%20%C2%B7%20Compose-43D3AD)
![Platform](https://img.shields.io/badge/platform-Android%208%2B-9DBDD0)

[English](#english) · [Deutsch](#deutsch)

</div>

---

## English

**SelfWG** is a lightweight, standalone VPN client that speaks the WireGuard® protocol. It solves one concrete everyday problem: residential internet lines usually rotate their public IP once a day (forced reconnect). After that, the official WireGuard app does **not** re-resolve the server hostname on its own — the tunnel sits dead until you toggle it off and on by hand. SelfWG fixes exactly that.

### Features

- **Auto-reconnect watchdog** — a foreground service checks every 60 s (and via a Doze wake-up alarm every 15 min) whether the server IP changed, and only then rebuilds the tunnel with the fresh IP. While everything is fine it touches nothing.
- **Multiple tunnels / accounts** — store several configs, switch with one tap.
- **Per-app split tunnel** — include or exclude individual apps from the tunnel (`IncludedApplications` / `ExcludedApplications`).
- **Labeled editor** — edit each config field (private key, address, DNS, endpoint, allowed IPs …) with a short description of what it does — no raw text blob.
- **Easy import** — scan a WG-Easy QR code, load a `.conf` file, or paste the text.
- **Fingerprint lock** — optionally protect the app with fingerprint / device credential.
- **DE / EN** — German and English UI, switchable at runtime.
- **Encrypted storage** — configs (including the private key) are kept in `EncryptedSharedPreferences`.
- **Self brand** — dark theme, teal accent.

### Install

1. Download the latest `SelfWG-*.apk` from **[Releases](../../releases/latest)** (or install it from **SelfStore**).
2. Open it on your Android device and install (allow installation from unknown sources).
3. Google Play Protect may warn about a "harmful app" — for sideloaded apps this is a **false positive**; choose "Install anyway".

### Privacy

No telemetry, no ads, no servers other than your own. The config never leaves the device.

### Build

System Gradle + JDK 17/21 (no Android Studio required):

```bash
gradle assembleRelease
```

APK: `app/build/outputs/apk/release/app-release.apk`. Stack: Kotlin, Jetpack Compose, Material 3, `com.wireguard.android:tunnel` (bundles the native WireGuard core), `zxing-android-embedded` (QR). minSdk 26, targetSdk 34.

---

## Deutsch

**SelfWG** ist ein schlanker, eigenständiger VPN-Client, der das WireGuard®-Protokoll nutzt. Er löst ein konkretes Alltagsproblem: Private Internetanschlüsse wechseln ihre öffentliche IP meist einmal täglich (Zwangstrennung). Danach löst die offizielle WireGuard-App den Server-Namen **nicht** von selbst neu auf — der Tunnel bleibt tot, bis man ihn von Hand aus- und wieder einschaltet. Genau das behebt SelfWG.

### Funktionen

- **Auto-Reconnect-Wächter** — ein Vordergrund-Dienst prüft alle 60 s (und per Doze-Weckalarm alle 15 min), ob sich die Server-IP geändert hat, und baut nur dann den Tunnel mit der frischen IP neu auf. Solange alles läuft, fasst er nichts an.
- **Mehrere Tunnel / Konten** — mehrere Configs speichern, per Tippen umschalten.
- **App-Split-Tunnel** — einzelne Apps vom Tunnel ein- oder ausschließen (`IncludedApplications` / `ExcludedApplications`).
- **Editor mit beschrifteten Feldern** — jedes Feld (Privater Schlüssel, Adresse, DNS, Endpunkt, Erlaubte IPs …) mit Kurzerklärung statt Roh-Textblock.
- **Einfacher Import** — WG-Easy-QR-Code scannen, `.conf`-Datei laden oder Text einfügen.
- **Fingerabdruck-Sperre** — die App optional per Fingerabdruck / Gerätesperre schützen.
- **DE / EN** — deutsche und englische Oberfläche, zur Laufzeit umschaltbar.
- **Verschlüsselter Speicher** — Configs (inkl. privatem Schlüssel) liegen in `EncryptedSharedPreferences`.
- **Self-Look** — dunkles Theme, Teal-Akzent.

### Installation

1. Aktuelles `SelfWG-*.apk` unter **[Releases](../../releases/latest)** herunterladen (oder aus dem **SelfStore** installieren).
2. Auf dem Android-Gerät öffnen und installieren (Installation aus unbekannten Quellen erlauben).
3. Google Play Protect meldet evtl. „Schädliche App" — bei sideloaded Apps ein **Fehlalarm**; mit „Trotzdem installieren" fortfahren.

### Privatsphäre

Keine Telemetrie, keine Werbung, keine Server außer deinem eigenen. Die Config verlässt das Gerät nie.

### Build

System-Gradle + JDK 17/21 (kein Android Studio nötig):

```bash
gradle assembleRelease
```

APK: `app/build/outputs/apk/release/app-release.apk`. Stack: Kotlin, Jetpack Compose, Material 3, `com.wireguard.android:tunnel` (bringt den nativen WireGuard-Kern mit), `zxing-android-embedded` (QR). minSdk 26, targetSdk 34.

---

## License

GPL-2.0 — this project uses the WireGuard® library and is therefore licensed under **GPL-2.0**. "WireGuard" is a registered trademark of Jason A. Donenfeld. SelfWG is an independent project and is not affiliated with the WireGuard project.
