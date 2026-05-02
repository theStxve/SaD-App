@echo off
setlocal enabledelayedexpansion

:: --- KONFIGURATION ---
set INPUT_PBF=map.osm.pbf
set FILTERED_PBF=dungeons_filtered.osm.pbf
set EXPORT_JSON=export.geojson
set PYTHON_SCRIPT=scripts/geojson_to_sqlite.py

echo [SAD DUNGEON FORGE] Starte Extraktion...

:: 1. PRUEFEN OB EINGABE EXISTIERT
if not exist "%INPUT_PBF%" (
    echo FEHLER: %INPUT_PBF% nicht gefunden! 
    echo Bitte lade eine .osm.pbf Datei herunter und nenne sie map.osm.pbf
    pause
    exit /b
)

:: 2. OSMIUM FILTERING
echo [1/3] Filter relevante Sektoren (Historisch, Versorgung, Tourismus)...
osmium tags-filter "%INPUT_PBF%" ^
    nwr/historic=ruins,castle,fort,monument ^
    nwr/abandoned=yes ^
    nwr/military=bunker ^
    nwr/amenity=cafe,pub,bar,restaurant ^
    nwr/tourism=museum,viewpoint,artwork ^
    -o "%FILTERED_PBF%" --overwrite

:: 3. GEOJSON EXPORT
echo [2/3] Konvertiere zu GeoJSON...
osmium export "%FILTERED_PBF%" -o "%EXPORT_JSON%" --overwrite

:: 4. SQLITE IMPORT
echo [3/3] Importiere in die App-Datenbank (places.db)...
python %PYTHON_SCRIPT%

:: 5. CLEANUP
echo [*] Aufraeumen...
del "%FILTERED_PBF%"
del "%EXPORT_JSON%"

echo.
echo [FERTIG] Die Dungeon-Datenbank wurde erfolgreich geschmiedet!
echo Du kannst die App jetzt in Android Studio neu bauen.
pause
