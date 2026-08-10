package com.sad.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sad.app.data.AddonManager
import com.sad.app.data.AddonMeta
import com.sad.app.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AddonScreen() {
    val context = LocalContext.current
    val colors = LocalAppColors.current
    val coroutineScope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }

    var showImportDialog by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var importNameInput by remember { mutableStateOf("") }

    // Launcher zum Importieren (JSON / DB)
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingImportUri = uri
            importNameInput = ""
            showImportDialog = true
        }
    }

    // Launcher zum Exportieren
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                val places = db.placeDao().getPlacesInArea(-90.0, 90.0, -180.0, 180.0)
                val result = AddonManager.exportPlacesToJson(context, places, uri)
                withContext(Dispatchers.Main) {
                    result.onSuccess { count ->
                        Toast.makeText(context, "✅ $count Orte als Addon exportiert!", Toast.LENGTH_LONG).show()
                    }.onFailure { err ->
                        Toast.makeText(context, "❌ Export-Fehler: ${err.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Header
        item {
            Column {
                Text("MODDING & COMMUNITY", color = colors.primary, fontSize = 11.sp, letterSpacing = 4.sp)
                Text("Addon Hub", color = colors.textPrimary, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(4.dp))
                Text("Erstelle und teile eigene Dungeon-Packs, Stories und Custom Routes.", color = colors.textSecondary, fontSize = 13.sp)
            }
        }

        // ── SECTION 1: MODDING GUIDE ──────────────────────────────────────────
        item {
            ModdingGuideSection(colors)
        }

        // ── SECTION 2: ADDON VERWALTUNG ───────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("INSTALLIERTE PACKS", color = colors.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Button(
                    onClick = { importLauncher.launch(arrayOf("application/json", "application/octet-stream", "*/*")) },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.accent)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("IMPORTIEREN", color = colors.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (AddonManager.installedAddons.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surface)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Extension, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Keine Addons installiert", color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text("Klicke oben auf IMPORTIEREN um ein Pack hinzuzufügen.", color = colors.textSecondary, fontSize = 12.sp)
                    }
                }
            }
        } else {
            items(AddonManager.installedAddons) { addon ->
                AddonItemCard(addon, colors) {
                    AddonManager.removeAddon(context, addon.id)
                    Toast.makeText(context, "Addon gelöscht.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // ── SECTION 3: CREATOR EXPORTER ───────────────────────────────────────
        item {
            Spacer(Modifier.height(4.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.gold.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("COMMUNITY CREATOR", color = colors.gold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Eigenes Addon exportieren", color = colors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("Exportiere alle Dungeons aus der lokalen Datenbank als wiederverwendbares Addon-Paket für deine Community.", color = colors.textSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { exportLauncher.launch("sad_addon_${System.currentTimeMillis() / 1000}.json") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.gold.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.gold)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = colors.gold, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("ALS JSON ADDON EXPORTIEREN", color = colors.gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Namensdialog beim Import
    if (showImportDialog && pendingImportUri != null) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("ADDON IMPORTIEREN", color = colors.primary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Gib einen Namen für das Paket ein:", color = colors.textPrimary, fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = importNameInput,
                        onValueChange = { importNameInput = it },
                        placeholder = { Text("z.B. Braunschweig Lost Places", color = colors.textSecondary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.surfaceVariant,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uri = pendingImportUri
                        if (uri != null) {
                            val res = AddonManager.importAddon(context, uri, importNameInput)
                            res.onSuccess { meta ->
                                Toast.makeText(context, "✅ '${meta.name}' mit ${meta.placeCount} Orten geladen!", Toast.LENGTH_LONG).show()
                            }.onFailure { err ->
                                Toast.makeText(context, "❌ Fehler: ${err.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                        showImportDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) {
                    Text("IMPORTIEREN", color = colors.bg, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("ABBRECHEN", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface
        )
    }
}

@Composable
fun ModdingGuideSection(colors: AppColors) {
    val context = LocalContext.current

    // Einzelne Akkordeon-Sektionen
    var expandedSection by remember { mutableStateOf<String?>(null) }

    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.primary.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Code, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("MODDING GUIDE", color = colors.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Spacer(Modifier.width(6.dp))
                Text("· JSON-Format, Pflicht: lat, lon, name", color = colors.textSecondary, fontSize = 11.sp)
            }
            Spacer(Modifier.height(10.dp))

            // KI-Master-Prompt Button
            val masterPrompt = buildAiMasterPrompt()
            Button(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("SAD Addon KI-Prompt", masterPrompt))
                    Toast.makeText(context, "KI-Prompt kopiert!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = colors.gold.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.gold.copy(alpha = 0.7f)),
                contentPadding = PaddingValues(vertical = 8.dp, horizontal = 12.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = colors.gold, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("KI-PROMPT KOPIEREN", color = colors.gold, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    Text("Vollstaendiger Kontext fuer ChatGPT / Claude / Gemini", color = colors.gold.copy(alpha = 0.7f), fontSize = 10.sp)
                }
            }

            Spacer(Modifier.height(10.dp))

            // Akkordeon 1: Pflichtfelder
            GuideAccordion(
                title = "Pflicht-Felder",
                subtitle = "Minimal-Setup: name, lat, lon",
                isExpanded = expandedSection == "fields",
                colors = colors,
                accentColor = colors.primary,
                onClick = { expandedSection = if (expandedSection == "fields") null else "fields" }
            ) {
                val minimalJson = """{
  "name": "Verlassenes Kraftwerk",
  "lat": 52.2688,
  "lon": 10.5268
}"""
                GuideCodeBlock(minimalJson, colors)
                Spacer(Modifier.height(8.dp))
                GuideFieldRow("name", "String", "Name des Ortes auf der Karte", colors)
                GuideFieldRow("lat", "Double", "Breitengrad in Dezimalgrad z.B. 52.2688", colors)
                GuideFieldRow("lon", "Double", "Laengengrad in Dezimalgrad z.B. 10.5268", colors)
                Spacer(Modifier.height(6.dp))
                CopyButton(minimalJson, context, colors)
            }

            Spacer(Modifier.height(6.dp))

            // Akkordeon 2: Kategorien & Raritaet
            GuideAccordion(
                title = "Raritaet & Kategorie",
                subtitle = "Markerfarbe, Dungeon-Typ, Sichtbarkeit",
                isExpanded = expandedSection == "rarity",
                colors = colors,
                accentColor = colors.accent,
                onClick = { expandedSection = if (expandedSection == "rarity") null else "rarity" }
            ) {
                val rarityJson = """{
  "name": "Verlassene Fabrik",
  "lat": 52.2688,
  "lon": 10.5268,
  "rarity": "epic",
  "category": "Lost Place",
  "type": "industrial"
}"""
                GuideCodeBlock(rarityJson, colors)
                Spacer(Modifier.height(8.dp))
                Text("Rarity-Werte:", color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.height(4.dp))
                RarityBadgeRow("common", "Grau", "Haeufig, wird bei weitem Zoom ausgeblendet", colors)
                RarityBadgeRow("uncommon", "Gruen", "Selten, immer sichtbar", colors)
                RarityBadgeRow("rare", "Gold", "Sehr selten, wird hervorgehoben", colors)
                RarityBadgeRow("epic", "Magenta", "Legendaer, groesster Marker", colors)
                Spacer(Modifier.height(8.dp))
                Text("Kategorien (Freitext):", color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Lost Place  Militaer  Industrie  Natur  Monument  Bunker  Ruine  Underground  Graffiti  Urban  Historic",
                    color = colors.textPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.height(6.dp))
                CopyButton(rarityJson, context, colors)
            }

            Spacer(Modifier.height(6.dp))

            // Akkordeon 3: Lore & Story
            GuideAccordion(
                title = "Lore & Beschreibungen",
                subtitle = "description, lore, questHint",
                isExpanded = expandedSection == "lore",
                colors = colors,
                accentColor = colors.gold,
                onClick = { expandedSection = if (expandedSection == "lore") null else "lore" }
            ) {
                val loreJson = """{
  "name": "Bunker 17",
  "lat": 52.2688,
  "lon": 10.5268,
  "rarity": "rare",
  "description": "Ein verlassener Kaltkrieg-Bunker am Stadtrand.",
  "lore": "Die Waende tragen noch Spuren der letzten Bewohner.\nKein Name, kein Datum. Nur Stille.",
  "questHint": "Suche nach dem Raum mit dem Metallschrank."
}"""
                GuideCodeBlock(loreJson, colors)
                Spacer(Modifier.height(8.dp))
                GuideFieldRow("description", "String", "Kurzbeschreibung 1-2 Saetze, sichtbar in der Ortsinfo", colors)
                GuideFieldRow("lore", "String", "Story-Fragment beim Entdecken. \\n fuer Zeilenumbrueche", colors)
                GuideFieldRow("questHint", "String", "Versteckter Hinweis fuer den Spieler", colors)
                Spacer(Modifier.height(6.dp))
                CopyButton(loreJson, context, colors)
            }

            Spacer(Modifier.height(6.dp))

            // Akkordeon 4: Gameplay
            GuideAccordion(
                title = "Gameplay-Mechaniken",
                subtitle = "xpReward, iconColor, minZoom",
                isExpanded = expandedSection == "gameplay",
                colors = colors,
                accentColor = colors.primary,
                onClick = { expandedSection = if (expandedSection == "gameplay") null else "gameplay" }
            ) {
                val gameplayJson = """{
  "name": "Geheimer Aussichtspunkt",
  "lat": 52.2688,
  "lon": 10.5268,
  "rarity": "uncommon",
  "xpReward": 250,
  "iconColor": "#FF8C00",
  "minZoom": 14.0
}"""
                GuideCodeBlock(gameplayJson, colors)
                Spacer(Modifier.height(8.dp))
                GuideFieldRow("xpReward", "Int", "XP beim Entdecken. Standard: common=50 uncommon=150 rare=300 epic=500", colors)
                GuideFieldRow("iconColor", "String", "Hex-Farbe des Markers z.B. #FF8C00 fuer Orange, #FF0000 fuer Rot", colors)
                GuideFieldRow("minZoom", "Double", "Marker erst ab diesem Zoom sichtbar. 10-12=weit weg 14-16=Nahbereich", colors)
                Spacer(Modifier.height(6.dp))
                CopyButton(gameplayJson, context, colors)
            }

            Spacer(Modifier.height(6.dp))

            // Akkordeon: Massen-Override / Alle Rot machen
            GuideAccordion(
                title = "Massen-Override (Alle Dungeons Rot)",
                subtitle = "override_all / iconColor fuer alle existierenden Orte",
                isExpanded = expandedSection == "override",
                colors = colors,
                accentColor = Color(0xFFFF0000),
                onClick = { expandedSection = if (expandedSection == "override") null else "override" }
            ) {
                val overrideJson = """[
  {
    "override_all": true,
    "iconColor": "#FF0000"
  }
]"""
                GuideCodeBlock(overrideJson, colors)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Fuer Massen-Anpassungen: Ein Addon mit 'override_all': true und 'iconColor': '#FF0000' ueberschreibt die Farben ALLER Dungeons auf der Karte mit Knallrot!",
                    color = colors.textPrimary,
                    fontSize = 11.sp
                )
                Spacer(Modifier.height(6.dp))
                CopyButton(overrideJson, context, colors)
            }

            Spacer(Modifier.height(6.dp))

            // Akkordeon 5: Vollbeispiel
            GuideAccordion(
                title = "Vollstaendiges Beispiel",
                subtitle = "Alle Features in einem 3-Ort-Pack",
                isExpanded = expandedSection == "full",
                colors = colors,
                accentColor = colors.accent,
                onClick = { expandedSection = if (expandedSection == "full") null else "full" }
            ) {
                val fullJson = """[
  {
    "osm_id": "lostplace_001",
    "name": "Verlassenes Kraftwerk Nord",
    "lat": 52.2688,
    "lon": 10.5268,
    "rarity": "epic",
    "category": "Lost Place",
    "type": "industrial",
    "description": "Stillgelegtes Kohlekraftwerk aus den 70ern.",
    "lore": "Der letzte Schichtwechsel war am 14. Maerz 1987.\nSeitdem laeuft nur noch die Zeit.",
    "questHint": "Finde den Maschinenraum im Untergeschoss.",
    "xpReward": 500,
    "iconColor": "#FF00E6",
    "minZoom": 12.0
  },
  {
    "osm_id": "bunker_002",
    "name": "Bunker 17",
    "lat": 52.2700,
    "lon": 10.5310,
    "rarity": "rare",
    "category": "Militaer",
    "type": "bunker",
    "description": "Kaltkrieg-Bunker, Mitte der 50er erbaut.",
    "lore": "Drei Meter Stahlbeton. Gebaut um alles zu ueberstehen.\nEs hat nichts ueberstanden.",
    "xpReward": 350,
    "iconColor": "#FFD700"
  },
  {
    "name": "Street Art Corridor",
    "lat": 52.2650,
    "lon": 10.5200,
    "rarity": "uncommon",
    "category": "Graffiti",
    "type": "urban"
  }
]"""
                GuideCodeBlock(fullJson, colors)
                Spacer(Modifier.height(6.dp))
                CopyButton(fullJson, context, colors)
            }

            Spacer(Modifier.height(6.dp))

            // Akkordeon 6: Tipps
            GuideAccordion(
                title = "Tipps fuer Addon-Ersteller",
                subtitle = "Koordinaten, IDs, Best Practices",
                isExpanded = expandedSection == "tips",
                colors = colors,
                accentColor = colors.gold,
                onClick = { expandedSection = if (expandedSection == "tips") null else "tips" }
            ) {
                val tips = listOf(
                    "1" to "Koordinaten via Google Maps: langer Tipp auf Karte, Dezimalgrad erscheinen oben.",
                    "2" to "OsmAnd oder Maps.me bieten Koordinaten fuer Offline-Bereiche.",
                    "3" to "osm_id eindeutig halten. Empfohlen: 'stadtname_typ_01'",
                    "4" to "Lore mit \\n umbrechen. Kurz und atmosphaerisch halten (2-3 Saetze).",
                    "5" to "Standard-XP: common=50, uncommon=150, rare=300, epic=500",
                    "6" to "iconColor als #RRGGBB oder #AARRGGBB fuer Transparenz.",
                    "7" to "minZoom: 10-12 fuer Weitansicht, 14-16 fuer Nahbereich-Dungeons.",
                    "8" to "Mehrere kleine Packs (pro Stadt) sind besser als ein grosses.",
                )
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    tips.forEach { (num, text) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(colors.bg)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(num, color = colors.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(text, color = colors.textSecondary, fontSize = 11.sp, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

fun buildAiMasterPrompt(): String = """
Du hilfst mir ein Addon fuer die Android-App "SAD" (Stealthed Area Discovery) zu erstellen.
SAD ist eine Erkundungs-App, in der Nutzer echte Orte (Dungeons) in ihrer Umgebung entdecken.

Das Addon-Format ist eine JSON-Datei (ein Array von Ort-Objekten).

=== PFLICHTFELDER ===
- name    (String)  Name des Ortes
- lat     (Double)  Breitengrad in Dezimalgrad
- lon     (Double)  Laengengrad in Dezimalgrad

=== OPTIONALE FELDER ===
- osm_id      (String)  Eindeutige ID, z.B. "braunschweig_bunker_01"
- rarity      (String)  Seltenheit: "common" | "uncommon" | "rare" | "epic"
- category    (String)  Freier Text, z.B. "Lost Place", "Militaer", "Bunker", "Graffiti"
- type        (String)  Freier Untertyp, z.B. "industrial", "urban", "bunker"
- description (String)  Kurzbeschreibung 1-2 Saetze
- lore        (String)  Atmosphaerischer Story-Text. \n fuer Zeilenumbrueche.
- questHint   (String)  Hinweis fuer den Spieler was er am Ort finden kann
- xpReward    (Int)     XP beim Entdecken. Defaults: common=50 uncommon=150 rare=300 epic=500
- iconColor   (String)  Marker-Farbe als Hex z.B. "#FF8C00" (Orange)
- minZoom     (Double)  Erst ab diesem Zoom sichtbar. 10-12=weit weg 14-16=Nahbereich

=== RARITY-BEDEUTUNG ===
common  = Grau,    haeufig, bei weitem Zoom ausgeblendet
uncommon= Gruen,   selten, immer sichtbar
rare    = Gold,    sehr selten, hervorgehoben
epic    = Magenta, legendaer, groesster Marker

=== BEISPIEL (3 Orte) ===
[
  {
    "osm_id": "bs_kraftwerk_001",
    "name": "Verlassenes Kraftwerk Nord",
    "lat": 52.2688,
    "lon": 10.5268,
    "rarity": "epic",
    "category": "Lost Place",
    "type": "industrial",
    "description": "Stillgelegtes Kohlekraftwerk aus den 70ern.",
    "lore": "Der letzte Schichtwechsel war am 14. Maerz 1987.\nSeitdem laeuft nur noch die Zeit.",
    "questHint": "Finde den Maschinenraum im Untergeschoss.",
    "xpReward": 500,
    "iconColor": "#FF00E6",
    "minZoom": 12.0
  },
  {
    "osm_id": "bs_bunker_002",
    "name": "Bunker 17",
    "lat": 52.2700,
    "lon": 10.5310,
    "rarity": "rare",
    "category": "Militaer",
    "type": "bunker",
    "description": "Kaltkrieg-Bunker, Mitte der 50er erbaut.",
    "lore": "Drei Meter Stahlbeton. Gebaut um alles zu ueberstehen.\nEs hat nichts ueberstanden.",
    "xpReward": 350,
    "iconColor": "#FFD700"
  },
  {
    "name": "Street Art Corridor",
    "lat": 52.2650,
    "lon": 10.5200,
    "rarity": "uncommon",
    "category": "Graffiti",
    "type": "urban"
  }
]

=== AUFGABE ===
Erstelle ein Addon-Pack fuer [DEINE STADT/REGION]. Suche reale interessante Orte (Lost Places, Bunker, historische Ruinen, Graffiti-Hotspots etc.) und erzeuge eine JSON-Datei mit mindestens 5-10 Orten. Verwende echte Koordinaten (Dezimalgrad). Schreibe atmosphaerische lore-Texte auf Deutsch.

Antworte nur mit dem fertigen JSON-Array, ohne Erklaerungen.
""".trimIndent()

@Composable
fun GuideAccordion(
    title: String,
    subtitle: String,
    isExpanded: Boolean,
    colors: AppColors,
    accentColor: Color,
    onClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, if (isExpanded) accentColor.copy(alpha = 0.5f) else colors.surfaceVariant, RoundedCornerShape(8.dp))
            .background(if (isExpanded) accentColor.copy(alpha = 0.05f) else colors.bg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = if (isExpanded) accentColor else colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                if (!isExpanded) {
                    Spacer(Modifier.width(6.dp))
                    Text(subtitle, color = colors.textSecondary, fontSize = 10.sp, maxLines = 1)
                }
            }
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = accentColor
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                content = content
            )
        }
    }
}

@Composable
fun GuideCodeBlock(code: String, colors: AppColors) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.bg)
            .border(1.dp, colors.surfaceVariant, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Text(
            code,
            color = colors.textPrimary,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            lineHeight = 17.sp
        )
    }
}

@Composable
fun GuideFieldRow(field: String, type: String, description: String, colors: AppColors) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(field, color = colors.primary, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(90.dp))
        Text(type, color = colors.gold, fontFamily = FontFamily.Monospace, fontSize = 11.sp, modifier = Modifier.width(60.dp))
        Text(description, color = colors.textSecondary, fontSize = 11.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
fun RarityBadgeRow(rarity: String, color: String, description: String, colors: AppColors) {
    val badgeColor = when (rarity) {
        "common" -> colors.textSecondary
        "uncommon" -> Color(0xFF00FF00)
        "rare" -> colors.gold
        "epic" -> colors.accent
        else -> colors.textSecondary
    }
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(badgeColor)
        )
        Spacer(Modifier.width(6.dp))
        Text(rarity, color = badgeColor, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(80.dp))
        Text("$color · $description", color = colors.textSecondary, fontSize = 11.sp)
    }
}

@Composable
fun CopyButton(text: String, context: android.content.Context, colors: AppColors) {
    Button(
        onClick = {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Addon JSON", text))
            Toast.makeText(context, "📋 In Zwischenablage kopiert!", Toast.LENGTH_SHORT).show()
        },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = colors.primary.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.primary.copy(alpha = 0.5f)),
        contentPadding = PaddingValues(vertical = 6.dp)
    ) {
        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = colors.primary, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text("JSON KOPIEREN", color = colors.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AddonItemCard(addon: AddonMeta, colors: AppColors, onDelete: () -> Unit) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY) }
    val dateStr = dateFormat.format(Date(addon.importedAt))

    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (addon.isEnabled) colors.primary.copy(alpha = 0.4f) else colors.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(addon.name, color = if (addon.isEnabled) colors.textPrimary else colors.textSecondary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = colors.primary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(addon.fileType.uppercase(), color = colors.primary, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text("${addon.placeCount} Orte · Importiert am $dateStr", color = colors.textSecondary, fontSize = 12.sp)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = addon.isEnabled,
                    onCheckedChange = { isChecked ->
                        AddonManager.toggleAddon(context, addon.id, isChecked)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colors.bg,
                        checkedTrackColor = colors.primary,
                        uncheckedThumbColor = colors.textSecondary,
                        uncheckedTrackColor = colors.surfaceVariant
                    )
                )
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Löschen", tint = colors.accent.copy(alpha = 0.8f))
                }
            }
        }
    }
}
