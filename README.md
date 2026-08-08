# ⌬ SAD - Stadt Als Dungeon

> **Status:** Underground Exploration Protocol [ACTIVE]  
> **Environment:** Real-World Urban Dungeon

**SAD (Stadt Als Dungeon)** ist ein Gamification-Experiment, das deine reale Umgebung in ein düsteres Cyberpunk-RPG verwandelt. Die App nutzt GPS-Daten, um echte Orte in "Dungeons" zu transformieren und lässt dich deine Stadt erkunden, wie du es noch nie zuvor getan hast.

---

## Kern-Features

- **Aktiver Radar:** Tracke deinen Standort in Echtzeit und entdecke verborgene Dungeons in deiner Nähe.
- **Fog of War:** Die Karte ist in Dunkelheit gehüllt. Nur dort, wo du physisch warst, wird die Welt dauerhaft enthüllt.
- **Dungeon-System:** Entdecke "Common", "Rare" und "Epic" Dungeons basierend auf realen OpenStreetMap-Daten.
- **P2P-Netzwerk (Gerüchte):** Triff andere Spieler in der echten Welt und tausche automatisch verschlüsselte Gerüchte über Fundorte via Google Nearby aus – mit "Auf Karte orten" und Löschen-Funktion.
- **Addon Hub & KI-Modding:** Eigener Tab für Community-Dungeon-Packs (JSON & DB), inklusive integriertem Modding-Guide und KI-Master-Prompt per Knopfdruck für ChatGPT/Claude/Gemini.
- **Custom Addon Styling:** Eigene Markerfarben (`iconColor`), Zoom-Sichtbarkeitsgrenzen (`minZoom`), Story-Lore, Quests und benutzerdefinierte XP per JSON.
- **App-Themes:** 4 visuelle Modi (Legacy Cyberpunk, Light, Inferno, Matrix) mit voller Farb-Anpassung aller Screens.
- **Achievement-Protokoll:** Sammle Erfolge für nächtliche Erkundungen, das Teilen deines Fortschritts oder das Hacken des Systems.
- **Offline Modus:** Zur Erstbestimmung der Position beim Start der App wird Internet empfohlen, abseits davon läuft die App ohne Netz.
- **Level System:** Levelaufstiege mit dem Sammeln von XP und Freischalten von neuen Profiltiteln durchs Spielen.
- **Backup & Restore:** Spielstand als JSON-Datei exportieren und auf neuen Geräten wiederherstellen.

---

## Addon Hub & Community-Modding

SAD besitzt einen eigenen **Addon Hub Tab** zur einfachen Verwaltung und Erstellung eigener Dungeon-Packs:

- **JSON & DB Import:** Unterstützt sowohl `.json` Addon-Packs als auch `.db` Datenbank-Dateien.
- **Pack-Verwaltung:** Einzelne Packs per Toggle aktivieren/deaktivieren oder löschen.
- **Creator Exporter:** Alle in der App entdeckten Dungeons als wiederverwendbares JSON-Addon für Freunde exportieren.
- **Interaktiver Modding Guide:** Einklappbare Sektionen erklären Pflichtfelder, Raritäten, Lore/Story, Gameplay-Attribute und Best Practices.
- **KI-Master-Prompt Button:** Kopiert per Knopfdruck einen vollständigen Prompt in die Zwischenablage. KI-Tools wie ChatGPT, Claude oder Gemini können so sofort funktionierende JSON-Packs für jede beliebige Stadt generieren.
- **Erweiterte Addon-Attribute:**
  - `iconColor`: Hex-Farbe für den Kartenmarker (z.B. `#FF8C00`)
  - `minZoom`: Erst ab diesem Zoom-Level sichtbar (z.B. `14.0`)
  - `description`, `lore`, `questHint`, `xpReward`

---

## Settings & Personalisierung

- **Design-Themes:** Auswählbar zwischen *Legacy*, *Light*, *Inferno* und *Matrix* – alle Farben und Kartenelemente passen sich nahtlos an.
- **Tab-Sichtbarkeit:** Ausblenden/Einblenden einzelner Bottom-Bar-Tabs (Quests, Gerüchte, Addon Hub, Erfolge).
- **Operative-Profilkarte:** Visuelles Cyberpunk-Sharing mit Tech-Corner Brackets, Glow-Effekten, Orbit-Ringen und "STATUS ONLINE"-Badge.
- **Karten-Anpassung:** Präzisionsmodus (20m-Radius), Verbindungsmodus (GPS-Linien), Nebel-Deckkraft, Kontrast/Helligkeit und Dark-Mode-Invertierung.
- **Datensicherung:** Export & Import des kompletten Spielstands als `.json`.

---

## Tech-Stack

- **Sprache:** Kotlin
- **UI:** Jetpack Compose
- **Datenbank:** Room SQLite (Lokale Persistency & RAM-Caching)
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

---

## Eigene Addons (Dungeon-Packs) erstellen

Du kannst Addons entweder per **JSON** (empfohlen & KI-gestützt) oder per **.db Pipeline** erstellen.

### Methode A: JSON-Addon (Empfohlen & KI-gestützt)
1. Öffne die SAD-App und navigiere zum **Addon Hub** Tab.
2. Tippe im Modding Guide auf **KI-PROMPT KOPIEREN**.
3. Füge den Prompt in ChatGPT, Claude oder Gemini ein und gib deine Wunschstadt an.
4. Speichere das generierte JSON als Datei ab und tippe im Addon Hub auf **IMPORTIEREN**.

### Methode B: Python .db Pipeline
1. Lade eine `.osm.pbf` von [Geofabrik](https://download.geofabrik.de/) herunter.
2. Führe aus: `python scripts/process_map.py scripts/deine_region.osm.pbf scripts/pack.db`
3. Übertrage `pack.db` auf dein Smartphone und importiere sie im Addon Hub.

---

## Installation & Setup

1. Repository klonen
2. Kartendatei herunterladen (siehe oben) → `scripts/map.osm.pbf`
3. `scripts/process_map.bat` ausführen (oder `python scripts/process_map.py`)
4. Projekt in Android Studio öffnen und auf das Gerät flashen

---

*"Die Stadt ist kein Ort, sie ist ein Dungeon. Geh raus und erobere sie."*

