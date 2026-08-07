package com.sad.app.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class TabVisibility(
    val showQuests: Boolean = true,
    val showRumors: Boolean = true,
    val showAddons: Boolean = true,
    val showAchievements: Boolean = true
)

object TabVisibilityManager {
    private const val PREFS_KEY = "tab_visibility_prefs"

    var current by mutableStateOf(TabVisibility())
        private set

    fun init(context: Context) {
        current = load(context)
    }

    fun save(context: Context, visibility: TabVisibility) {
        context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE).edit()
            .putBoolean("showQuests", visibility.showQuests)
            .putBoolean("showRumors", visibility.showRumors)
            .putBoolean("showAddons", visibility.showAddons)
            .putBoolean("showAchievements", visibility.showAchievements)
            .apply()
        current = visibility
    }

    private fun load(context: Context): TabVisibility {
        val prefs = context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)
        return TabVisibility(
            showQuests = prefs.getBoolean("showQuests", true),
            showRumors = prefs.getBoolean("showRumors", true),
            showAddons = prefs.getBoolean("showAddons", true),
            showAchievements = prefs.getBoolean("showAchievements", true)
        )
    }
}
