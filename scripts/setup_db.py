import sqlite3
import os

def setup_database():
    db_path = "app/src/main/assets/places.db"
    
    # Sicherstellen, dass der Ordner existiert
    os.makedirs(os.path.dirname(db_path), exist_ok=True)
    
    print(f"Erstelle Datenbank unter: {db_path}...")
    conn = sqlite3.connect(db_path)
    c = conn.cursor()
    
    # Die exakte Struktur, die PlaceEntity.kt erwartet
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
    
    # Beispiel-Eintrag (angepasst an Entity)
    c.execute('''
        INSERT OR IGNORE INTO places (osm_id, name, category, type, rarity, lat, lon)
        VALUES ('12345', 'Beispiel Dungeon', 'POI', 'ruins', 'rare', 52.5200, 13.4050)
    ''')
    
    conn.commit()
    conn.close()
    print("Fertig! Die Datenbank wurde mit der korrekten Struktur erstellt.")
    print("Tipp: Nutze Overpass API Exports (OSM), um diese Tabelle mit echten Orten zu füllen.")

if __name__ == "__main__":
    setup_database()
