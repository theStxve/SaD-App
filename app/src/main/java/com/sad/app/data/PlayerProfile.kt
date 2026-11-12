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
        /**
         * Gemeinsamer Lock für alle Read-Modify-Write-Zugriffe auf "player_profile".
         * Service (IO-Thread) und UI schreiben parallel – ohne Lock gehen Updates verloren.
         * Wird auch vom [DailyQuestManager] benutzt (reentrant, daher kein Deadlock).
         */
        val LOCK = Any()

        fun load(context: Context): PlayerProfile {
            val prefs = context.getSharedPreferences("player_profile", Context.MODE_PRIVATE)
            val playerName = prefs.getString("player_name", "") ?: ""
            val xp = prefs.getInt("xp", 0)
            val explored = prefs.getInt("explored_count", 0)
            val dungeons = prefs.getInt("visited_dungeons", 0)
            val nightExplored = prefs.getInt("night_explored_count", 0)
            val morningExplored = prefs.getInt("morning_explored_count", 0)

            // Rueckwirkende Distanz-Schaetzung als permanente Basis speichern
            var trackedMeters = prefs.getFloat("total_distance_meters", -1f)
            if (trackedMeters < 0f) {
                trackedMeters = (explored * 150f) + (dungeons * 1200f)
                prefs.edit().putFloat("total_distance_meters", trackedMeters).apply()
            }
            val totalDistanceMeters = trackedMeters

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
            synchronized(LOCK) {
                val prefs = prefs(context)
                var current = prefs.getFloat("total_distance_meters", -1f)
                if (current < 0f) {
                    val explored = prefs.getInt("explored_count", 0)
                    val dungeons = prefs.getInt("visited_dungeons", 0)
                    current = (explored * 150f) + (dungeons * 1200f)
                }
                prefs.edit().putFloat("total_distance_meters", current + meters).apply()
                DailyQuestManager.trackDistance(context, meters)
            }
        }

        fun setPlayerName(context: Context, name: String) {
            prefs(context).edit().putString("player_name", name.trim()).apply()
        }

        fun unlockAchievement(context: Context, id: String) = synchronized(LOCK) {
            val prefs = prefs(context)
            val current = prefs.getStringSet("unlocked_achievements", emptySet()) ?: emptySet()
            if (!current.contains(id)) {
                val newSet = current.toMutableSet().apply { add(id) }
                prefs.edit().putStringSet("unlocked_achievements", newSet).apply()
                addXP(context, 100)
            }
        }

        /**
         * @param countsForQuests false für XP, die selbst aus Quests stammen (Belohnungen)
         *   oder per Dev-Menü vergeben werden – sonst würden XP-Quests sich selbst füttern.
         */
        fun addXP(context: Context, amount: Int, countsForQuests: Boolean = true) = synchronized(LOCK) {
            val prefs = prefs(context)
            val newXp = (prefs.getInt("xp", 0) + amount).coerceAtLeast(0)
            prefs.edit().putInt("xp", newXp).apply()
            if (amount > 0 && countsForQuests) {
                DailyQuestManager.trackXpEarned(context, amount)
            }
        }

        fun subtractXP(context: Context, amount: Int) {
            addXP(context, -amount, countsForQuests = false)
        }

        fun setXP(context: Context, newXP: Int) {
            prefs(context).edit().putInt("xp", newXP.coerceAtLeast(0)).apply()
        }

        fun setExploredCount(context: Context, count: Int) {
            prefs(context).edit().putInt("explored_count", count.coerceAtLeast(0)).apply()
        }

        fun subtractExplored(context: Context, amount: Int) = synchronized(LOCK) {
            setExploredCount(context, prefs(context).getInt("explored_count", 0) - amount)
        }

        fun setVisitedDungeons(context: Context, count: Int) {
            prefs(context).edit().putInt("visited_dungeons", count.coerceAtLeast(0)).apply()
        }

        fun subtractDungeons(context: Context, amount: Int) = synchronized(LOCK) {
            setVisitedDungeons(context, prefs(context).getInt("visited_dungeons", 0) - amount)
        }

        fun incrementExplored(context: Context) = synchronized(LOCK) {
            val prefs = prefs(context)
            val edit = prefs.edit().putInt("explored_count", prefs.getInt("explored_count", 0) + 1)

            val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            if (hour >= 23 || hour < 4) {
                edit.putInt("night_explored_count", prefs.getInt("night_explored_count", 0) + 1)
            } else if (hour in 5..8) {
                edit.putInt("morning_explored_count", prefs.getInt("morning_explored_count", 0) + 1)
            }
            edit.apply()

            DailyQuestManager.trackExplore(context)
            addXP(context, 10)
        }

        /** Zählt nur den Dungeon. Die XP vergibt der Aufrufer (abhängig von der Rarität). */
        fun incrementDungeons(context: Context) = synchronized(LOCK) {
            val prefs = prefs(context)
            prefs.edit().putInt("visited_dungeons", prefs.getInt("visited_dungeons", 0) + 1).apply()
        }

        fun incrementReceivedRumors(context: Context) = synchronized(LOCK) {
            val prefs = prefs(context)
            prefs.edit().putInt("received_rumors_count", prefs.getInt("received_rumors_count", 0) + 1).apply()
        }

        fun markShared(context: Context) {
            prefs(context).edit().putBoolean("has_shared", true).apply()
        }

        private fun prefs(context: Context): SharedPreferences =
            context.applicationContext.getSharedPreferences("player_profile", Context.MODE_PRIVATE)
    }
}
