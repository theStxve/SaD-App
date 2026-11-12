# Projekt-spezifische R8/ProGuard-Regeln.
# Minify ist aktuell deaktiviert (isMinifyEnabled = false). Falls es aktiviert wird:

# osmdroid lädt Tile-Provider teils per Reflection
-dontwarn org.osmdroid.**
-keep class org.osmdroid.** { *; }
