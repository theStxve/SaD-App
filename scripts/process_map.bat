@echo off
setlocal enabledelayedexpansion
chcp 65001 >nul

echo.
echo  ╔══════════════════════════════════════════╗
echo  ║        SAD DUNGEON FORGE                 ║
echo  ║   Datenbank aus OSM-Karte erstellen      ║
echo  ╚══════════════════════════════════════════╝
echo.

:: --- SCHRITT 1: Python prüfen ---
python --version >nul 2>&1
if errorlevel 1 (
    echo  FEHLER: Python nicht gefunden!
    echo  Bitte installiere Python 3.9+ von https://www.python.org/downloads/
    echo  Beim Installieren "Add Python to PATH" ankreuzen!
    pause
    exit /b 1
)
echo  [OK] Python gefunden.

:: --- SCHRITT 2: osmium (pyosmium) installieren ---
echo  [1/3] Installiere Abhaengigkeiten (osmium)...
pip install osmium --quiet
if errorlevel 1 (
    echo  FEHLER: pip install osmium fehlgeschlagen.
    echo  Versuche: pip install osmium --user
    pause
    exit /b 1
)
echo  [OK] osmium bereit.

:: --- SCHRITT 3: PBF-Datei prüfen ---
echo  [2/3] Suche nach Kartendatei...

:: Datei im scripts/-Ordner?
if exist "%~dp0map.osm.pbf" (
    set INPUT_PBF=%~dp0map.osm.pbf
    goto :found
)

:: Datei im Elternordner (SAD_App/)?
if exist "%~dp0..\map.osm.pbf" (
    set INPUT_PBF=%~dp0..\map.osm.pbf
    goto :found
)

echo.
echo  FEHLER: Keine map.osm.pbf gefunden!
echo.
echo  Bitte lade eine Kartendatei herunter und lege sie als
echo  "map.osm.pbf" in diesen Ordner:
echo  %~dp0
echo.
echo  Download fuer Deutschland (Bundeslaender):
echo  https://download.geofabrik.de/europe/germany.html
echo.
pause
exit /b 1

:found
echo  [OK] Karte gefunden: !INPUT_PBF!

:: --- SCHRITT 4: Python-Skript ausführen ---
echo  [3/3] Starte Extraktion...
echo.
python "%~dp0process_map.py" "!INPUT_PBF!"

if errorlevel 1 (
    echo.
    echo  FEHLER: Das Skript ist mit einem Fehler beendet worden.
    pause
    exit /b 1
)

echo.
echo  Fertig! Rebuild die App in Android Studio.
pause
