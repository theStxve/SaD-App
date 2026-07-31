# ⌬ SAD - Stadt Als Dungeon

> **Status:** Underground Exploration Protocol [ACTIVE]  
> **Environment:** Real-World Urban Dungeon

**SAD (Stadt Als Dungeon)** ist ein Gamification-Experiment, das deine reale Umgebung in ein düsteres Cyberpunk-RPG verwandelt. Die App nutzt GPS-Daten, um echte Orte in "Dungeons" zu transformieren und lässt dich deine Stadt erkunden, wie du es noch nie zuvor getan hast.

---

## Kern-Features

- **Aktiver Radar:** Tracke deinen Standort in Echtzeit und entdecke verborgene Dungeons in deiner Nähe.
- **Fog of War:** Die Karte ist in Dunkelheit gehüllt. Nur dort, wo du physisch warst, wird die Welt dauerhaft enthüllt.
- **Dungeon-System:** Entdecke "Common", "Rare" und "Epic" Dungeons basierend auf realen OpenStreetMap-Daten.
- **P2P-Netzwerk (Gerüchte):** Triff andere Spieler in der echten Welt und tausche automatisch verschlüsselte Gerüchte über Fundorte via Google Nearby aus – ganz ohne zentralen Server.
- **Achievement-Protokoll:** Sammle Erfolge für nächtliche Erkundungen, das Teilen deines Fortschritts oder das Hacken des Systems.
- **Offline Modus:** Zur Erstbestimmung der Position beim Start der App wird Internet empfohlen, abseits davon läuft die App ohne Netz.
- **Level System:** Levelaufstiege mit dem Sammeln von XP und Freischalten von neuen Profiltiteln durchs Spielen.
- **Backup & Restore:** Spielstand als JSON-Datei exportieren und auf neuen Geräten wiederherstellen.

---

## Settings-Tab

Ab Version 1.0.2 gibt es einen dedizierten Einstellungs-Tab in der Navigation.

### Farbschema
Vier auswählbare App-Themes, die alle Screens und die Navigationsleiste einschließen:

| Theme | Beschreibung |
|---|---|
| **Legacy** | Dunkles Cyberpunk-Design mit Neon-Cyan und -Pink (Standard) |
| **Light** | Helles Tages-Design mit klarer, lesbarer Karte |
| **Inferno** | Dunkles Design mit Neon-Orange und Rot |
| **Matrix** | Terminal-Look mit Neon-Grün auf Schwarz |

Das Light-Theme deaktiviert den Karten-Invertierungsfilter automatisch und zeigt normale helle OpenStreetMap-Kacheln.

### Karten-Anpassung & Erkundung
Die Kartenanzeige lässt sich unabhängig vom Theme konfigurieren:

- **Präzisionsmodus** – Kleinere Erkundungskreise (20m) für exakte Pfade (Toggle)
- **Verbindungsmodus** – Deckt die direkte Linie zwischen gemessenen GPS-Punkten auf (Toggle)
- **Dark-Mode Invertierung** – Karte hell/dunkel umkehren (Toggle)
- **Kontrast & Helligkeit** – Stärke der Straßen- und Gebäudehervorhebung
- **Nebel-Deckkraft** – Transparenz des Fog-of-War-Overlays (20% – 100%)

### Addons (Dungeon-Packs)
- Importiere eigene `.db`-Dateien aus anderen Städten/Regionen als Addon
- Werden nahtlos in die Hauptkarte integriert und können einzeln verwaltet/gelöscht werden

- **Reset** – Alle Kartenwerte auf Standard zurücksetzen

Alle Werte werden persistent gespeichert und sofort auf die Karte angewendet, ohne Neustart.

### Datensicherung
- **Exportieren:** Spielstand (XP, Level, Dungeons, erkundete Sektoren, Achievements) als `.json`-Datei sichern
- **Importieren:** Gespeicherten Stand aus einer `.json`-Datei wiederherstellen

### Teilen
Spielfortschritt als formatierten Text-Log über die Android-Share-Funktion teilen.

---

## Tech-Stack

- **Sprache:** Kotlin
- **UI:** Jetpack Compose
- **Datenbank:** Room SQLite (Lokale Persistenz)
- **Karten:** OSMDroid (OpenStreetMap Integration)
- **Kommunikation:** Google Nearby Connections (Dezentraler Datenabgleich)
- **Hintergrund-Logik:** Android Foreground Services für dauerhaftes Tracking und Aufdecken der Karte

---

## Disclaimer (Haftungsausschluss)

SAD ist ein Spiel für Entdecker. Dennoch gilt:
1. **Betreten auf eigene Gefahr:** Betrete niemals Privatgelände oder gefährliche Areale (Lost Places) ohne Erlaubnis.
2. **Sicherheit geht vor:** Achte auf deine Umgebung, besonders bei nächtlichen Erkundungen.
3. **Haftung:** Der Entwickler übernimmt keinerlei Haftung für Schäden, Unfälle oder rechtliche Konsequenzen, die durch die Nutzung der App entstehen.
4. GPS-Daten werden nur zum Bestimmen der Freischaltpositionen für die Dungeons und den Nebel genutzt. Die App funktioniert auch offline.

---

## Map Data Pipeline (Dungeon-Schmiede)

Wir behandeln die ganze Stadt als einen einzigen großen Dungeon. Um deine Region zu importieren, nutze die automatisierte Pipeline im `scripts/`-Ordner.

> **Voraussetzung:** [Python 3.9+](https://www.python.org/downloads/) muss installiert sein ("Add Python to PATH" beim Setup ankreuzen).

### 1. Karte herunterladen
Lade eine `.osm.pbf` Datei für deine Region (z.B. ein Bundesland) von [Geofabrik](https://download.geofabrik.de/europe/germany.html) herunter und lege sie als `map.osm.pbf` in den `scripts/`-Ordner.

### 2. Schmieden (Windows)
Doppelklick auf:
```
scripts/process_map.bat
```
Das Skript installiert alle Abhängigkeiten automatisch (`pip install osmium`) und erstellt die Datenbank.

### 2. Schmieden (Linux / macOS)
```bash
pip install osmium
python scripts/process_map.py
```

**Was passiert dabei?**
- **Extraktion:** Ruinen, Burgen, Bunker, verlassene Orte, alte Bahnstrecken, Cafés, Museen v.v.m. werden aus der OSM-Karte gefiltert.
- **Rarity-Zuweisung:** Jeder Ort bekommt automatisch eine Seltenheitsstufe (`uncommon` / `rare` / `epic`).
- **Datenbank:** `app/src/main/assets/places.db` wird direkt erstellt – kein manueller Kopiervorgang nötig.

---

## Installation & Setup

1. Repository klonen
2. Kartendatei herunterladen (siehe oben) → `scripts/map.osm.pbf`
3. `scripts/process_map.bat` ausführen (oder `python scripts/process_map.py`)
4. Projekt in Android Studio öffnen und auf das Gerät flashen

---

*"Die Stadt ist kein Ort, sie ist ein Dungeon. Geh raus und erobere sie."*
