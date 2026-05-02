package com.sad.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.sad.app.data.PlayerProfile

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
    Quest("explore_10", "Stadtläufer", "Erkunde 10 verschiedene Bereiche", "◈", 10, 200, "Erkundung"),
    Quest("explore_50", "Kartograph", "Erkunde 50 verschiedene Bereiche", "⬡", 50, 500, "Erkundung"),
    Quest("explore_100", "Stadtkenner", "Erkunde 100 Bereiche", "⬢", 100, 1000, "Erkundung"),
    Quest("explore_500", "Vagabund", "Erkunde 500 Bereiche", "⎔", 500, 2500, "Erkundung"),
    Quest("explore_1000", "Urban Explorer", "Erkunde 1.000 Bereiche", "⏣", 1000, 5000, "Erkundung"),
    Quest("explore_5000", "Eroberer der Stadt", "Erkunde 5.000 Bereiche", "◩", 5000, 20000, "Erkundung"),
    Quest("explore_10000", "Das wandelnde Lexikon", "Erkunde unfassbare 10.000 Bereiche", "◪", 10000, 50000, "Erkundung"),
    
    Quest("dungeon_1", "Dungeon-Einsteiger", "Besuche deinen ersten Dungeon", "◬", 1, 100, "Dungeons"),
    Quest("dungeon_5", "Dungeon-Läufer", "Besuche 5 Dungeons", "▲", 5, 300, "Dungeons"),
    Quest("dungeon_20", "Dungeon-Meister", "Besuche 20 Dungeons", "▼", 20, 800, "Dungeons"),
    Quest("dungeon_50", "Dungeon-Lord", "Besuche 50 Dungeons", "⚔", 50, 2000, "Dungeons"),
    Quest("dungeon_100", "Schattengänger", "Betritt 100 Dungeons", "☣", 100, 5000, "Dungeons"),
    Quest("dungeon_500", "König des Untergrunds", "Betritt 500 Dungeons", "☢", 500, 25000, "Dungeons"),
    Quest("dungeon_1000", "Cyber-Gott", "Betritt 1.000 Dungeons", "☠", 1000, 100000, "Dungeons"),
    
    Quest("lost_1", "Verlorene Seele", "Entdecke einen Lost Place", "∅", 1, 200, "Lost Places"),
    Quest("lost_5", "Ruinenjäger", "Entdecke 5 Lost Places", "⊘", 5, 600, "Lost Places"),
    Quest("lost_20", "Geisterflüsterer", "Entdecke 20 Lost Places", "⊗", 20, 2500, "Lost Places"),
    Quest("lost_50", "Reliktjäger", "Entdecke 50 Lost Places", "⍚", 50, 10000, "Lost Places"),
    
    Quest("xp_500", "Aufsteiger", "Sammle 500 XP", "★", 500, 100, "Profil"),
    Quest("xp_2000", "Veteran", "Sammle 2.000 XP", "✦", 2000, 300, "Profil"),
    Quest("xp_10000", "Legende", "Sammle 10.000 XP", "✧", 10000, 1500, "Profil"),
    Quest("xp_50000", "Halbgott", "Sammle 50.000 XP", "✪", 50000, 5000, "Profil"),
    Quest("xp_250000", "Mythos", "Sammle 250.000 XP", "❂", 250000, 50000, "Profil"),
)

@Composable
fun QuestScreen(refreshKey: Int = 0) {
    val context = LocalContext.current
    val profile by produceState(initialValue = PlayerProfile.load(context), refreshKey) {
        value = PlayerProfile.load(context)
    }
    
    val bg = Color(0xFF0A0A12)
    val cyan = Color(0xFF00F3FF)
    val pink = Color(0xFFFF00E6)
    val gold = Color(0xFFFFD700)
    val surface = Color(0xFF111122)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            // Header
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("QUEST LOG", color = cyan, fontSize = 11.sp, letterSpacing = 4.sp)
                Text("Aufträge", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(4.dp))
                Text("Fortschritt: ${profile.exploredCount} Bereiche erkundet • ${profile.visitedDungeons} Dungeons", 
                     color = Color.Gray, fontSize = 13.sp)
            }
        }

        val grouped = ALL_QUESTS.groupBy { it.category }
        grouped.forEach { (category, quests) ->
            item {
                Spacer(Modifier.height(8.dp))
                Text(category.uppercase(), color = pink, fontSize = 11.sp, letterSpacing = 3.sp,
                     fontWeight = FontWeight.Bold)
            }
            items(quests) { quest ->
                val progress = when {
                    quest.id.startsWith("explore") -> profile.exploredCount
                    quest.id.startsWith("dungeon") -> profile.visitedDungeons
                    quest.id.startsWith("lost")    -> 0 // TODO
                    quest.id.startsWith("xp")      -> profile.xp
                    else -> 0
                }
                val completed = progress >= quest.targetCount
                QuestCard(quest, progress, completed, cyan, gold, surface)
            }
        }
    }
}

@Composable
fun QuestCard(quest: Quest, progress: Int, completed: Boolean, cyan: Color, gold: Color, surface: Color) {
    val borderColor = if (completed) gold else Color(0xFF223344)
    val progressFraction = (progress.toFloat() / quest.targetCount).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(surface)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.width(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = quest.icon,
                        fontSize = 24.sp,
                        color = if (completed) gold else cyan,
                        fontWeight = FontWeight.Light
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(quest.title, color = if (completed) gold else Color.White,
                         fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(quest.description, color = Color.Gray, fontSize = 12.sp)
                }
                if (completed) {
                    Text("✓", color = gold, fontSize = 22.sp, fontWeight = FontWeight.Black)
                } else {
                    Text("+${quest.xpReward} XP", color = cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (!completed) {
                Spacer(Modifier.height(10.dp))
                // Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF223344))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressFraction)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp))
                            .background(cyan)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text("${progress.coerceAtMost(quest.targetCount)} / ${quest.targetCount}",
                     color = Color.Gray, fontSize = 11.sp)
            }
        }
    }
}
