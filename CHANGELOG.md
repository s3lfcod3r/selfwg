# 📰 Changelog

Newest version first. Full release notes are on [GitHub Releases](https://github.com/s3lfcod3r/selfwg/releases).
Neueste Version zuerst. Die ausführlichen Release-Notes stehen unter [GitHub Releases](https://github.com/s3lfcod3r/selfwg/releases).

---

## 1.2.5 · Build 8 · September 2026
- 🇬🇧 Fix: the Doze alarm restarts the **60 s watchdog loop** when the tunnel should be up (before, after a process restart it only ran every 15 minutes); `keystore.properties.example` corrected. Not tested on a device.
- 🇩🇪 Fix: Der Doze-Alarm startet die **60-s-Wächter-Schleife** wieder, wenn der Tunnel an sein soll (vorher lief sie nach einem Prozess-Neustart nur alle 15 Minuten); `keystore.properties.example` korrigiert. Nicht auf einem Gerät getestet.

## 1.2.4 · Build 7 · September 2026 🔒
- 🇬🇧 Security update: the **app lock** stays locked if no device lock / fingerprint is set up; release builds without `keystore.properties` fail instead of using the debug key. New **"connection failed"** display with error type (no config contents). Not tested on a device.
- 🇩🇪 Sicherheitsupdate: Die **App-Sperre** bleibt zu, wenn keine Gerätesperre / kein Fingerabdruck eingerichtet ist; Release-Builds ohne `keystore.properties` brechen ab, statt den Debug-Schlüssel zu nehmen. Neue Anzeige **„Verbindung fehlgeschlagen“** mit Fehlerart (keine Config-Inhalte). Nicht auf einem Gerät getestet.

## 1.2.3 · Build 6 · July 2026
- 🇬🇧 Reliability: permanent **watchdog service** (foreground type fixed, survives long-term use), reconnect with backoff + status display, protection against hanging connection calls, private key masked in the editor.
- 🇩🇪 Zuverlässigkeit: dauerhafter **Wächter-Dienst** (Vordergrund-Typ korrigiert, übersteht den Dauerbetrieb), Neuverbinden mit Wartezeit + Statusanzeige, Schutz gegen hängende Verbindungsaufrufe, privater Schlüssel im Editor verdeckt.

## 1.2.2 · June 2026
- 🇬🇧 German umlauts in the app UI fixed (ue/oe/ae/ss → ä/ö/ü/ß).
- 🇩🇪 Deutsche Umlaute in der App-Oberfläche korrigiert (ue/oe/ae/ss → ä/ö/ü/ß).

## 1.2.1 · June 2026
- 🇬🇧 First release: standalone WireGuard client with **auto-reconnect** on ISP IP change, multiple tunnels, per-app split tunnel, labeled editor, QR / file / paste import, fingerprint lock, DE / EN.
- 🇩🇪 Erste Version: eigenständiger WireGuard-Client mit **Auto-Reconnect** beim IP-Wechsel, mehreren Tunneln, Split-Tunnel pro App, Editor mit beschrifteten Feldern, Import per QR / Datei / Text, Fingerabdruck-Sperre, DE / EN.
