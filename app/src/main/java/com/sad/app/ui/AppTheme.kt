package com.sad.app.ui

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

// ─── Alle Farben die ein Theme definiert ─────────────────────────────────────
data class AppColors(
    val bg: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val primary: Color,     // Haupt-Akzentfarbe (Cyan / Blau / Orange / Grün)
    val accent: Color,      // Sekundär-Akzent (Pink / Magenta / Rot / Dunkelgrün)
    val gold: Color,        // Für Achievements / Legendary
    val textPrimary: Color,
    val textSecondary: Color,
    val isDark: Boolean,    // Bestimmt Statusbar-Farbe und Karten-Filter
    val fogColor: Color = Color(0xFF0A0A14)
)

// ─── Theme Enum ───────────────────────────────────────────────────────────────
enum class AppTheme(val displayName: String, val colors: AppColors) {

    LEGACY("Legacy", AppColors(
        bg             = Color(0xFF0A0A12),
        surface        = Color(0xFF111122),
        surfaceVariant = Color(0xFF1A1A2E),
        primary        = Color(0xFF00F3FF),
        accent         = Color(0xFFFF00E6),
        gold           = Color(0xFFFFD700),
        textPrimary    = Color.White,
        textSecondary  = Color(0xFF8899AA),
        isDark         = true,
        fogColor       = Color(0xFF0A0A14),
    )),

    LIGHT("Light", AppColors(
        bg             = Color(0xFFF0F4FF),
        surface        = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFE4EAF8),
        primary        = Color(0xFF0066DD),
        accent         = Color(0xFFCC0066),
        gold           = Color(0xFFB8860B),
        textPrimary    = Color(0xFF0D0D1A),
        textSecondary  = Color(0xFF556080),
        isDark         = false,
        fogColor       = Color(0xFFD0D7E5),
    )),

    INFERNO("Inferno", AppColors(
        bg             = Color(0xFF120600),
        surface        = Color(0xFF1E0C00),
        surfaceVariant = Color(0xFF2E1500),
        primary        = Color(0xFFFF6200),
        accent         = Color(0xFFFF1A00),
        gold           = Color(0xFFFFCC00),
        textPrimary    = Color.White,
        textSecondary  = Color(0xFFAA7755),
        isDark         = true,
        fogColor       = Color(0xFF120600),
    )),

    MATRIX("Matrix", AppColors(
        bg             = Color(0xFF000D00),
        surface        = Color(0xFF001400),
        surfaceVariant = Color(0xFF002200),
        primary        = Color(0xFF00FF41),
        accent         = Color(0xFF00AA2A),
        gold           = Color(0xFF88FF00),
        textPrimary    = Color(0xFFCCFFCC),
        textSecondary  = Color(0xFF448844),
        isDark         = true,
        fogColor       = Color(0xFF000D00),
    ));
}

// ─── CompositionLocal – wird in SADApp bereitgestellt ─────────────────────────
val LocalAppColors = compositionLocalOf { AppTheme.LEGACY.colors }

// ─── ThemeManager – Speichert/Lädt das gewählte Theme ────────────────────────
object ThemeManager {
    private const val PREFS_KEY = "app_settings"
    private const val THEME_KEY = "selected_theme"

    fun save(context: Context, theme: AppTheme) {
        context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)
            .edit().putString(THEME_KEY, theme.name).apply()
    }

    fun load(context: Context): AppTheme {
        val name = context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)
            .getString(THEME_KEY, AppTheme.LEGACY.name)
        return AppTheme.entries.firstOrNull { it.name == name } ?: AppTheme.LEGACY
    }
}
