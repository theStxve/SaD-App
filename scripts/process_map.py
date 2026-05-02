"""
SAD DUNGEON FORGE – Karten-Prozessor
=====================================
Liest eine OpenStreetMap .pbf-Datei und schreibt alle
gefundenen Dungeon-Orte direkt in die App-Datenbank.

Voraussetzung:
  pip install osmium

Aufruf (aus dem SAD_App-Ordner):
  python scripts/process_map.py

  Die Datei 'map.osm.pbf' muss im gleichen Verzeichnis
  wie dieses Skript liegen, ODER du gibst den Pfad an:
  python scripts/process_map.py /pfad/zur/datei.osm.pbf

Karte herunterladen:
  https://download.geofabrik.de/europe/germany/niedersachsen.html
"""

import osmium
import sqlite3
import os
import sys

# --- KONFIGURATION ---
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
DB_FILE    = os.path.join(SCRIPT_DIR, "..", "app", "src", "main", "assets", "places.db")

# Filter: Welche OSM-Tags gelten als Dungeons?
FILTER_TAGS = {
    "historic": ["ruins", "castle", "fort", "monument", "bunker",
                 "wayside_cross", "memorial", "archaeological_site",
                 "industrial", "railway_station"],
    "military": ["bunker", "barracks", "training_area", "danger_area",
                 "nuclear_explosion_site"],
    "railway":  ["abandoned", "disused"],
    "landuse":  ["military"],
    "building": ["ruins"],
    "amenity":  ["cafe", "pub", "bar", "restaurant"],
    "tourism":  ["museum", "viewpoint", "artwork", "attraction"],
    "leisure":  ["nature_reserve"],
}

# ──────────────────────────────────────────────
def matches_filter(tags: dict) -> bool:
    for key, values in FILTER_TAGS.items():
        if key in tags and tags[key] in values:
            return True
    # Generisch: abandoned/disused=yes auf beliebigen Tags
    if tags.get("abandoned") == "yes" or tags.get("disused") == "yes":
        return True
    return False


def get_rarity(tags: dict) -> str:
    # Epic: Burgen, Festungen, Bunker, Militärgelände
    if (tags.get("historic") in ["castle", "fort", "bunker"]
            or tags.get("military") in ["bunker", "barracks", "training_area"]
            or tags.get("landuse") == "military"):
        return "epic"
    # Rare: Ruinen, alte Schienen, verlassenes Zeug
    if (tags.get("historic") in ["ruins", "industrial", "railway_station"]
            or tags.get("ruins") == "yes"
            or tags.get("building") == "ruins"
            or tags.get("railway") in ["abandoned", "disused"]
            or tags.get("amenity") in ["pub", "bar"]
            or tags.get("abandoned") == "yes"
            or tags.get("disused") == "yes"):
        return "rare"
    return "uncommon"

# ──────────────────────────────────────────────
# Eingabedatei bestimmen
# ──────────────────────────────────────────────
if len(sys.argv) > 1:
    INPUT_PBF = sys.argv[1]
else:
    INPUT_PBF = os.path.join(SCRIPT_DIR, "map.osm.pbf")

print("[SAD DUNGEON FORGE] Starte Extraktion...")
print(f"Eingabe:   {INPUT_PBF}")
print(f"Datenbank: {DB_FILE}")

if not os.path.exists(INPUT_PBF):
    print(f"\nFEHLER: {INPUT_PBF} nicht gefunden!")
    print("Lege die .osm.pbf in den scripts/-Ordner oder übergib den Pfad als Argument.")
    print("Download: https://download.geofabrik.de/europe/germany/")
    sys.exit(1)

# ──────────────────────────────────────────────
# OSM-Daten einlesen
# ──────────────────────────────────────────────
print("\n[1/2] Lese und filtere PBF-Datei...")

features = []

class DungeonHandler(osmium.SimpleHandler):
    def process(self, osm_object, lat, lon):
        tags = {tag.k: tag.v for tag in osm_object.tags}
        if not matches_filter(tags):
            return

        features.append({
            "osm_id":   str(osm_object.id),
            "name":     tags.get("name", "Unbekannter Sektor"),
            "category": "Dungeon",
            "type":     tags.get("historic", tags.get("amenity", tags.get("tourism", "point"))),
            "rarity":   get_rarity(tags),
            "lat":      lat,
            "lon":      lon,
        })

    def node(self, n):
        if n.location.valid():
            self.process(n, n.location.lat, n.location.lon)

    def way(self, w):
        try:
            lats = [n.location.lat for n in w.nodes if n.location.valid()]
            lons = [n.location.lon for n in w.nodes if n.location.valid()]
            if lats:
                self.process(w, sum(lats) / len(lats), sum(lons) / len(lons))
        except Exception:
            pass

handler = DungeonHandler()
handler.apply_file(INPUT_PBF, locations=True, idx="flex_mem")

print(f"    → {len(features)} Dungeons gefunden")

# ──────────────────────────────────────────────
# SQLite importieren
# ──────────────────────────────────────────────
print("\n[2/2] Importiere in App-Datenbank...")
os.makedirs(os.path.dirname(DB_FILE), exist_ok=True)
conn = sqlite3.connect(DB_FILE)
c = conn.cursor()
c.execute("""
    CREATE TABLE IF NOT EXISTS places (
        osm_id   TEXT PRIMARY KEY,
        name     TEXT,
        category TEXT,
        type     TEXT,
        rarity   TEXT,
        lat      REAL,
        lon      REAL
    )
""")

c.executemany("""
    INSERT OR IGNORE INTO places (osm_id, name, category, type, rarity, lat, lon)
    VALUES (:osm_id, :name, :category, :type, :rarity, :lat, :lon)
""", features)

conn.commit()
conn.close()

print()
print(f"[FERTIG] {len(features)} Dungeons wurden in die Datenbank geschmiedet!")
print(f"Datei: {os.path.normpath(DB_FILE)}")
print("Rebuild die App in Android Studio.")
