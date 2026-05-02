# ⌬ SAD - Stadt Als Dungeon

> **Status:** Underground Exploration Protocol [ACTIVE]  
> **Environment:** Real-World Urban Dungeon

**SAD (Stadt Als Dungeon)** ist ein Gamification-Experiment, das deine reale Umgebung in ein düsteres Cyberpunk-RPG verwandelt. Die App nutzt GPS-Daten, um echte Orte in "Dungeons" zu transformieren und lässt dich deine Stadt erkunden, wie du es noch nie zuvor getan hast.

---

## ⚡ Kern-Features

- **📡 Aktiver Radar:** Tracke deinen Standort in Echtzeit und entdecke verborgene Dungeons in deiner Nähe.
- **🌫 Fog of War:** Die Karte ist in Dunkelheit gehüllt. Nur dort, wo du physisch warst, wird die Welt dauerhaft enthüllt.
- **🗝 Dungeon-System:** Entdecke "Common", "Rare" und "Epic" Dungeons basierend auf realen OpenStreetMap-Daten.
- **🕸 P2P-Netzwerk (Gerüchte):** Triff andere Spieler in der echten Welt! Tausche automatisch verschlüsselte Gerüchte über Fundorte via Google Nearby (P2P) aus – ganz ohne zentralen Server.
- **🏆 Achievement-Protokoll:** Sammle Erfolge für nächtliche Erkundungen, das Teilen deines Fortschritts oder das Hacken des Systems.
- **🛜 Offline Modus** Zur Erstbestimmung der Position beim Start der App wird Internet empfohlen, abseits davon läuft die App ohne Netz.
- **⬆️ Level System** Levelaufstiege mit dem Sammeln von XP und Freischalten von neuen Profiltiteln durchs Spielen!

---

## 🛠 Tech-Stack

- **Sprache:** Kotlin
- **UI:** Jetpack Compose (Modernes Cyberpunk-Design)
- **Datenbank:** Room SQLite (Lokale Persistenz)
- **Karten:** OSMDroid (OpenStreetMap Integration)
- **Kommunikation:** Google Nearby Connections (Dezentraler Datenabgleich)
- **Hintergrund-Logik:** Android Foreground Services für dauerhaftes Tracking und Aufdecken der Karte

---

## ⚠️ Disclaimer (Haftungsausschluss)

SAD ist ein Spiel für Entdecker. Dennoch gilt:
1. **Betreten auf eigene Gefahr:** Betrete niemals Privatgelände oder gefährliche Areale (Lost Places) ohne Erlaubnis.
2. **Sicherheit geht vor:** Achte auf deine Umgebung, besonders bei nächtlichen Erkundungen.
3. **Haftung:** Der Entwickler übernimmt keinerlei Haftung für Schäden, Unfälle oder rechtliche Konsequenzen, die durch die Nutzung der App entstehen.
4. GPS Daten werden nur zum Bestimmen der Freischaltpositionen für die Dungeons & den Nebel genutzt, die App funktioniert auch offline.

---

## 🗺 Map Data Pipeline (Dungeon-Schmiede)

Wir behandeln die ganze Stadt als einen einzigen großen Dungeon. Um deine Region (z.B. Berlin, NRW) zu importieren, nutze die automatisierte Pipeline:

### 1. Vorbereitung
1. Installiere [Osmium Tool](https://osmcode.org/osmium-tool/) (für Windows via `choco install osmium-tool` oder manuellem Download).
2. Lade eine `.osm.pbf` Datei (z.B. dein Bundesland) von [Geofabrik](https://download.geofabrik.de/) herunter.
3. Benenne die Datei in `map.osm.pbf` um und lege sie in den Projekt-Hauptordner.

### 2. Schmieden
Führe einfach die Batch-Datei aus:
```bash
process_map.bat
```

**Was passiert dabei?**
- **Extraktion:** Alle Ruinen, Burgen, Bunker, aber auch Cafés und Museen werden als Sektoren extrahiert.
- **Konvertierung:** Die Daten werden für die App aufbereitet.
- **Datenbank:** Die `app/src/main/assets/places.db` wird automatisch erstellt/aktualisiert.

---

## 🏗 Installation & Setup
1. Repository klonen.
2. Dungeon-Schmiede ausführen (siehe oben).
3. In Android Studio öffnen und auf das Gerät flashen.


---

*“Die Stadt ist kein Ort, sie ist ein Dungeon. Geh raus und erobere sie.”*
