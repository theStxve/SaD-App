package com.sad.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Calendar

// ─── Datenmodell ────────────────────────────────────────────────────────────

enum class QuestPeriod { DAILY, WEEKLY }

data class DailyQuest(
    val id: String,
    val period: QuestPeriod,
    val title: String,
    val description: String,
    val targetCount: Int,
    val xpReward: Int,
    val type: String    // "dungeons" | "explore" | "rarity_rare" | "rarity_epic" | "xp"
)

data class DailyQuestState(
    val quest: DailyQuest,
    val progress: Int,
    val claimed: Boolean
) {
    val isComplete get() = progress >= quest.targetCount
    val isClaimable get() = isComplete && !claimed
    val progressFraction get() = (progress.toFloat() / quest.targetCount).coerceIn(0f, 1f)
}

// ─── Manager ────────────────────────────────────────────────────────────────

object DailyQuestManager {

    private const val PREFS = "daily_quests"
    private const val KEY_CLAIMED_PREFIX = "claimed_"
    private const val KEY_LAST_DAY = "last_day"
    private const val KEY_LAST_WEEK = "last_week"

    // Reaktiver State fuer Compose
    var dailyQuests  by mutableStateOf<List<DailyQuestState>>(emptyList()); private set
    var weeklyQuests by mutableStateOf<List<DailyQuestState>>(emptyList()); private set

    // ── Zeithelfer ──────────────────────────────────────────────────────────

    fun todayStart(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0);      cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun weekStart(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0);      cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun todayKey(): String {
        val cal = Calendar.getInstance()
        return "${cal.get(Calendar.YEAR)}_${cal.get(Calendar.DAY_OF_YEAR)}"
    }

    private fun weekKey(): String {
        val cal = Calendar.getInstance()
        return "${cal.get(Calendar.YEAR)}_W${cal.get(Calendar.WEEK_OF_YEAR)}"
    }

    // ── Quest-Generierung (deterministisch aus Datum) ───────────────────────

    private val DAILY_POOL = listOf(
        Triple("dungeons", "Dungeon-Run",         "Besuche heute {n} Dungeon(s)"),
        Triple("explore",  "Gebiet erkunden",      "Erkunde heute {n} neue Bereiche"),
        Triple("dungeons", "Aktiver Erkunder",     "Besuche heute {n} Dungeon(s)"),
        Triple("rarity_rare",  "Rarer Fund",       "Entdecke heute einen Rare Dungeon"),
        Triple("rarity_epic",  "Epischer Fund",    "Entdecke heute einen Epic Dungeon"),
        Triple("explore",  "Stadtlaeufer",         "Erkunde heute {n} neue Bereiche"),
        Triple("dungeons", "Naechtliche Tour",     "Besuche heute {n} Dungeon(s)"),
    )

    private val DAILY_TARGETS = mapOf(
        "dungeons" to listOf(1, 2, 3),
        "explore"  to listOf(3, 5, 8),
        "rarity_rare" to listOf(1),
        "rarity_epic" to listOf(1)
    )

    private val WEEKLY_POOL = listOf(
        Triple("dungeons", "Wochen-Raider",    "Besuche diese Woche {n} Dungeons"),
        Triple("explore",  "Wochenerkunder",   "Erkunde diese Woche {n} Bereiche"),
        Triple("dungeons", "Hardcore-Woche",   "Besuche diese Woche {n} Dungeons"),
        Triple("xp",       "XP-Kollektor",     "Sammle diese Woche {n} XP"),
    )

    private val WEEKLY_TARGETS = mapOf(
        "dungeons" to listOf(5, 10, 15),
        "explore"  to listOf(20, 30, 50),
        "xp"       to listOf(300, 500, 1000)
    )

    private fun generateDailyQuests(seed: Int): List<DailyQuest> {
        val rng = java.util.Random(seed.toLong())
        val shuffled = DAILY_POOL.shuffled(rng)
        val selected = shuffled.take(3)
        return selected.mapIndexed { i, (type, title, desc) ->
            val targets = DAILY_TARGETS[type] ?: listOf(1)
            val target = targets[rng.nextInt(targets.size)]
            val xp = when (type) {
                "rarity_epic" -> 200
                "rarity_rare" -> 150
                "dungeons"    -> target * 60
                "explore"     -> target * 20
                else          -> 100
            }
            DailyQuest(
                id = "daily_${todayKey()}_$i",
                period = QuestPeriod.DAILY,
                title = title,
                description = desc.replace("{n}", target.toString()),
                targetCount = target,
                xpReward = xp,
                type = type
            )
        }
    }

    private fun generateWeeklyQuests(seed: Int): List<DailyQuest> {
        val rng = java.util.Random(seed.toLong() + 9999)
        val shuffled = WEEKLY_POOL.shuffled(rng)
        val selected = shuffled.take(2)
        return selected.mapIndexed { i, (type, title, desc) ->
            val targets = WEEKLY_TARGETS[type] ?: listOf(5)
            val target = targets[rng.nextInt(targets.size)]
            val xp = when (type) {
                "dungeons" -> target * 50
                "explore"  -> target * 15
                "xp"       -> target / 3
                else       -> 200
            }
            DailyQuest(
                id = "weekly_${weekKey()}_$i",
                period = QuestPeriod.WEEKLY,
                title = title,
                description = desc.replace("{n}", target.toString()),
                targetCount = target,
                xpReward = xp,
                type = type
            )
        }
    }

    // ── Laden & Refresh ─────────────────────────────────────────────────────

    suspend fun refresh(context: Context, gameDb: GameDatabase, placeDb: AppDatabase) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        val daySeed  = todayKey().hashCode()
        val weekSeed = weekKey().hashCode()

        val generatedDaily  = generateDailyQuests(daySeed)
        val generatedWeekly = generateWeeklyQuests(weekSeed)

        val todaySince = todayStart()
        val weekSince  = weekStart()

        // Fortschritt berechnen
        val dungeonsToday  = gameDb.visitedDungeonDao().countSince(todaySince)
        val dungeonsWeek   = gameDb.visitedDungeonDao().countSince(weekSince)

        val exploredToday = context.getSharedPreferences("player_profile", Context.MODE_PRIVATE)
            .getInt("explored_today_${todayKey()}", 0)
        val exploredWeek  = context.getSharedPreferences("player_profile", Context.MODE_PRIVATE)
            .getInt("explored_week_${weekKey()}", 0)

        // Rarity-Checks: Osm-IDs seit heute / Woche -> Rarity aus places.db
        val osmIdsToday = gameDb.visitedDungeonDao().getOsmIdsSince(todaySince)
        val raritiesAvailable = if (osmIdsToday.isNotEmpty())
            placeDb.placeDao().getRaritiesForIds(osmIdsToday)
        else emptyList()

        val xpToday = context.getSharedPreferences("player_profile", Context.MODE_PRIVATE)
            .getInt("xp_earned_today_${todayKey()}", 0)
        val xpWeek  = context.getSharedPreferences("player_profile", Context.MODE_PRIVATE)
            .getInt("xp_earned_week_${weekKey()}", 0)

        fun progressFor(quest: DailyQuest, isWeekly: Boolean): Int {
            val dungeons = if (isWeekly) dungeonsWeek else dungeonsToday
            val explored = if (isWeekly) exploredWeek  else exploredToday
            val xp       = if (isWeekly) xpWeek        else xpToday
            return when (quest.type) {
                "dungeons"    -> dungeons
                "explore"     -> explored
                "rarity_rare" -> if (raritiesAvailable.contains("rare") || raritiesAvailable.contains("epic")) 1 else 0
                "rarity_epic" -> if (raritiesAvailable.contains("epic")) 1 else 0
                "xp"          -> xp
                else          -> 0
            }
        }

        dailyQuests = generatedDaily.map { q ->
            DailyQuestState(
                quest = q,
                progress = progressFor(q, false),
                claimed = prefs.getBoolean(KEY_CLAIMED_PREFIX + q.id, false)
            )
        }

        weeklyQuests = generatedWeekly.map { q ->
            DailyQuestState(
                quest = q,
                progress = progressFor(q, true),
                claimed = prefs.getBoolean(KEY_CLAIMED_PREFIX + q.id, false)
            )
        }
    }

    fun claimReward(context: Context, questId: String): Int {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_CLAIMED_PREFIX + questId, false)) return 0

        val quest = (dailyQuests + weeklyQuests).find { it.quest.id == questId } ?: return 0
        if (!quest.isClaimable) return 0

        prefs.edit().putBoolean(KEY_CLAIMED_PREFIX + questId, true).apply()
        PlayerProfile.addXP(context, quest.quest.xpReward)

        // State aktualisieren
        dailyQuests  = dailyQuests.map  { if (it.quest.id == questId) it.copy(claimed = true) else it }
        weeklyQuests = weeklyQuests.map { if (it.quest.id == questId) it.copy(claimed = true) else it }

        return quest.quest.xpReward
    }

    /** Soll nach jedem Dungeon-Besuch aufgerufen werden um XP-Tracking zu aktualisieren */
    fun trackDungeonVisit(context: Context, xpEarned: Int) {
        val prefs = context.getSharedPreferences("player_profile", Context.MODE_PRIVATE)
        val todayK = todayKey()
        val weekK  = weekKey()
        val curDayXP  = prefs.getInt("xp_earned_today_$todayK", 0)
        val curWeekXP = prefs.getInt("xp_earned_week_$weekK", 0)
        prefs.edit()
            .putInt("xp_earned_today_$todayK", curDayXP + xpEarned)
            .putInt("xp_earned_week_$weekK",   curWeekXP + xpEarned)
            .apply()
    }

    /** Soll nach jedem Bereich-Erkunden aufgerufen werden */
    fun trackExplore(context: Context) {
        val prefs = context.getSharedPreferences("player_profile", Context.MODE_PRIVATE)
        val todayK = todayKey()
        val weekK  = weekKey()
        val curDay  = prefs.getInt("explored_today_$todayK", 0)
        val curWeek = prefs.getInt("explored_week_$weekK", 0)
        prefs.edit()
            .putInt("explored_today_$todayK", curDay + 1)
            .putInt("explored_week_$weekK",   curWeek + 1)
            .apply()
    }
}
