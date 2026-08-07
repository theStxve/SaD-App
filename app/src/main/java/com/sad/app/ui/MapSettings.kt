package com.sad.app.ui

import android.content.Context
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class MapSettings(
    val contrast: Float = 1.2f,             // 0.5f .. 2.5f
    val brightness: Float = 0f,             // -100f .. 100f
    val isInverted: Boolean = true,         // Dark Mode Invert
    val fogOpacity: Float = 0.85f,           // 0.2f .. 1.0f
    val precisionModeEnabled: Boolean = false, // 20m Radius (Präzisionsmode) vs 150m Standard
    val connectionModeEnabled: Boolean = false, // Aufdecken der Verbindungslinie zwischen Punkten
    val rememberedZoom: Float = 17f,          // Gespeicherte Zoom-Stufe
    val showVisitedDungeonsGlobally: Boolean = false, // Erkundete (graue) Dungeons weltweit auf Karte anzeigen
    val forcePrecisionPaths: Boolean = false  // Alle Pfade als Präzisionspfade rendern und Lücken glätten
) {
    val visionRadiusMeters: Double
        get() = if (precisionModeEnabled) 20.0 else 150.0
}

object MapSettingsManager {
    private const val PREFS_KEY = "map_customization_prefs"

    // Globaler reaktiver State – SettingsScreen schreibt, MapScreen liest
    var current by mutableStateOf(MapSettings())
        private set

    fun init(context: Context) {
        current = load(context)
    }

    fun save(context: Context, settings: MapSettings) {
        context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE).edit()
            .putFloat("contrast", settings.contrast)
            .putFloat("brightness", settings.brightness)
            .putBoolean("isInverted", settings.isInverted)
            .putFloat("fogOpacity", settings.fogOpacity)
            .putBoolean("precisionModeEnabled", settings.precisionModeEnabled)
            .putBoolean("connectionModeEnabled", settings.connectionModeEnabled)
            .putFloat("rememberedZoom", settings.rememberedZoom)
            .putBoolean("showVisitedDungeonsGlobally", settings.showVisitedDungeonsGlobally)
            .putBoolean("forcePrecisionPaths", settings.forcePrecisionPaths)
            .apply()
        current = settings  // Reaktives Update → MapScreen sieht es sofort
    }

    private fun load(context: Context): MapSettings {
        val prefs = context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)
        return MapSettings(
            contrast = prefs.getFloat("contrast", 1.2f),
            brightness = prefs.getFloat("brightness", 0f),
            isInverted = prefs.getBoolean("isInverted", true),
            fogOpacity = prefs.getFloat("fogOpacity", 0.85f),
            precisionModeEnabled = prefs.getBoolean("precisionModeEnabled", false),
            connectionModeEnabled = prefs.getBoolean("connectionModeEnabled", false),
            rememberedZoom = prefs.getFloat("rememberedZoom", 17f),
            showVisitedDungeonsGlobally = prefs.getBoolean("showVisitedDungeonsGlobally", false),
            forcePrecisionPaths = prefs.getBoolean("forcePrecisionPaths", false)
        )
    }

    fun buildColorFilter(settings: MapSettings): ColorMatrixColorFilter? {
        // Light-Mode ohne Invertierung und Default-Kontrast → kein Filter nötig
        if (!settings.isInverted && settings.contrast == 1.0f && settings.brightness == 0f) return null

        val cm = ColorMatrix()

        // 1. Kontrast & Helligkeit
        val c = settings.contrast
        val b = settings.brightness
        val t = (1.0f - c) / 2.0f * 255.0f + b
        cm.set(floatArrayOf(
            c,  0f, 0f, 0f, t,
            0f,  c, 0f, 0f, t,
            0f, 0f,  c, 0f, t,
            0f, 0f, 0f, 1f, 0f
        ))

        // 2. Invertieren für Dark Mode
        if (settings.isInverted) {
            cm.postConcat(ColorMatrix(floatArrayOf(
                -1f,  0f,  0f, 0f, 255f,
                 0f, -1f,  0f, 0f, 255f,
                 0f,  0f, -1f, 0f, 255f,
                 0f,  0f,  0f, 1f,   0f
            )))
        }
        return ColorMatrixColorFilter(cm)
    }
}
