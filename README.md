<div align="center">

<img src="assets/logo.png" width="240" alt="SelfWG logo" />

**Standalone WireGuard® client for Android with an auto-reconnect watchdog — survives the daily ISP IP change, so your tunnel just stays up.**<br>
*Eigenständiger WireGuard®-Client für Android mit Auto-Reconnect-Wächter — übersteht die tägliche Zwangstrennung, dein Tunnel bleibt einfach oben.*

[![Download](https://img.shields.io/badge/download-latest%20APK-33A78C?logo=android&logoColor=white)](https://github.com/s3lfcod3r/selfwg/releases/latest)
![Version](https://img.shields.io/badge/version-1.2.5-33A78C)
[![License](https://img.shields.io/badge/license-All%20rights%20reserved-8A9CAA)](LICENSE)
![Stack](https://img.shields.io/badge/stack-Kotlin%20%C2%B7%20Compose-43D3AD)
![App](https://img.shields.io/badge/app-Android%208%2B-9DBDD0)

**[🇬🇧 English](#-english) · [🇩🇪 Deutsch](#-deutsch)**

📰 **What's new / Neuigkeiten** → [CHANGELOG.md](CHANGELOG.md)

</div>

---

<a id="-english"></a>

## 🇬🇧 English

**SelfWG** is a lightweight, standalone VPN client that speaks the WireGuard® protocol. It solves one concrete everyday problem: residential internet lines usually rotate their public IP once a day (forced reconnect). After that, the official WireGuard app does **not** re-resolve the server hostname on its own — the tunnel sits dead until you toggle it off and on by hand. SelfWG fixes exactly that. It is a client only — it does not run a VPN server.

Part of the **Self** family (SelfMailer, SelfStream, SelfArchiver, SelfDashboard …) — same design system, same style.

> **TL;DR** — auto-reconnect after IP change · multiple tunnels · per-app split tunnel · QR import · fingerprint lock · no telemetry

### 📑 Table of contents

[Features](#en-features) · [Architecture](#en-architecture) · [Quick start](#en-quick-start) · [Security](#en-security) · [Tech stack](#en-tech-stack) · [Development](#en-development) · [Roadmap](#en-roadmap)

<a id="en-features"></a>

### ✨ Features

**🔄 Connection**
- **Auto-reconnect watchdog** — a foreground service checks every 60 s (and via a Doze wake-up alarm every 15 min) whether the server IP changed, and only then rebuilds the tunnel with the fresh IP. While everything is fine it touches nothing.
- **Reconnect with backoff** and status display; on failure the app shows "connection failed" with the error type
- **Multiple tunnels / accounts** — store several configs, switch with one tap
- **Per-app split tunnel** — include or exclude individual apps (`IncludedApplications` / `ExcludedApplications`)

**📝 Configs**
- **Labeled editor** — every field (private key, address, DNS, endpoint, allowed IPs …) with a short description — no raw text blob; private key masked
- **Easy import** — scan a WG-Easy QR code, load a `.conf` file, or paste the text

**🔐 App**
- **Fingerprint lock** — optionally protect the app with fingerprint / device credential
- **DE / EN** — German and English UI, switchable at runtime
- **Self brand** — dark theme, teal accent

<a id="en-architecture"></a>

### 🧭 Architecture

```text
Foreground service (watchdog)
  │  every 60 s  +  Doze alarm every 15 min
  ▼
resolve server hostname ──▶ IP unchanged? ──▶ do nothing
  │
  └─ IP changed ──▶ rebuild tunnel with the new IP (backoff on failure)
                        │
                        ▼
              com.wireguard.android:tunnel (native WireGuard core)
```

<a id="en-quick-start"></a>

### 🚀 Quick start (install)

1. Download the latest `SelfWG-*.apk` from **[Releases](https://github.com/s3lfcod3r/selfwg/releases/latest)** (or install it from **SelfStore**).
2. Open it on your Android device and install (allow installation from unknown sources).
3. Import your config (QR code, `.conf` file or text) and switch the tunnel on.

> **⚠️ Play Protect:** Google Play Protect may warn about a "harmful app" — for sideloaded apps this is a **false positive**; choose "Install anyway".

<a id="en-security"></a>

### 🛡️ Security

- **No telemetry, no ads**, no servers other than your own — the config never leaves the device
- **Encrypted storage** — configs (including the private key) are kept in `EncryptedSharedPreferences`
- **App lock** stays locked if no device lock / fingerprint is set up (instead of unlocking without a check)
- Error messages show only the error type — never config contents or keys
- **Release builds are signed only with the release key** — without `keystore.properties` the build fails instead of silently using the debug key; the signing certificate stays the same across updates
- **Deliberate trade-offs** (documented): sideloaded APK outside Google Play (hence the Play Protect warning); `targetSdk 34` to avoid the forced edge-to-edge layout of Android 15

Security update history → [CHANGELOG.md](CHANGELOG.md) (entries marked 🔒).

<a id="en-tech-stack"></a>

### 🧱 Tech stack

| Part | Tech |
|---|---|
| App | Kotlin · Jetpack Compose · Material 3 |
| VPN | `com.wireguard.android:tunnel` (bundles the native WireGuard core) |
| QR | `zxing-android-embedded` |
| Platform | minSdk 26 (Android 8) · targetSdk 34 · compileSdk 35 |
| Release | GitHub Releases · SelfStore catalog sync |

<a id="en-development"></a>

### 🛠️ Development

System Gradle + JDK 17/21 (no Android Studio required). For a release build, copy `keystore.properties.example` to `keystore.properties` and fill in your signing key first.

```bash
gradle assembleRelease
```

APK: `app/build/outputs/apk/release/app-release.apk`. A new GitHub release triggers the SelfStore catalog sync ([`notify-selfstore.yml`](.github/workflows/notify-selfstore.yml)).

<a id="en-roadmap"></a>

### 🗺️ Roadmap

- [x] Auto-reconnect watchdog, multiple tunnels, split tunnel
- [x] QR / file / paste import, labeled editor
- [x] Fingerprint lock, DE / EN
- [x] Robust watchdog (foreground service type, timeouts, backoff)
- [x] Error display and strict app lock
- [ ] Test 1.2.4 / 1.2.5 changes on a real device

<p align="right"><a href="#-english">⬆ back to top</a></p>

---

<a id="-deutsch"></a>

## 🇩🇪 Deutsch

**SelfWG** ist ein schlanker, eigenständiger VPN-Client, der das WireGuard®-Protokoll nutzt. Er löst ein konkretes Alltagsproblem: Private Internetanschlüsse wechseln ihre öffentliche IP meist einmal täglich (Zwangstrennung). Danach löst die offizielle WireGuard-App den Server-Namen **nicht** von selbst neu auf — der Tunnel bleibt tot, bis man ihn von Hand aus- und wieder einschaltet. Genau das behebt SelfWG. Es ist nur ein Client — einen VPN-Server betreibt es nicht.

Teil der **Self**-Familie (SelfMailer, SelfStream, SelfArchiver, SelfDashboard …) — gleiches Design-System, gleicher Stil.

> **Kurz** — Auto-Reconnect nach IP-Wechsel · mehrere Tunnel · Split-Tunnel pro App · QR-Import · Fingerabdruck-Sperre · keine Telemetrie

### 📑 Inhaltsverzeichnis

[Funktionen](#de-funktionen) · [Architektur](#de-architektur) · [Schnellstart](#de-schnellstart) · [Sicherheit](#de-sicherheit) · [Tech-Stack](#de-tech-stack) · [Entwicklung](#de-entwicklung) · [Roadmap](#de-roadmap)

<a id="de-funktionen"></a>

### ✨ Funktionen

**🔄 Verbindung**
- **Auto-Reconnect-Wächter** — ein Vordergrund-Dienst prüft alle 60 s (und per Doze-Weckalarm alle 15 min), ob sich die Server-IP geändert hat, und baut nur dann den Tunnel mit der frischen IP neu auf. Solange alles läuft, fasst er nichts an.
- **Neuverbinden mit wachsender Wartezeit** und Statusanzeige; schlägt es fehl, zeigt die App „Verbindung fehlgeschlagen“ mit der Fehlerart
- **Mehrere Tunnel / Konten** — mehrere Configs speichern, per Tippen umschalten
- **App-Split-Tunnel** — einzelne Apps vom Tunnel ein- oder ausschließen (`IncludedApplications` / `ExcludedApplications`)

**📝 Configs**
- **Editor mit beschrifteten Feldern** — jedes Feld (privater Schlüssel, Adresse, DNS, Endpunkt, erlaubte IPs …) mit Kurzerklärung statt Roh-Textblock; privater Schlüssel verdeckt
- **Einfacher Import** — WG-Easy-QR-Code scannen, `.conf`-Datei laden oder Text einfügen

**🔐 App**
- **Fingerabdruck-Sperre** — die App optional per Fingerabdruck / Gerätesperre schützen
- **DE / EN** — deutsche und englische Oberfläche, zur Laufzeit umschaltbar
- **Self-Look** — dunkles Theme, Teal-Akzent

<a id="de-architektur"></a>

### 🧭 Architektur

```text
Vordergrund-Dienst (Wächter)
  │  alle 60 s  +  Doze-Alarm alle 15 min
  ▼
Server-Namen auflösen ──▶ IP gleich? ──▶ nichts tun
  │
  └─ IP geändert ──▶ Tunnel mit neuer IP aufbauen (bei Fehler mit Wartezeit)
                        │
                        ▼
              com.wireguard.android:tunnel (nativer WireGuard-Kern)
```

<a id="de-schnellstart"></a>

### 🚀 Schnellstart (Installation)

1. Aktuelles `SelfWG-*.apk` unter **[Releases](https://github.com/s3lfcod3r/selfwg/releases/latest)** herunterladen (oder aus dem **SelfStore** installieren).
2. Auf dem Android-Gerät öffnen und installieren (Installation aus unbekannten Quellen erlauben).
3. Config importieren (QR-Code, `.conf`-Datei oder Text) und den Tunnel einschalten.

> **⚠️ Play Protect:** Google Play Protect meldet evtl. „Schädliche App“ — bei Apps außerhalb des Play Stores ein **Fehlalarm**; mit „Trotzdem installieren“ fortfahren.

<a id="de-sicherheit"></a>

### 🛡️ Sicherheit

- **Keine Telemetrie, keine Werbung**, keine Server außer deinem eigenen — die Config verlässt das Gerät nie
- **Verschlüsselter Speicher** — Configs (inkl. privatem Schlüssel) liegen in `EncryptedSharedPreferences`
- **App-Sperre** bleibt zu, wenn keine Gerätesperre / kein Fingerabdruck eingerichtet ist (statt ohne Abfrage zu entsperren)
- Fehlermeldungen zeigen nur die Fehlerart — nie Config-Inhalte oder Schlüssel
- **Release-Builds werden nur mit dem Release-Schlüssel signiert** — ohne `keystore.properties` bricht der Build ab, statt still den Debug-Schlüssel zu nehmen; das Signaturzertifikat bleibt über Updates gleich
- **Bewusste Kompromisse** (dokumentiert): APK außerhalb von Google Play (daher die Play-Protect-Warnung); `targetSdk 34`, um das erzwungene Edge-to-Edge-Layout von Android 15 zu vermeiden

Verlauf der Sicherheitsupdates → [CHANGELOG.md](CHANGELOG.md) (Einträge mit 🔒).

<a id="de-tech-stack"></a>

### 🧱 Tech-Stack

| Teil | Technik |
|---|---|
| App | Kotlin · Jetpack Compose · Material 3 |
| VPN | `com.wireguard.android:tunnel` (bringt den nativen WireGuard-Kern mit) |
| QR | `zxing-android-embedded` |
| Plattform | minSdk 26 (Android 8) · targetSdk 34 · compileSdk 35 |
| Release | GitHub Releases · SelfStore-Katalog-Sync |

<a id="de-entwicklung"></a>

### 🛠️ Entwicklung

System-Gradle + JDK 17/21 (kein Android Studio nötig). Für einen Release-Build zuerst `keystore.properties.example` nach `keystore.properties` kopieren und deinen Signaturschlüssel eintragen.

```bash
gradle assembleRelease
```

APK: `app/build/outputs/apk/release/app-release.apk`. Ein neues GitHub-Release stößt den SelfStore-Katalog-Sync an ([`notify-selfstore.yml`](.github/workflows/notify-selfstore.yml)).

<a id="de-roadmap"></a>

### 🗺️ Roadmap

- [x] Auto-Reconnect-Wächter, mehrere Tunnel, Split-Tunnel
- [x] Import per QR / Datei / Text, Editor mit beschrifteten Feldern
- [x] Fingerabdruck-Sperre, DE / EN
- [x] Robuster Wächter (Vordergrund-Diensttyp, Timeouts, Wartezeit)
- [x] Fehleranzeige und strenge App-Sperre
- [ ] Änderungen aus 1.2.4 / 1.2.5 auf einem echten Gerät testen

<p align="right"><a href="#-deutsch">⬆ nach oben</a></p>

---

## ⚖️ Lizenz / License

**🇩🇪 Alle Rechte vorbehalten.** © 2026 s3lfcod3r / SelfCoder

Du darfst die Software **privat und nicht-kommerziell** installieren und benutzen (z. B. im Homelab, über Docker oder Unraid).

**Ohne schriftliche Erlaubnis verboten:** kommerzielle Nutzung · Kopieren · Verändern · Weitergeben · Einbauen in eigene Projekte (auch Closed Source) · Verkaufen.

Die WireGuard®-Tunnel-Bibliothek bleibt unter Apache-2.0. „WireGuard“ ist eine eingetragene Marke von Jason A. Donenfeld; SelfWG ist nicht mit dem WireGuard-Projekt verbunden.

Details: [LICENSE](LICENSE)

**🇬🇧 All rights reserved.** © 2026 s3lfcod3r / SelfCoder

You may install and use the software **privately and non-commercially** (e.g. in your home lab, via Docker or Unraid).

**Not permitted without written permission:** commercial use · copying · modifying · redistributing · incorporating into other projects (including closed source) · selling.

The WireGuard® tunnel library stays under Apache-2.0. "WireGuard" is a registered trademark of Jason A. Donenfeld; SelfWG is not affiliated with the WireGuard project.

Details: [LICENSE](LICENSE)

---

<div align="center">

**SelfWG** · part of the Self family · Teil der Self-Familie<br>
made with 🔐 for self-hosting

</div>
