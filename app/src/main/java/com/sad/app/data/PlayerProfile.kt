package com.sad.app.data

import android.content.Context
import android.content.SharedPreferences

data class PlayerProfile(
    val xp: Int,
    val level: Int,
    val exploredCount: Int,
    val visitedDungeons: Int,
    val nightExploredCount: Int,
    val morningExploredCount: Int,
    val title: String,
    val unlockedAchievements: Set<String>
) {
    companion object {
        fun load(context: Context): PlayerProfile {
            val prefs = context.getSharedPreferences("player_profile", Context.MODE_PRIVATE)
            val xp = prefs.getInt("xp", 0)
            val explored = prefs.getInt("explored_count", 0)
            val dungeons = prefs.getInt("visited_dungeons", 0)
            val nightExplored = prefs.getInt("night_explored_count", 0)
            val morningExplored = prefs.getInt("morning_explored_count", 0)
            val level = (xp / 500) + 1
            val title = when {
                level >= 500 -> "Gott-Status"
                level >= 100 -> "Mythos"
                level >= 50  -> "Halbgott"
                level >= 20  -> "Stadtlegende"
                level >= 10  -> "Dungeon-Meister"
                level >= 5   -> "Erkunder"
                level >= 2   -> "Wanderer"
                else         -> "Neuling"
            }
            val unlockedAchievements = prefs.getStringSet("unlocked_achievements", emptySet()) ?: emptySet()
            return PlayerProfile(xp, level, explored, dungeons, nightExplored, morningExplored, title, unlockedAchievements)
        }
        
        fun unlockAchievement(context: Context, id: String) {
            val prefs = context.getSharedPreferences("player_profile", Context.MODE_PRIVATE)
            val current = prefs.getStringSet("unlocked_achievements", emptySet()) ?: emptySet()
            if (!current.contains(id)) {
                val newSet = current.toMutableSet().apply { add(id) }
                prefs.edit().putStringSet("unlocked_achievements", newSet).apply()
                addXP(context, 100)
            }
        }

        fun addXP(context: Context, amount: Int) {
            val prefs = context.getSharedPreferences("player_profile", Context.MODE_PRIVATE)
            val current = prefs.getInt("xp", 0)
            prefs.edit().putInt("xp", current + amount).apply()
        }

        fun incrementExplored(context: Context) {
            val prefs = context.getSharedPreferences("player_profile", Context.MODE_PRIVATE)
            val current = prefs.getInt("explored_count", 0)
            prefs.edit().putInt("explored_count", current + 1).apply()
            
            val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            if (hour >= 23 || hour < 4) {
                val night = prefs.getInt("night_explored_count", 0)
                prefs.edit().putInt("night_explored_count", night + 1).apply()
            } else if (hour in 5..8) {
                val morning = prefs.getInt("morning_explored_count", 0)
                prefs.edit().putInt("morning_explored_count", morning + 1).apply()
            }

            addXP(context, 10)
        }

        fun incrementDungeons(context: Context) {
            val prefs = context.getSharedPreferences("player_profile", Context.MODE_PRIVATE)
            val current = prefs.getInt("visited_dungeons", 0)
            prefs.edit().putInt("visited_dungeons", current + 1).apply()
            addXP(context, 50)
        }
    }
}
