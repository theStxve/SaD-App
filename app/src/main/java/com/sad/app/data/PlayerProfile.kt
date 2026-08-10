package com.sad.app.data

import android.content.Context
import android.content.SharedPreferences

data class PlayerProfile(
    val playerName: String,
    val xp: Int,
    val level: Int,
    val exploredCount: Int,
    val visitedDungeons: Int,
    val nightExploredCount: Int,
    val morningExploredCount: Int,
    val totalDistanceMeters: Float,
    val streakDays: Int,
    val title: String,
    val unlockedAchievements: Set<String>
) {
    val displayName: String
        get() = playerName.ifBlank { "Agent_$level" }

    val totalDistanceKm: Float
        get() = totalDistanceMeters / 1000f

    companion object {
        fun load(context: Context): PlayerProfile {
            val prefs = context.getSharedPreferences("player_profile", Context.MODE_PRIVATE)
            val playerName = prefs.getString("player_name", "") ?: ""
            val xp = prefs.getInt("xp", 0)
            val explored = prefs.getInt("explored_count", 0)
            val dungeons = prefs.getInt("visited_dungeons", 0)
            val nightExplored = prefs.getInt("night_explored_count", 0)
            val morningExplored = prefs.getInt("morning_explored_count", 0)

            // Rueckwirkende Distanz-Schaetzung (150m pro erkundetem Bereich + 1.2km pro Dungeon)
            val trackedMeters = prefs.getFloat("total_distance_meters", 0f)
            val estimatedMeters = (explored * 150f) + (dungeons * 1200f)
            val totalDistanceMeters = maxOf(trackedMeters, estimatedMeters)

            val streakDays = prefs.getInt("streak_days", if (dungeons > 0 || explored > 0) 1 else 0)

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
            return PlayerProfile(playerName, xp, level, explored, dungeons, nightExplored, morningExplored, totalDistanceMeters, streakDays, title, unlockedAchievements)
        }

        fun addDistanceMeters(context: Context, meters: Float) {
            if (meters <= 0) return
            val prefs = context.getSharedPreferences("player_profile", Context.MODE_PRIVATE)
            val current = prefs.getFloat("total_distance_meters", 0f)
            prefs.edit().putFloat("total_distance_meters", current + meters).apply()
        }

        fun setPlayerName(context: Context, name: String) {
            val prefs = context.getSharedPreferences("player_profile", Context.MODE_PRIVATE)
            prefs.edit().putString("player_name", name.trim()).apply()
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
            val newXp = (current + amount).coerceAtLeast(0)
            prefs.edit().putInt("xp", newXp).apply()
            if (amount > 0) {
                DailyQuestManager.trackDungeonVisit(context, amount)
            }
        }

        fun subtractXP(context: Context, amount: Int) {
            addXP(context, -amount)
        }

        fun setXP(context: Context, newXP: Int) {
            val prefs = context.getSharedPreferences("player_profile", Context.MODE_PRIVATE)
            prefs.edit().putInt("xp", newXP.coerceAtLeast(0)).apply()
        }

        fun setExploredCount(context: Context, count: Int) {
            val prefs = context.getSharedPreferences("player_profile", Context.MODE_PRIVATE)
            prefs.edit().putInt("explored_count", count.coerceAtLeast(0)).apply()
        }

        fun subtractExplored(context: Context, amount: Int) {
            val prefs = context.getSharedPreferences("player_profile", Context.MODE_PRIVATE)
            val current = prefs.getInt("explored_count", 0)
            setExploredCount(context, current - amount)
        }

        fun setVisitedDungeons(context: Context, count: Int) {
            val prefs = context.getSharedPreferences("player_profile", Context.MODE_PRIVATE)
            prefs.edit().putInt("visited_dungeons", count.coerceAtLeast(0)).apply()
        }

        fun subtractDungeons(context: Context, amount: Int) {
            val prefs = context.getSharedPreferences("player_profile", Context.MODE_PRIVATE)
            val current = prefs.getInt("visited_dungeons", 0)
            setVisitedDungeons(context, current - amount)
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

            DailyQuestManager.trackExplore(context)
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
