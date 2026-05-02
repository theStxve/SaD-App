import json
import sqlite3
import os

# Konfiguration
GEOJSON_FILE = "export.geojson"
DB_FILE = "app/src/main/assets/places.db"

def import_geojson():
    if not os.path.exists(GEOJSON_FILE):
        print(f"Fehler: {GEOJSON_FILE} nicht gefunden!")
        print("Tipp: Erstelle das GeoJSON mit: osmium export map.pbf -o export.geojson")
        return

    conn = sqlite3.connect(DB_FILE)
    c = conn.cursor()

    # Tabelle sicherstellen (Exakt wie PlaceEntity.kt)
    c.execute('''
        CREATE TABLE IF NOT EXISTS places (
            osm_id TEXT PRIMARY KEY,
            name TEXT,
            category TEXT,
            type TEXT,
            rarity TEXT,
            lat REAL,
            lon REAL
        )
    ''')

    with open(GEOJSON_FILE, 'r', encoding='utf-8') as f:
        data = json.load(f)

    count = 0
    for feature in data['features']:
        props = feature.get('properties', {})
        geom = feature.get('geometry', {})
        
        if geom['type'] == 'Point':
            lon, lat = geom['coordinates']
        elif geom['type'] == 'Polygon':
            coords = geom['coordinates'][0]
            lon = sum(p[0] for p in coords) / len(coords)
            lat = sum(p[1] for p in coords) / len(coords)
        else:
            continue

        osm_id = str(props.get('@id', props.get('id', count)))
        name = props.get('name', 'Unbekannter Sektor')
        
        # Zurück zum sauberen Design: Alles ist ein "Dungeon" (POI)
        category = "Dungeon"
        osm_type = props.get('historic', props.get('amenity', props.get('tourism', 'point')))
        
        # Seltenheit bestimmt die Icon-Farbe (uncommon=grün, rare=gold, epic=pink)
        rarity = 'uncommon'
        if props.get('historic') in ['castle', 'fort', 'bunker']:
            rarity = 'epic'
        elif props.get('ruins') == 'yes' or props.get('historic') == 'ruins':
            rarity = 'rare'
        elif props.get('amenity') in ['pub', 'bar']:
            rarity = 'rare'
        elif props.get('abandoned') == 'yes':
            rarity = 'rare'

        c.execute('''
            INSERT OR IGNORE INTO places (osm_id, name, category, type, rarity, lat, lon)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        ''', (osm_id, name, category, osm_type, rarity, lat, lon))
        count += 1

    conn.commit()
    conn.close()
    print(f"Erfolg! {count} Orte wurden einheitlich in {DB_FILE} importiert.")

if __name__ == "__main__":
    import_geojson()
