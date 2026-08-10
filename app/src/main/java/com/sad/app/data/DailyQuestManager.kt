package com.sad.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Calendar

enum class QuestPeriod { DAILY, WEEKLY }

data class DailyQuest(
    val id: String,
    val period: QuestPeriod,
    val title: String,
    val description: String,
    val targetCount: Int,
    val xpReward: Int,
    val type: String    // "dungeons" | "explore" | "rarity_rare" | "rarity_epic" | "xp" | "night" | "morning" | "distance"
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

object DailyQuestManager {

    private const val PREFS = "daily_quests"
    private const val KEY_CLAIMED_PREFIX = "claimed_"

    var dailyQuests  by mutableStateOf<List<DailyQuestState>>(emptyList()); private set
    var weeklyQuests by mutableStateOf<List<DailyQuestState>>(emptyList()); private set

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

    fun formatTimeUntilDailyReset(): String {
        val now = System.currentTimeMillis()
        val nextReset = todayStart() + 24 * 60 * 60 * 1000L
        val diff = (nextReset - now).coerceAtLeast(0L)
        val hours = diff / (1000 * 60 * 60)
        val minutes = (diff / (1000 * 60)) % 60
        val seconds = (diff / 1000) % 60
        return String.format(java.util.Locale.US, "%02dh %02dm %02ds", hours, minutes, seconds)
    }

    fun formatTimeUntilWeeklyReset(): String {
        val now = System.currentTimeMillis()
        val calNext = Calendar.getInstance()
        calNext.firstDayOfWeek = Calendar.MONDAY
        calNext.set(Calendar.HOUR_OF_DAY, 0)
        calNext.set(Calendar.MINUTE, 0)
        calNext.set(Calendar.SECOND, 0)
        calNext.set(Calendar.MILLISECOND, 0)

        val dayOfWeek = calNext.get(Calendar.DAY_OF_WEEK)
        var daysUntilMonday = (Calendar.MONDAY - dayOfWeek + 7) % 7
        if (daysUntilMonday == 0 && now >= calNext.timeInMillis) {
            daysUntilMonday = 7
        }
        calNext.add(Calendar.DAY_OF_YEAR, daysUntilMonday)
        val diff = (calNext.timeInMillis - now).coerceAtLeast(0L)
        val days = diff / (1000 * 60 * 60 * 24)
        val hours = (diff / (1000 * 60 * 60)) % 24
        val minutes = (diff / (1000 * 60)) % 60
        return if (days > 0) {
            "${days}d ${hours}h ${minutes}m"
        } else {
            String.format(java.util.Locale.US, "%02dh %02dm", hours, minutes)
        }
    }

    // ─── 100 DAILY QUEST TEMPLATES ──────────────────────────────────────────

    private val DAILY_POOL = listOf(
        // Dungeons (1-25)
        Triple("dungeons", "Erste Patrouille", "Besuche heute {n} Dungeon(s)"),
        Triple("dungeons", "Scout des Tages", "Besuche heute {n} Dungeon(s)"),
        Triple("dungeons", "Schattengang", "Betritt heute {n} Dungeon(s)"),
        Triple("dungeons", "Urban Raid", "Erkunde heute {n} Dungeon(s)"),
        Triple("dungeons", "Ruinen-Check", "Sichere heute {n} Dungeon(s)"),
        Triple("dungeons", "Zielerfassung", "Besuche heute {n} Dungeon(s)"),
        Triple("dungeons", "Gewoelbe-Scanner", "Scanne heute {n} Dungeon(s)"),
        Triple("dungeons", "Sub-Terra Tour", "Betritt heute {n} Dungeon(s)"),
        Triple("dungeons", "Sektor-Clearing", "Clean heute {n} Dungeon(s)"),
        Triple("dungeons", "Tages-Infiltration", "Infiltriere heute {n} Dungeon(s)"),
        Triple("dungeons", "Bunker-Inspektion", "Besuche heute {n} Dungeon(s)"),
        Triple("dungeons", "Gefahrenzone", "Betritt heute {n} Dungeon(s)"),
        Triple("dungeons", "Verlorene Hallen", "Besuche heute {n} Dungeon(s)"),
        Triple("dungeons", "Tagesbeute", "Sichere heute {n} Dungeon(s)"),
        Triple("dungeons", "Sektor-Scan", "Scanne heute {n} Dungeon(s)"),
        Triple("dungeons", "Vorstoss", "Betritt heute {n} Dungeon(s)"),
        Triple("dungeons", "Betreten verboten", "Betritt heute {n} Dungeon(s)"),
        Triple("dungeons", "Sicherheits-Check", "Besuche heute {n} Dungeon(s)"),
        Triple("dungeons", "Phantom-Spur", "Infiltriere heute {n} Dungeon(s)"),
        Triple("dungeons", "Festung", "Erstuerme heute {n} Dungeon(s)"),
        Triple("dungeons", "Sperrgebiet", "Betritt heute {n} Dungeon(s)"),
        Triple("dungeons", "Tagesziel", "Erreiche heute {n} Dungeon(s)"),
        Triple("dungeons", "Geister-Lauf", "Besuche heute {n} Dungeon(s)"),
        Triple("dungeons", "Schatten-Infiltration", "Infiltriere heute {n} Dungeon(s)"),
        Triple("dungeons", "Echo des Tages", "Erkunde heute {n} Dungeon(s)"),

        // Explore (26-50)
        Triple("explore", "Stadtlaeufer", "Erkunde heute {n} neue Bereiche"),
        Triple("explore", "Sektor-Erkundung", "Erkunde heute {n} neue Bereiche"),
        Triple("explore", "Karten-Update", "Erkunde heute {n} neue Bereiche"),
        Triple("explore", "Pflaster-Treter", "Leg heute {n} neue Bereiche frei"),
        Triple("explore", "Zonen-Scan", "Decke heute {n} neue Bereiche auf"),
        Triple("explore", "Gassen-Jaege", "Erkunde heute {n} neue Bereiche"),
        Triple("explore", "Nebel-Entferner", "Luefte den Nebel in {n} Bereichen"),
        Triple("explore", "Stadtteil-Scout", "Erkunde heute {n} neue Bereiche"),
        Triple("explore", "Urbanes Areal", "Erkunde heute {n} neue Bereiche"),
        Triple("explore", "Territorial-Gewinn", "Erkunde heute {n} neue Bereiche"),
        Triple("explore", "Asphalt-Surfer", "Decke heute {n} neue Bereiche auf"),
        Triple("explore", "Karten-Erweiterung", "Erkunde heute {n} neue Bereiche"),
        Triple("explore", "Areal-Sicherung", "Erkunde heute {n} neue Bereiche"),
        Triple("explore", "Nebel-Wanderer", "Luefte den Nebel in {n} Bereichen"),
        Triple("explore", "Strassen-Kartograph", "Erkunde heute {n} neue Bereiche"),
        Triple("explore", "Stadt-Pionier", "Decke heute {n} neue Bereiche auf"),
        Triple("explore", "Grenzgaenger", "Erkunde heute {n} neue Bereiche"),
        Triple("explore", "Sektor-Lauf", "Erkunde heute {n} neue Bereiche"),
        Triple("explore", "Bezirk-Checker", "Decke heute {n} neue Bereiche auf"),
        Triple("explore", "Neuland", "Erkunde heute {n} neue Bereiche"),
        Triple("explore", "Territorium", "Erkunde heute {n} neue Bereiche"),
        Triple("explore", "Radar-Check", "Decke heute {n} neue Bereiche auf"),
        Triple("explore", "Karten-Luecke", "Erkunde heute {n} neue Bereiche"),
        Triple("explore", "Urban-Expedition", "Erkunde heute {n} neue Bereiche"),
        Triple("explore", "Erkundungs-Rush", "Decke heute {n} neue Bereiche auf"),

        // Rarity & Special (51-75)
        Triple("rarity_rare", "Edler Fund", "Entdecke heute einen Rare Dungeon"),
        Triple("rarity_rare", "Sehr selten", "Findest du heute einen Rare Dungeon?"),
        Triple("rarity_rare", "Geldbeutel voll", "Loot einen Rare Dungeon"),
        Triple("rarity_epic", "Klassiker", "Spiere heute einen Epic Dungeon auf"),
        Triple("rarity_epic", "Legenden-Jagt", "Entdecke heute einen Epic Dungeon"),
        Triple("rarity_epic", "Das dicke Ding", "Sichere heute einen Epic Dungeon"),
        Triple("xp", "Tages-Earn", "Sammle heute {n} XP"),
        Triple("xp", "Erfahrungssammler", "Sammle heute {n} XP"),
        Triple("xp", "Tages-Konto", "Sammle heute {n} XP"),
        Triple("xp", "Fokus-Tag", "Sammle heute {n} XP"),
        Triple("xp", "XP-Booster", "Sammle heute {n} XP"),
        Triple("night", "Nachtschicht", "Erkunde heute nachts {n} Bereich(e)"),
        Triple("night", "Schattenlauf", "Erkunde heute nachts {n} Bereich(e)"),
        Triple("night", "Mitternacht-Spur", "Erkunde heute nachts {n} Bereich(e)"),
        Triple("morning", "Fruehsport", "Erkunde morgens {n} Bereich(e)"),
        Triple("morning", "Morgenstund", "Erkunde morgens {n} Bereich(e)"),
        Triple("morning", "Tagesanbruch", "Erkunde morgens {n} Bereich(e)"),
        Triple("rarity_rare", "Relikt-Suche", "Entdecke heute einen Rare Dungeon"),
        Triple("rarity_epic", "Episches Omen", "Entdecke heute einen Epic Dungeon"),
        Triple("xp", "Erfahrungs-Rush", "Sammle heute {n} XP"),
        Triple("xp", "Punktelandung", "Sammle heute {n} XP"),
        Triple("night", "Nacht-Patrouille", "Erkunde nachts {n} Bereich(e)"),
        Triple("morning", "Sonnenaufgang", "Erkunde morgens {n} Bereich(e)"),
        Triple("rarity_rare", "Seltenes Signal", "Sichere einen Rare Dungeon"),
        Triple("rarity_epic", "Meister-Fund", "Infiltriere einen Epic Dungeon"),

        // Distance & Extra (76-100)
        Triple("distance", "Tages-Marsch", "Lege heute {n} km zurück"),
        Triple("distance", "Schrittzaehler", "Wandere heute {n} km"),
        Triple("distance", "Beinarbeit", "Lege heute {n} km zurück"),
        Triple("distance", "Kilometer-Hunter", "Wandere heute {n} km"),
        Triple("distance", "Marathon-Vorbereitung", "Lege heute {n} km zurück"),
        Triple("distance", "Ausdauer-Test", "Wandere heute {n} km"),
        Triple("distance", "Strecken-Jaeger", "Lege heute {n} km zurück"),
        Triple("distance", "Marschbefehl", "Wandere heute {n} km"),
        Triple("distance", "Wanderstiefel", "Lege heute {n} km zurück"),
        Triple("distance", "Outdoor-Session", "Wandere heute {n} km"),
        Triple("distance", "Tages-Distanz", "Lege heute {n} km zurück"),
        Triple("dungeons", "Kurztrip", "Besuche heute {n} Dungeon(s)"),
        Triple("dungeons", "Blitz-Infiltration", "Besuche heute {n} Dungeon(s)"),
        Triple("explore", "Flugbahn", "Decke heute {n} Bereiche auf"),
        Triple("explore", "Spaziergang", "Erkunde heute {n} Bereiche"),
        Triple("xp", "Level-Up-Vorbereitung", "Sammle heute {n} XP"),
        Triple("xp", "Erfahrungs-Boost", "Sammle heute {n} XP"),
        Triple("distance", "Wegstrecke", "Lege heute {n} km zurück"),
        Triple("distance", "Tour de Stadt", "Wandere heute {n} km"),
        Triple("night", "Nachtfalke", "Erkunde nachts {n} Bereiche"),
        Triple("morning", "Fruehaufsteher", "Erkunde morgens {n} Bereiche"),
        Triple("rarity_rare", "Schatzsucher", "Entdecke einen Rare Dungeon"),
        Triple("rarity_epic", "Kronjuwel", "Entdecke einen Epic Dungeon"),
        Triple("dungeons", "Dungeon-Sprint", "Besuche heute {n} Dungeon(s)"),
        Triple("explore", "Abschluss-Runde", "Erkunde heute {n} Bereiche")
    )

    private val DAILY_TARGETS = mapOf(
        "dungeons" to listOf(1, 2, 3),
        "explore"  to listOf(3, 5, 8),
        "rarity_rare" to listOf(1),
        "rarity_epic" to listOf(1),
        "xp"       to listOf(100, 200, 300),
        "night"    to listOf(1, 2, 3),
        "morning"  to listOf(1, 2, 3),
        "distance" to listOf(1, 2, 3)
    )

    // ─── 50 WEEKLY QUEST TEMPLATES (Schwerer + Sehr viel mehr XP!) ──────────

    private val WEEKLY_POOL = listOf(
        // Dungeons Schwer (1-15)
        Triple("dungeons", "Wochen-Raider", "Besuche diese Woche {n} Dungeons"),
        Triple("dungeons", "Hardcore-Woche", "Besuche diese Woche {n} Dungeons"),
        Triple("dungeons", "Dungeon-Marathon", "Betritt diese Woche {n} Dungeons"),
        Triple("dungeons", "Untergrund-Imperium", "Cleare diese Woche {n} Dungeons"),
        Triple("dungeons", "Sektor-Dominanz", "Besuche diese Woche {n} Dungeons"),
        Triple("dungeons", "Gewoelbe-Spezialist", "Erkunde diese Woche {n} Dungeons"),
        Triple("dungeons", "Wochen-Expedition", "Betritt diese Woche {n} Dungeons"),
        Triple("dungeons", "Dungeon-Bezwinger", "Cleare diese Woche {n} Dungeons"),
        Triple("dungeons", "Ruinen-Meister", "Besuche diese Woche {n} Dungeons"),
        Triple("dungeons", "Schatten-Woche", "Betritt diese Woche {n} Dungeons"),
        Triple("dungeons", "Großmeister-Raid", "Erkunde diese Woche {n} Dungeons"),
        Triple("dungeons", "Infiltrations-Serie", "Besuche diese Woche {n} Dungeons"),
        Triple("dungeons", "Dungeon-Veteran", "Betritt diese Woche {n} Dungeons"),
        Triple("dungeons", "Subterra-Herrscher", "Cleare diese Woche {n} Dungeons"),
        Triple("dungeons", "Wochen-Abschluss", "Besuche diese Woche {n} Dungeons"),

        // Explore Schwer (16-30)
        Triple("explore", "Wochen-Kartograph", "Erkunde diese Woche {n} neue Bereiche"),
        Triple("explore", "Stadt-Bezwinger", "Erkunde diese Woche {n} neue Bereiche"),
        Triple("explore", "Grossraum-Scan", "Decke diese Woche {n} Bereiche auf"),
        Triple("explore", "Pflaster-Legende", "Erkunde diese Woche {n} neue Bereiche"),
        Triple("explore", "Nebel-Vernichter", "Decke diese Woche {n} Bereiche auf"),
        Triple("explore", "Metropolen-Scout", "Erkunde diese Woche {n} neue Bereiche"),
        Triple("explore", "Grossstadt-Wanderer", "Erkunde diese Woche {n} neue Bereiche"),
        Triple("explore", "Sektor-Grossreinemachen", "Decke diese Woche {n} Bereiche auf"),
        Triple("explore", "Territorial-Imperium", "Erkunde diese Woche {n} neue Bereiche"),
        Triple("explore", "Asphalt-König", "Decke diese Woche {n} Bereiche auf"),
        Triple("explore", "Wochen-Grenzgang", "Erkunde diese Woche {n} neue Bereiche"),
        Triple("explore", "Urbaner Feldzug", "Decke diese Woche {n} Bereiche auf"),
        Triple("explore", "Kartograph der Woche", "Erkunde diese Woche {n} neue Bereiche"),
        Triple("explore", "Nebel-Jäger", "Decke diese Woche {n} Bereiche auf"),
        Triple("explore", "Metropol-Eroberer", "Erkunde diese Woche {n} neue Bereiche"),

        // XP & Distance Schwer (31-50)
        Triple("xp", "XP-Kollektor", "Sammle diese Woche {n} XP"),
        Triple("xp", "Wochen-Ertrag", "Sammle diese Woche {n} XP"),
        Triple("xp", "Punkte-Gewitter", "Sammle diese Woche {n} XP"),
        Triple("xp", "XP-Meister", "Sammle diese Woche {n} XP"),
        Triple("xp", "Legendärer Aufstieg", "Sammle diese Woche {n} XP"),
        Triple("distance", "Wochen-Marsch", "Leg diese Woche {n} km zurück"),
        Triple("distance", "Fernwanderer", "Leg diese Woche {n} km zurück"),
        Triple("distance", "Kilometer-Fresser", "Leg diese Woche {n} km zurück"),
        Triple("distance", "Marathon-Woche", "Leg diese Woche {n} km zurück"),
        Triple("distance", "Ausdauer-Titan", "Leg diese Woche {n} km zurück"),
        Triple("distance", "Ultra-Wanderer", "Leg diese Woche {n} km zurück"),
        Triple("distance", "Strecken-Koenig", "Leg diese Woche {n} km zurück"),
        Triple("xp", "XP-Gigant", "Sammle diese Woche {n} XP"),
        Triple("xp", "Erfahrungs-Gigant", "Sammle diese Woche {n} XP"),
        Triple("dungeons", "Wochen-Clearing", "Besuche diese Woche {n} Dungeons"),
        Triple("explore", "Total-Eroberung", "Erkunde diese Woche {n} Bereiche"),
        Triple("distance", "Super-Marsch", "Leg diese Woche {n} km zurück"),
        Triple("xp", "Mega-XP", "Sammle diese Woche {n} XP"),
        Triple("dungeons", "Finale Dungeon-Woche", "Besuche diese Woche {n} Dungeons"),
        Triple("distance", "Endlos-Schritt", "Leg diese Woche {n} km zurück")
    )

    private val WEEKLY_TARGETS = mapOf(
        "dungeons" to listOf(5, 10, 15, 20),
        "explore"  to listOf(20, 30, 50, 75),
        "xp"       to listOf(500, 1000, 2000, 3000),
        "distance" to listOf(5, 10, 15, 25)
    )

    // ── Generator ───────────────────────────────────────────────────────────

    private fun generateDailyQuests(seed: Int): List<DailyQuest> {
        val rng = java.util.Random(seed.toLong())
        val shuffled = DAILY_POOL.shuffled(rng)
        val selected = shuffled.take(3)
        return selected.mapIndexed { i, (type, title, desc) ->
            val targets = DAILY_TARGETS[type] ?: listOf(1)
            val target = targets[rng.nextInt(targets.size)]
            val xp = when (type) {
                "rarity_epic" -> 250
                "rarity_rare" -> 180
                "dungeons"    -> target * 75
                "explore"     -> target * 25
                "distance"    -> target * 80
                "night"       -> target * 90
                "morning"     -> target * 90
                "xp"          -> (target * 0.8).toInt()
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
        val rng = java.util.Random(seed.toLong() + 8888)
        val shuffled = WEEKLY_POOL.shuffled(rng)
        val selected = shuffled.take(3) // 3 Schwerere Wochen-Quests
        return selected.mapIndexed { i, (type, title, desc) ->
            val targets = WEEKLY_TARGETS[type] ?: listOf(5)
            val target = targets[rng.nextInt(targets.size)]
            val xp = when (type) {
                "dungeons" -> target * 120
                "explore"  -> target * 40
                "distance" -> target * 100
                "xp"       -> (target * 0.5).toInt()
                else       -> 500
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

    // ── Refresh & Progress Calculation ──────────────────────────────────────

    suspend fun refresh(context: Context, gameDb: GameDatabase, placeDb: AppDatabase) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        val daySeed  = todayKey().hashCode()
        val weekSeed = weekKey().hashCode()

        val generatedDaily  = generateDailyQuests(daySeed)
        val generatedWeekly = generateWeeklyQuests(weekSeed)

        val todaySince = todayStart()
        val weekSince  = weekStart()

        // Counts
        val dungeonsToday = gameDb.visitedDungeonDao().countSince(todaySince)
        val dungeonsWeek  = gameDb.visitedDungeonDao().countSince(weekSince)

        val profilePrefs = context.getSharedPreferences("player_profile", Context.MODE_PRIVATE)
        val exploredToday = profilePrefs.getInt("explored_today_${todayKey()}", 0)
        val exploredWeek  = profilePrefs.getInt("explored_week_${weekKey()}", 0)

        val osmIdsToday = gameDb.visitedDungeonDao().getOsmIdsSince(todaySince)
        val raritiesAvailable = if (osmIdsToday.isNotEmpty())
            placeDb.placeDao().getRaritiesForIds(osmIdsToday)
        else emptyList()

        val xpToday = profilePrefs.getInt("xp_earned_today_${todayKey()}", 0)
        val xpWeek  = profilePrefs.getInt("xp_earned_week_${weekKey()}", 0)

        val distTodayM = profilePrefs.getFloat("dist_today_${todayKey()}", 0f)
        val distWeekM  = profilePrefs.getFloat("dist_week_${weekKey()}", 0f)
        val distTodayKm = (distTodayM / 1000f).toInt()
        val distWeekKm  = (distWeekM / 1000f).toInt()

        val nightToday   = profilePrefs.getInt("night_today_${todayKey()}", 0)
        val morningToday = profilePrefs.getInt("morning_today_${todayKey()}", 0)

        fun progressFor(quest: DailyQuest, isWeekly: Boolean): Int {
            val dungeons = if (isWeekly) dungeonsWeek else dungeonsToday
            val explored = if (isWeekly) exploredWeek  else exploredToday
            val xp       = if (isWeekly) xpWeek        else xpToday
            val distKm   = if (isWeekly) distWeekKm    else distTodayKm

            return when (quest.type) {
                "dungeons"    -> dungeons
                "explore"     -> explored
                "rarity_rare" -> if (raritiesAvailable.contains("rare") || raritiesAvailable.contains("epic")) 1 else 0
                "rarity_epic" -> if (raritiesAvailable.contains("epic")) 1 else 0
                "xp"          -> xp
                "distance"    -> distKm
                "night"       -> nightToday
                "morning"     -> morningToday
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

        dailyQuests  = dailyQuests.map  { if (it.quest.id == questId) it.copy(claimed = true) else it }
        weeklyQuests = weeklyQuests.map { if (it.quest.id == questId) it.copy(claimed = true) else it }

        return quest.quest.xpReward
    }

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

    fun trackExplore(context: Context) {
        val prefs = context.getSharedPreferences("player_profile", Context.MODE_PRIVATE)
        val todayK = todayKey()
        val weekK  = weekKey()
        val curDay  = prefs.getInt("explored_today_$todayK", 0)
        val curWeek = prefs.getInt("explored_week_$weekK", 0)

        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val edit = prefs.edit()
            .putInt("explored_today_$todayK", curDay + 1)
            .putInt("explored_week_$weekK",   curWeek + 1)

        if (hour >= 23 || hour < 4) {
            val night = prefs.getInt("night_today_$todayK", 0)
            edit.putInt("night_today_$todayK", night + 1)
        } else if (hour in 5..8) {
            val morning = prefs.getInt("morning_today_$todayK", 0)
            edit.putInt("morning_today_$todayK", morning + 1)
        }
        edit.apply()
    }

    fun trackDistance(context: Context, meters: Float) {
        if (meters <= 0) return
        val prefs = context.getSharedPreferences("player_profile", Context.MODE_PRIVATE)
        val todayK = todayKey()
        val weekK  = weekKey()
        val curDayM  = prefs.getFloat("dist_today_$todayK", 0f)
        val curWeekM = prefs.getFloat("dist_week_$weekK", 0f)
        prefs.edit()
            .putFloat("dist_today_$todayK", curDayM + meters)
            .putFloat("dist_week_$weekK",   curWeekM + meters)
            .apply()
    }
}
