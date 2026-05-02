package com.sad.app.ui

import android.content.Intent
import androidx.compose.foundation.background
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import java.util.Calendar
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sad.app.data.PlayerProfile

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val rarity: String // "common", "rare", "epic", "legendary"
)

val ALL_ACHIEVEMENTS = listOf(
    // EXPLORER (Granular)
    Achievement("first_steps", "Erste Schritte", "Erkunde deinen ersten Bereich", "◇", "common"),
    Achievement("explorer_10", "Pfadfinder", "Erkunde 10 Bereiche", "◈", "common"),
    Achievement("explorer_50", "Wanderer", "Erkunde 50 Bereiche", "▣", "common"),
    Achievement("explorer_100", "Kartograph", "Erkunde 100 Bereiche", "⬢", "rare"),
    Achievement("explorer_250", "Grenzläufer", "Erkunde 250 Bereiche", "⬣", "rare"),
    Achievement("explorer_500", "Weltenbummler", "Erkunde 500 Bereiche", "⎔", "epic"),
    Achievement("explorer_750", "Vagabund", "Erkunde 750 Bereiche", "⍟", "epic"),
    Achievement("explorer_1000", "Stadtführer", "Erkunde 1.000 Bereiche", "⏣", "epic"),
    Achievement("explorer_2500", "Meister-Erkunder", "Erkunde 2.500 Bereiche", "◩", "legendary"),
    Achievement("explorer_5000", "Eroberer der Stadt", "Erkunde 5.000 Bereiche", "◪", "legendary"),
    Achievement("explorer_7500", "Urbaner Nomad", "Erkunde 7.500 Bereiche", "◫", "legendary"),
    Achievement("explorer_10000", "Das wandelnde Lexikon", "Erkunde 10.000 Bereiche", "◬", "legendary"),
    Achievement("explorer_25000", "Omnipräsent", "Erkunde 25.000 Bereiche", "⌬", "legendary"),
    Achievement("explorer_50000", "Eins mit der Stadt", "Erkunde 50.000 Bereiche", "♾", "legendary"),

    // DUNGEONS (Granular)
    Achievement("dungeon_1", "Dungeon-Taucher", "Betrete deinen ersten Dungeon", "▽", "common"),
    Achievement("dungeon_5", "Kämpfer", "Bezwinge 5 Dungeons", "▼", "common"),
    Achievement("dungeon_10", "Dungeon-Meister", "Bezwinge 10 Dungeons", "△", "rare"),
    Achievement("dungeon_25", "Veteranen-Looter", "Bezwinge 25 Dungeons", "▲", "rare"),
    Achievement("dungeon_50", "Dungeon-Lord", "Bezwinge 50 Dungeons", "⚔", "epic"),
    Achievement("dungeon_75", "Schatzsucher", "Bezwinge 75 Dungeons", "❂", "epic"),
    Achievement("dungeon_100", "Der Unaufhaltsame", "Bezwinge 100 Dungeons", "☢", "epic"),
    Achievement("dungeon_250", "Kataster-König", "Bezwinge 250 Dungeons", "☣", "legendary"),
    Achievement("dungeon_500", "König des Untergrunds", "Bezwinge 500 Dungeons", "☠", "legendary"),
    Achievement("dungeon_750", "Dungeon-Vernichter", "Bezwinge 750 Dungeons", "🗲", "legendary"),
    Achievement("dungeon_1000", "Cyber-Gott", "Bezwinge 1.000 Dungeons", "⍚", "legendary"),
    Achievement("dungeon_5000", "Unsterblichkeit", "Bezwinge 5.000 Dungeons", "◈", "legendary"),

    // LEVEL (Granular)
    Achievement("level_5", "Frischling", "Erreiche Level 5", "⌂", "common"),
    Achievement("level_10", "Adept", "Erreiche Level 10", "✦", "common"),
    Achievement("level_20", "Stadtlegende", "Erreiche Level 20", "✧", "rare"),
    Achievement("level_30", "Experte", "Erreiche Level 30", "✪", "rare"),
    Achievement("level_40", "Profi", "Erreiche Level 40", "✫", "rare"),
    Achievement("level_50", "Halbgott", "Erreiche Level 50", "★", "epic"),
    Achievement("level_75", "Heros", "Erreiche Level 75", "✮", "epic"),
    Achievement("level_100", "Mythos", "Erreiche Level 100", "✯", "legendary"),
    Achievement("level_200", "Titan", "Erreiche Level 200", "✵", "legendary"),
    Achievement("level_300", "Entität", "Erreiche Level 300", "✸", "legendary"),
    Achievement("level_400", "Weltenwächter", "Erreiche Level 400", "✹", "legendary"),
    Achievement("level_500", "Gott-Status", "Erreiche Level 500", "❂", "legendary"),
    Achievement("level_1000", "The Arch-Cyber", "Erreiche Level 1000", "♕", "legendary"),

    // SPECIAL / SOCIAL
    Achievement("rumor_1", "Zuhörer", "Erhalte dein erstes Gerücht via P2P", "◖", "common"),
    Achievement("rumor_10", "Informant", "Erhalte 10 Gerüchte via P2P", "◗", "rare"),
    Achievement("rumor_50", "Netzwerker", "Erhalte 50 Gerüchte via P2P", "🕸", "epic"),
    Achievement("rumor_100", "Das Orakel", "Erhalte 100 Gerüchte via P2P", "👁", "legendary"),
    
    Achievement("lost_place", "Verlorene Welt", "Entdecke einen Lost Place", "∅", "epic"),
    Achievement("lost_50", "Reliktjäger", "Entdecke 50 Lost Places", "⍚", "legendary"),
    Achievement("night_owl", "Nachteule", "Erkunde 5 Bereiche zwischen 23:00 und 04:00 Uhr", "☽", "rare"),
    Achievement("early_bird", "Frühaufsteher", "Erkunde 5 Bereiche zwischen 05:00 und 08:00 Uhr", "☼", "rare"),
    Achievement("speed_run", "Speed Runner", "Erkunde 5 Orte in einer Stunde", "⎋", "epic"),
    Achievement("social", "Teiler", "Teile deine Erkundungen", "⎗", "common"),

    // SECRET ACHIEVEMENTS (Versteckt)
    Achievement("hacker", "Hacker im System", "Wo ist es?", "⧉", "legendary"),
    Achievement("teleporter", "Glitch in der Matrix", "Krümme Raum und Zeit", "⧗", "legendary"),
    Achievement("godmode", "Göttliche Sicht", "Deaktiviere den Nebel der Welt", "⌬", "legendary"),
)

@Composable
fun AchievementsScreen(onRefreshRequested: () -> Unit = {}) {
    val context = LocalContext.current
    var secretTaps by remember { mutableStateOf(0) }
    var showSecretMenu by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    val correctPin = "8437"

    var internalRefreshTrigger by remember { mutableStateOf(0) }
    val profile by produceState(initialValue = PlayerProfile.load(context), internalRefreshTrigger) {
        // Jedes Mal wenn der Screen sichtbar wird oder der Trigger steigt, Profil neu laden
        value = PlayerProfile.load(context)
    }

    val prefs = remember { context.getSharedPreferences("player_profile", android.content.Context.MODE_PRIVATE) }
    var isDevModeUnlocked by remember { mutableStateOf(prefs.getBoolean("is_dev_mode_unlocked", false)) }


    val bg = Color(0xFF0A0A12)
    val cyan = Color(0xFF00F3FF)
    val pink = Color(0xFFFF00E6)
    val gold = Color(0xFFFFD700)
    val surface = Color(0xFF111122)

    // Welche Achievements sind freigeschaltet?
    val unlocked = remember(profile) {
        val list = mutableSetOf<String>()
        
        // EXPLORER
        if (profile.exploredCount >= 1) list.add("first_steps")
        if (profile.exploredCount >= 10) list.add("explorer_10")
        if (profile.exploredCount >= 50) list.add("explorer_50")
        if (profile.exploredCount >= 100) list.add("explorer_100")
        if (profile.exploredCount >= 250) list.add("explorer_250")
        if (profile.exploredCount >= 500) list.add("explorer_500")
        if (profile.exploredCount >= 750) list.add("explorer_750")
        if (profile.exploredCount >= 1000) list.add("explorer_1000")
        if (profile.exploredCount >= 2500) list.add("explorer_2500")
        if (profile.exploredCount >= 5000) list.add("explorer_5000")
        if (profile.exploredCount >= 7500) list.add("explorer_7500")
        if (profile.exploredCount >= 10000) list.add("explorer_10000")
        if (profile.exploredCount >= 25000) list.add("explorer_25000")
        if (profile.exploredCount >= 50000) list.add("explorer_50000")

        // DUNGEONS
        if (profile.visitedDungeons >= 1) list.add("dungeon_1")
        if (profile.visitedDungeons >= 5) list.add("dungeon_5")
        if (profile.visitedDungeons >= 10) list.add("dungeon_10")
        if (profile.visitedDungeons >= 25) list.add("dungeon_25")
        if (profile.visitedDungeons >= 50) list.add("dungeon_50")
        if (profile.visitedDungeons >= 75) list.add("dungeon_75")
        if (profile.visitedDungeons >= 100) list.add("dungeon_100")
        if (profile.visitedDungeons >= 250) list.add("dungeon_250")
        if (profile.visitedDungeons >= 500) list.add("dungeon_500")
        if (profile.visitedDungeons >= 750) list.add("dungeon_750")
        if (profile.visitedDungeons >= 1000) list.add("dungeon_1000")
        if (profile.visitedDungeons >= 5000) list.add("dungeon_5000")

        // LEVELS
        if (profile.level >= 5) list.add("level_5")
        if (profile.level >= 10) list.add("level_10")
        if (profile.level >= 20) list.add("level_20")
        if (profile.level >= 30) list.add("level_30")
        if (profile.level >= 40) list.add("level_40")
        if (profile.level >= 50) list.add("level_50")
        if (profile.level >= 75) list.add("level_75")
        if (profile.level >= 100) list.add("level_100")
        if (profile.level >= 200) list.add("level_200")
        if (profile.level >= 300) list.add("level_300")
        if (profile.level >= 400) list.add("level_400")
        if (profile.level >= 500) list.add("level_500")
        if (profile.level >= 1000) list.add("level_1000")

        // SPECIAL
        val prefs = context.getSharedPreferences("player_profile", android.content.Context.MODE_PRIVATE)
        
        // Rumors
        val receivedRumors = prefs.getInt("received_rumors_count", 0)
        if (receivedRumors >= 1) list.add("rumor_1")
        if (receivedRumors >= 10) list.add("rumor_10")
        if (receivedRumors >= 50) list.add("rumor_50")
        if (receivedRumors >= 100) list.add("rumor_100")

        if (prefs.getBoolean("has_shared", false)) list.add("social")

        val isTimeHack = prefs.getBoolean("dev_time_hack", false)
        if (profile.nightExploredCount >= 5 || isTimeHack) list.add("night_owl")
        if (profile.morningExploredCount >= 5 || isTimeHack) list.add("early_bird")

        // Secret Unlocks
        if (prefs.getBoolean("is_dev_mode_unlocked", false)) list.add("hacker")
        if (prefs.getBoolean("dev_gods_eye", false)) list.add("godmode")
        if (prefs.getFloat("lat_offset", 0f) != 0f || prefs.getFloat("lon_offset", 0f) != 0f) list.add("teleporter")
        
        list.addAll(profile.unlockedAchievements)
        list
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("ACHIEVEMENTS", color = cyan, fontSize = 11.sp, letterSpacing = 4.sp)
                        Text(
                            text = "Errungenschaften",
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                secretTaps++
                                if (secretTaps >= 7) {
                                    if (isDevModeUnlocked) {
                                        isDevModeUnlocked = false
                                        prefs.edit().putBoolean("is_dev_mode_unlocked", false).apply()
                                    } else {
                                        showPinDialog = true
                                    }
                                    secretTaps = 0
                                }
                            }
                        )
                    }

                    if (isDevModeUnlocked) {
                        Surface(
                            onClick = { showSecretMenu = true },
                            color = pink.copy(alpha = 0.1f),
                            shape = androidx.compose.foundation.shape.CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, pink.copy(alpha = 0.6f)),
                            modifier = Modifier.height(32.dp).padding(start = 12.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
                                Text("DEV_OVERRIDE", color = pink, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))

                // Stats Übersicht
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatChip("${profile.level}", "Level", gold, Modifier.weight(1f))
                    StatChip("${profile.xp}", "XP", cyan, Modifier.weight(1f))
                    StatChip("${unlocked.size}/${ALL_ACHIEVEMENTS.size}", "Achievements", pink, Modifier.weight(1f))
                }

                Spacer(Modifier.height(12.dp))

                // XP Progress Bar bis zum nächsten Level
                val xpForThisLevel = (profile.level - 1) * 500
                val xpForNextLevel = profile.level * 500
                val levelProgress = ((profile.xp - xpForThisLevel).toFloat() / 500f).coerceIn(0f, 1f)
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Level ${profile.level} · ${profile.title}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("${profile.xp} / ${xpForNextLevel} XP", color = Color.Gray, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF1A1A2E))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(levelProgress)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(3.dp))
                                .background(Brush.horizontalGradient(listOf(cyan, pink)))
                        )
                    }
                }
            }
        }

        items(ALL_ACHIEVEMENTS) { achievement ->
            val isUnlocked = achievement.id in unlocked
            AchievementCard(achievement, isUnlocked, surface, gold, cyan)
        }

        item {
            Spacer(Modifier.height(20.dp))
            // Share Button jetzt ganz unten
            val shareText = "--- SYSTEM LOG: CITY AS A DUNGEON ---\n" +
                "ID: USER_${profile.title.uppercase()}_${profile.level}\n" +
                "LEVEL: ${profile.level} [${profile.title}]\n" +
                "EXPLORED: ${profile.exploredCount} NODES\n" +
                "DUNGEONS: ${profile.visitedDungeons} CLEARED\n" +
                "RUMORS: ${prefs.getInt("received_rumors_count", 0)} SYNCED\n" +
                "ACHIEVEMENTS: ${unlocked.size}/${ALL_ACHIEVEMENTS.size}\n" +
                "------------------------------------\n" +
                "Join the network: #CityAsADungeon #SAD"

            Button(
                onClick = {
                    prefs.edit().putBoolean("has_shared", true).apply()
                    internalRefreshTrigger++
                    onRefreshRequested()
                    
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(intent, "TERMINAL BROADCAST"))
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A2E)),
                shape = androidx.compose.foundation.shape.CutCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, cyan.copy(alpha = 0.4f))
            ) {
                Text("📤 FORTSCHRITT TEILEN", color = cyan, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, letterSpacing = 2.sp)
            }
        }
    }



    if (showSecretMenu) {
        val coroutineScope = rememberCoroutineScope()
        val gameDb = remember { com.sad.app.data.GameDatabase.getDatabase(context) }
        
        AlertDialog(
            onDismissRequest = { showSecretMenu = false },
            containerColor = surface,
            shape = RoundedCornerShape(16.dp),
            title = { 
                Text("Dev-Menü", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // --- SECTION: Ressourcen ---
                    DevSectionTitle("Ressourcen")
                    
                    var xpInput by remember { mutableStateOf("10000") }
                    var areaInput by remember { mutableStateOf("100") }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DevTextField(value = xpInput, label = "XP hinzufügen", color = cyan) { xpInput = it }
                        Button(
                            onClick = { 
                                val amount = xpInput.toIntOrNull() ?: 0
                                PlayerProfile.addXP(context, amount)
                                internalRefreshTrigger++
                                onRefreshRequested()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = cyan.copy(alpha = 0.1f)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) { Text("XP Injektion", color = cyan) }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DevTextField(value = areaInput, label = "Gebiete hinzufügen", color = gold) { areaInput = it }
                        Button(
                            onClick = { 
                                val amount = areaInput.toIntOrNull() ?: 0
                                coroutineScope.launch(Dispatchers.IO) {
                                    val prefs = context.getSharedPreferences("player_profile", android.content.Context.MODE_PRIVATE)
                                    prefs.edit().putInt("explored_count", prefs.getInt("explored_count", 0) + amount).apply()
                                    withContext(Dispatchers.Main) {
                                        internalRefreshTrigger++
                                        onRefreshRequested()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = gold.copy(alpha = 0.1f)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) { Text("Gebiete synchronisieren", color = gold) }
                    }

                    // --- SECTION: Cheats ---
                    DevSectionTitle("Einstellungen & Cheats")
                    
                    val prefs = remember { context.getSharedPreferences("player_profile", android.content.Context.MODE_PRIVATE) }
                    var godsEye by remember { mutableStateOf(prefs.getBoolean("dev_gods_eye", false)) }
                    var magnetMode by remember { mutableStateOf(prefs.getBoolean("dev_magnet_mode", false)) }
                    var timeHack by remember { mutableStateOf(prefs.getBoolean("dev_time_hack", false)) }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        DevSwitch("God's Eye (Nebel-Bypass)", godsEye, cyan) {
                            godsEye = it
                            prefs.edit().putBoolean("dev_gods_eye", it).apply()
                        }
                        DevSwitch("Magnet-Modus (500m Loot)", magnetMode, gold) {
                            magnetMode = it
                            prefs.edit().putBoolean("dev_magnet_mode", it).apply()
                        }
                        DevSwitch("Time-Hacker (Always Night)", timeHack, pink) {
                            timeHack = it
                            prefs.edit().putBoolean("dev_time_hack", it).apply()
                        }
                    }

                    // --- SECTION: GPS Teleport ---
                    DevSectionTitle("GPS Teleport")
                    Text("Offset anpassen: 0.001 ≈ 110m", color = Color.Gray, fontSize = 11.sp)
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("N ↑" to 1, "S ↓" to 2, "O →" to 3, "W ←" to 4).forEach { (label, dir) ->
                            Button(
                                onClick = { 
                                    when(dir) {
                                        1 -> prefs.edit().putFloat("lat_offset", prefs.getFloat("lat_offset", 0f) + 0.005f).apply()
                                        2 -> prefs.edit().putFloat("lat_offset", prefs.getFloat("lat_offset", 0f) - 0.005f).apply()
                                        3 -> prefs.edit().putFloat("lon_offset", prefs.getFloat("lon_offset", 0f) + 0.005f).apply()
                                        4 -> prefs.edit().putFloat("lon_offset", prefs.getFloat("lon_offset", 0f) - 0.005f).apply()
                                    }
                                },
                                modifier = Modifier.weight(1f).height(44.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) { Text(label, color = cyan, fontSize = 12.sp) }
                        }
                    }
                    
                    Button(
                        onClick = { prefs.edit().putFloat("lat_offset", 0f).putFloat("lon_offset", 0f).apply() },
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(8.dp)
                    ) { Text("GPS Offset zurücksetzen", color = Color.Gray, fontSize = 12.sp) }

                    Spacer(Modifier.height(16.dp))
                    
                    // --- SECTION: Reset ---
                    Button(
                        onClick = {
                            coroutineScope.launch(Dispatchers.IO) {
                                // 1. Alle Datenbanken physisch loeschen (game.db, places.db etc.)
                                context.databaseList().forEach { dbName ->
                                    context.deleteDatabase(dbName)
                                }
                                
                                // 2. Alle SharedPreferences vernichten
                                val sharedPrefsDir = java.io.File(context.applicationInfo.dataDir, "shared_prefs")
                                if (sharedPrefsDir.exists()) {
                                    sharedPrefsDir.listFiles()?.forEach { it.delete() }
                                }
                                
                                // 3. Cache Ordner leeren (OSMDroid Tiles etc.)
                                context.cacheDir.deleteRecursively()

                                withContext(Dispatchers.Main) {
                                    // Sauberer Neustart: Beendet alle Instanzen und laedt alles neu
                                    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                                    intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                    intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(intent)
                                    Runtime.getRuntime().exit(0)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) { Text("ALLE DATEN LÖSCHEN", color = Color.Red, fontSize = 12.sp) }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSecretMenu = false }) { 
                    Text("SCHLIESSEN", color = Color.White.copy(alpha = 0.5f)) 
                }
            }
        )
    }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false; pinInput = "" },
            containerColor = surface,
            shape = RoundedCornerShape(16.dp),
            title = { Text("Authentifizierung", color = Color.White) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Bitte PIN eingeben", color = Color.Gray, fontSize = 14.sp)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 4) pinInput = it },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(140.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center, fontSize = 24.sp, color = cyan, letterSpacing = 8.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = cyan,
                            unfocusedBorderColor = cyan.copy(alpha = 0.5f)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinInput == correctPin) {
                            isDevModeUnlocked = true
                            prefs.edit().putBoolean("is_dev_mode_unlocked", true).apply()
                            showPinDialog = false
                            pinInput = ""
                        } else {
                            pinInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = cyan),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("Bestätigen") }
            }
        )
    }
}

@Composable
fun DevSectionTitle(title: String) {
    Column(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
        Text(title, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Divider(color = Color.White.copy(alpha = 0.1f))
    }
}

@Composable
fun DevTextField(value: String, label: String, color: Color, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.all { char -> char.isDigit() }) onValueChange(it) },
        label = { Text(label, color = color.copy(alpha = 0.7f), fontSize = 10.sp) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = Color.White),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = color,
            unfocusedBorderColor = color.copy(alpha = 0.3f),
            focusedContainerColor = color.copy(alpha = 0.05f)
        )
    )
}

@Composable
fun DevSwitch(label: String, checked: Boolean, color: Color, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.White, fontSize = 12.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = color,
                checkedTrackColor = color.copy(alpha = 0.3f),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color.Transparent
            )
        )
    }
}

@Composable
fun StatChip(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF111122))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = color, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Text(label, color = Color.Gray, fontSize = 11.sp)
        }
    }
}

@Composable
fun AchievementCard(achievement: Achievement, isUnlocked: Boolean, surface: Color, gold: Color, cyan: Color) {
    val rarityColor = when (achievement.rarity) {
        "legendary" -> Color(0xFFFFD700)
        "epic"      -> Color(0xFFFF00E6)
        "rare"      -> Color(0xFF00F3FF)
        else        -> Color(0xFF888888)
    }

    val alpha = if (isUnlocked) 1f else 0.35f
    val borderColor = if (isUnlocked) rarityColor.copy(alpha = 0.7f) else Color(0xFF1A1A2E)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(surface.copy(alpha = alpha))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon Kreis
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(if (isUnlocked) rarityColor.copy(alpha = 0.15f) else Color(0xFF0D0D1A)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (isUnlocked) achievement.icon else "[x]",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = if (isUnlocked) rarityColor else Color.DarkGray,
                letterSpacing = 1.sp
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(achievement.title,
                     color = if (isUnlocked) Color.White else Color.Gray,
                     fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.width(8.dp))
                // Rarity Badge
                if (achievement.rarity != "common") {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(rarityColor.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(achievement.rarity.uppercase(), color = rarityColor, fontSize = 9.sp, letterSpacing = 1.sp)
                    }
                }
            }
            Text(achievement.description,
                 color = if (isUnlocked) Color.Gray else Color(0xFF333355),
                 fontSize = 12.sp)
        }

        if (isUnlocked) {
            Text("✓", color = rarityColor, fontSize = 20.sp, fontWeight = FontWeight.Black)
        }
    }
}
