package com.sad.app.ui

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sad.app.data.AppDatabase
import com.sad.app.data.DailyQuestManager
import com.sad.app.data.DailyQuestState
import com.sad.app.data.GameDatabase
import com.sad.app.data.PlayerProfile
import com.sad.app.data.QuestPeriod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class Quest(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val targetCount: Int,
    val xpReward: Int,
    val category: String
)

val ALL_QUESTS = listOf(
    Quest("explore_1", "Erste Schritte", "Erkunde deinen ersten Bereich der Stadt", "◇", 1, 50, "Erkundung"),
    Quest("explore_10", "Stadtlaeufer", "Erkunde 10 verschiedene Bereiche", "◈", 10, 200, "Erkundung"),
    Quest("explore_50", "Kartograph", "Erkunde 50 verschiedene Bereiche", "⬡", 50, 500, "Erkundung"),
    Quest("explore_100", "Stadtkenner", "Erkunde 100 Bereiche", "⬢", 100, 1000, "Erkundung"),
    Quest("explore_500", "Vagabund", "Erkunde 500 Bereiche", "⎔", 500, 2500, "Erkundung"),
    Quest("explore_1000", "Urban Explorer", "Erkunde 1.000 Bereiche", "⏣", 1000, 5000, "Erkundung"),
    Quest("explore_5000", "Eroberer der Stadt", "Erkunde 5.000 Bereiche", "◩", 5000, 20000, "Erkundung"),
    Quest("explore_10000", "Das wandelnde Lexikon", "Erkunde unfassbare 10.000 Bereiche", "◪", 10000, 50000, "Erkundung"),

    Quest("dungeon_1", "Dungeon-Einsteiger", "Besuche deinen ersten Dungeon", "◬", 1, 100, "Dungeons"),
    Quest("dungeon_5", "Dungeon-Laeufer", "Besuche 5 Dungeons", "▲", 5, 300, "Dungeons"),
    Quest("dungeon_20", "Dungeon-Meister", "Besuche 20 Dungeons", "▼", 20, 800, "Dungeons"),
    Quest("dungeon_50", "Dungeon-Lord", "Besuche 50 Dungeons", "⚔", 50, 2000, "Dungeons"),
    Quest("dungeon_100", "Schattengaenger", "Betritt 100 Dungeons", "☣", 100, 5000, "Dungeons"),
    Quest("dungeon_500", "Koenig des Untergrunds", "Betritt 500 Dungeons", "☢", 500, 25000, "Dungeons"),
    Quest("dungeon_1000", "Cyber-Gott", "Betritt 1.000 Dungeons", "☠", 1000, 100000, "Dungeons"),

    Quest("lost_1", "Verlorene Seele", "Entdecke einen Lost Place", "∅", 1, 200, "Lost Places"),
    Quest("lost_5", "Ruinenjaeger", "Entdecke 5 Lost Places", "⊘", 5, 600, "Lost Places"),
    Quest("lost_20", "Geisterfluest.", "Entdecke 20 Lost Places", "⊗", 20, 2500, "Lost Places"),
    Quest("lost_50", "Reliktjaeger", "Entdecke 50 Lost Places", "⍚", 50, 10000, "Lost Places"),

    Quest("xp_500", "Aufsteiger", "Sammle 500 XP", "★", 500, 100, "Profil"),
    Quest("xp_2000", "Veteran", "Sammle 2.000 XP", "✦", 2000, 300, "Profil"),
    Quest("xp_10000", "Legende", "Sammle 10.000 XP", "✧", 10000, 1500, "Profil"),
    Quest("xp_50000", "Halbgott", "Sammle 50.000 XP", "✪", 50000, 5000, "Profil"),
    Quest("xp_250000", "Mythos", "Sammle 250.000 XP", "❂", 250000, 50000, "Profil"),
)

// ─── Stats-Karte ─────────────────────────────────────────────────────────────

@Composable
fun StatsCard(profile: PlayerProfile, rarityBreakdown: Map<String, Int>, colors: AppColors) {
    var expanded by remember { mutableStateOf(false) }

    val kmFormatted = String.format(java.util.Locale.US, "%.1f km", profile.totalDistanceKm)
    val avgXpPerDungeon = if (profile.visitedDungeons > 0) profile.xp / profile.visitedDungeons else 0
    val kmPerDungeon = if (profile.visitedDungeons > 0) profile.totalDistanceKm / profile.visitedDungeons else 0f
    val kmPerDungeonFormatted = String.format(java.util.Locale.US, "%.2f km", kmPerDungeon)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .border(1.dp, colors.primary.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .clickable { expanded = !expanded }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("STATISTIKEN", color = colors.primary, fontSize = 10.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text("Level ${profile.level} - ${profile.title}",
                    color = colors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Black)
            }
            Text(if (expanded) "▲" else "▼", color = colors.primary, fontSize = 14.sp)
        }

        Spacer(Modifier.height(10.dp))

        // Immer sichtbar: Haupt-Pills (Strecke, Bereiche, Dungeons)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StatPill(kmFormatted, colors.primary, colors)
            StatPill("${profile.exploredCount} Bereiche", colors.accent, colors)
            StatPill("${profile.visitedDungeons} Dungeons", colors.gold, colors)
        }

        Spacer(Modifier.height(6.dp))

        // Immer sichtbar: XP & Rate
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StatPill("${profile.xp} XP Gesamt", colors.gold, colors)
            StatPill("Ø $avgXpPerDungeon XP/Dungeon", colors.textSecondary, colors)
        }

        if (expanded) {
            Spacer(Modifier.height(12.dp))
            Divider(color = colors.surfaceVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(12.dp))

            // Rarity-Breakdown
            Text("NACH RARITAET", color = colors.textSecondary, fontSize = 10.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                RarityStatBox("Epic",     rarityBreakdown["epic"] ?: 0,     Color(0xFFFF00E6), colors, Modifier.weight(1f))
                RarityStatBox("Rare",     rarityBreakdown["rare"] ?: 0,     Color(0xFFFFD700), colors, Modifier.weight(1f))
                RarityStatBox("Uncommon", rarityBreakdown["uncommon"] ?: 0, Color(0xFF00FF00), colors, Modifier.weight(1f))
                RarityStatBox("Common",   rarityBreakdown["common"] ?: 0,   Color(0xFF555555), colors, Modifier.weight(1f))
            }

            Spacer(Modifier.height(12.dp))
            Divider(color = colors.surfaceVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(12.dp))

            // Effizienz & Tageszeiten
            Text("EFFIZIENZ & ZEITEN", color = colors.textSecondary, fontSize = 10.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StatPill("Ø $kmPerDungeonFormatted / Dungeon", colors.primary, colors)
                StatPill("${profile.nightExploredCount} Nacht", Color(0xFF8888FF), colors)
                StatPill("${profile.morningExploredCount} Morgen", Color(0xFFFF8C00), colors)
            }
        }
    }
}

@Composable
private fun StatPill(text: String, color: Color, colors: AppColors) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RarityStatBox(label: String, count: Int, color: Color, colors: AppColors, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.08f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("$count", color = color, fontSize = 18.sp, fontWeight = FontWeight.Black)
        Text(label, color = colors.textSecondary, fontSize = 9.sp, textAlign = TextAlign.Center)
    }
}

// ─── Daily/Weekly Quest Card ──────────────────────────────────────────────────

@Composable
fun DailyQuestCard(state: DailyQuestState, colors: AppColors, onClaim: () -> Unit) {
    val isComplete   = state.isComplete
    val isClaimed    = state.claimed
    val isClaimable  = state.isClaimable

    val borderColor by animateColorAsState(
        targetValue = when {
            isClaimed   -> colors.surfaceVariant
            isClaimable -> colors.gold
            else        -> colors.surfaceVariant
        },
        animationSpec = tween(400), label = "border"
    )
    val bgColor = when {
        isClaimed   -> colors.surface
        isClaimable -> colors.gold.copy(alpha = 0.07f)
        else        -> colors.surface
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(state.quest.title,
                        color = if (isClaimed) colors.textSecondary else colors.textPrimary,
                        fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(state.quest.description, color = colors.textSecondary, fontSize = 12.sp)
                }
                Spacer(Modifier.width(10.dp))
                when {
                    isClaimed -> Text("ERLEDIGT", color = colors.textSecondary,
                        fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    isClaimable -> Button(
                        onClick = onClaim,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.gold.copy(alpha = 0.22f)),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, colors.gold),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text("EINLÖSEN", color = colors.gold, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                            Text("•", color = colors.gold.copy(alpha = 0.5f), fontSize = 10.sp)
                            Text("+${state.quest.xpReward} XP", color = colors.gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    else -> Text("+${state.quest.xpReward} XP",
                        color = colors.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (!isClaimed) {
                Spacer(Modifier.height(10.dp))
                val fraction by animateFloatAsState(
                    targetValue = state.progressFraction,
                    animationSpec = tween(600), label = "progress"
                )
                Box(
                    modifier = Modifier.fillMaxWidth().height(4.dp)
                        .clip(RoundedCornerShape(2.dp)).background(colors.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(fraction).fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (isClaimable) colors.gold else colors.primary)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "${state.progress.coerceAtMost(state.quest.targetCount)} / ${state.quest.targetCount}",
                    color = colors.textSecondary, fontSize = 11.sp
                )
            }
        }
    }
}

// ─── Quest Card (Meilensteine) ────────────────────────────────────────────────

@Composable
fun QuestCard(quest: Quest, progress: Int, completed: Boolean, colors: AppColors) {
    val borderColor = if (completed) colors.gold else colors.surfaceVariant
    val progressFraction = (progress.toFloat() / quest.targetCount).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.width(36.dp), contentAlignment = Alignment.Center) {
                    Text(quest.icon, fontSize = 24.sp,
                        color = if (completed) colors.gold else colors.primary,
                        fontWeight = FontWeight.Light)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(quest.title,
                        color = if (completed) colors.gold else colors.textPrimary,
                        fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(quest.description, color = colors.textSecondary, fontSize = 12.sp)
                }
                if (completed) {
                    Text("✓", color = colors.gold, fontSize = 22.sp, fontWeight = FontWeight.Black)
                } else {
                    Text("+${quest.xpReward} XP", color = colors.primary,
                        fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (!completed) {
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().height(4.dp)
                        .clip(RoundedCornerShape(2.dp)).background(colors.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(progressFraction).fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp)).background(colors.primary)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text("${progress.coerceAtMost(quest.targetCount)} / ${quest.targetCount}",
                    color = colors.textSecondary, fontSize = 11.sp)
            }
        }
    }
}

// ─── Hauptscreen ─────────────────────────────────────────────────────────────

@Composable
fun QuestScreen(refreshKey: Int = 0) {
    val context = LocalContext.current
    val colors  = LocalAppColors.current
    val scope   = rememberCoroutineScope()

    val profile by produceState(initialValue = PlayerProfile.load(context), refreshKey) {
        value = PlayerProfile.load(context)
    }

    val gameDb  = remember { GameDatabase.getDatabase(context) }
    val placeDb = remember { AppDatabase.getDatabase(context) }

    // Stats: Rarity-Breakdown retroaktiv aus DB
    var rarityBreakdown by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }

    // Live Countdowns
    var dailyCountdown  by remember { mutableStateOf(DailyQuestManager.formatTimeUntilDailyReset()) }
    var weeklyCountdown by remember { mutableStateOf(DailyQuestManager.formatTimeUntilWeeklyReset()) }

    LaunchedEffect(Unit) {
        while (true) {
            dailyCountdown  = DailyQuestManager.formatTimeUntilDailyReset()
            weeklyCountdown = DailyQuestManager.formatTimeUntilWeeklyReset()
            kotlinx.coroutines.delay(1000L)
        }
    }

    // Daily/Weekly Quests laden
    LaunchedEffect(refreshKey) {
        withContext(Dispatchers.IO) {
            // Rarity-Breakdown: alle besuchten Dungeons -> Rarity aus places.db
            val allVisited = gameDb.visitedDungeonDao().getAll()
            if (allVisited.isNotEmpty()) {
                val ids = allVisited.map { it.osm_id }
                val rarities = placeDb.placeDao().getRaritiesForIds(ids)
                rarityBreakdown = rarities.groupingBy { it }.eachCount()
            }
            // Daily/Weekly Quests refreshen
            DailyQuestManager.refresh(context, gameDb, placeDb)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // ── Header
        item {
            Text("QUEST LOG", color = colors.primary, fontSize = 11.sp, letterSpacing = 4.sp)
            Text("Auftraege", color = colors.textPrimary, fontSize = 28.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(12.dp))
        }

        // ── Stats-Karte
        item {
            StatsCard(profile, rarityBreakdown, colors)
            Spacer(Modifier.height(8.dp))
        }

        // ── Tagesaufgaben
        item {
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("TAGESAUFGABEN", color = colors.accent, fontSize = 11.sp,
                    letterSpacing = 3.sp, fontWeight = FontWeight.Bold)
                Text("Reset in $dailyCountdown", color = colors.textSecondary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(4.dp))
        }
        val daily = DailyQuestManager.dailyQuests
        if (daily.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp)).background(colors.surface)
                        .padding(16.dp), contentAlignment = Alignment.Center
                ) { Text("Laden...", color = colors.textSecondary, fontSize = 13.sp) }
            }
        } else {
            items(daily, key = { it.quest.id }) { state ->
                DailyQuestCard(state, colors) {
                    val earned = DailyQuestManager.claimReward(context, state.quest.id)
                    if (earned > 0) Toast.makeText(context, "+$earned XP kassiert!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // ── Wochenaufgaben
        item {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("WOCHENAUFGABEN", color = colors.primary, fontSize = 11.sp,
                    letterSpacing = 3.sp, fontWeight = FontWeight.Bold)
                Text("Reset in $weeklyCountdown", color = colors.textSecondary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(4.dp))
        }
        val weekly = DailyQuestManager.weeklyQuests
        if (weekly.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp)).background(colors.surface)
                        .padding(16.dp), contentAlignment = Alignment.Center
                ) { Text("Laden...", color = colors.textSecondary, fontSize = 13.sp) }
            }
        } else {
            items(weekly, key = { it.quest.id }) { state ->
                DailyQuestCard(state, colors) {
                    val earned = DailyQuestManager.claimReward(context, state.quest.id)
                    if (earned > 0) Toast.makeText(context, "+$earned XP kassiert!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // ── Trennlinie vor Meilensteinen
        item {
            Spacer(Modifier.height(8.dp))
            Divider(color = colors.surfaceVariant)
            Spacer(Modifier.height(8.dp))
            Text("MEILENSTEINE", color = colors.textSecondary, fontSize = 11.sp,
                letterSpacing = 3.sp, fontWeight = FontWeight.Bold)
        }

        // ── Bestehende Milestone-Quests
        val grouped = ALL_QUESTS.groupBy { it.category }
        grouped.forEach { (category, quests) ->
            item {
                Spacer(Modifier.height(4.dp))
                Text(category.uppercase(), color = colors.accent, fontSize = 10.sp,
                    letterSpacing = 3.sp, fontWeight = FontWeight.Bold)
            }
            items(quests) { quest ->
                val progress = when {
                    quest.id.startsWith("explore") -> profile.exploredCount
                    quest.id.startsWith("dungeon") -> profile.visitedDungeons
                    quest.id.startsWith("lost")    -> 0
                    quest.id.startsWith("xp")      -> profile.xp
                    else -> 0
                }
                QuestCard(quest, progress, progress >= quest.targetCount, colors)
            }
        }
    }
}
