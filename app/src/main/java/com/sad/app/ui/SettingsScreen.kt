package com.sad.app.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sad.app.data.BackupManager
import com.sad.app.data.PlayerProfile
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onThemeChanged: (AppTheme) -> Unit = {}) {
    val context = LocalContext.current
    val colors = LocalAppColors.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTheme by remember { mutableStateOf(ThemeManager.load(context)) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { saveUri ->
            coroutineScope.launch {
                val result = BackupManager.exportBackup(context, saveUri)
                if (result.isSuccess) {
                    Toast.makeText(context, "Backup erfolgreich exportiert!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Export fehlgeschlagen: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { pendingImportUri = it }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // ── Header ──────────────────────────────────────────────────────────
        item {
            Text("EINSTELLUNGEN", color = colors.primary, fontSize = 11.sp, letterSpacing = 4.sp)
            Text(
                "Settings",
                color = colors.textPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(16.dp))
        }

        // ── Theme Picker ─────────────────────────────────────────────────────
        item {
            Text(
                "FARBSCHEMA",
                color = colors.textSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(8.dp))

            // 2x2 Grid
            val themes = AppTheme.entries
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                themes.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { theme ->
                            val isSelected = theme == selectedTheme
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) theme.colors.primary.copy(alpha = 0.12f)
                                        else colors.surface
                                    )
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) theme.colors.primary
                                                else colors.surfaceVariant,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        selectedTheme = theme
                                        ThemeManager.save(context, theme)
                                        onThemeChanged(theme)
                                    }
                                    .padding(14.dp)
                            ) {
                                Column {
                                    Text(
                                        theme.displayName,
                                        color = if (isSelected) theme.colors.primary else colors.textPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    // Farbpunkte Preview
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf(theme.colors.primary, theme.colors.accent, theme.colors.gold).forEach { c ->
                                            Box(
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clip(CircleShape)
                                                    .background(c)
                                            )
                                        }
                                    }
                                    if (isSelected) {
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            "● Aktiv",
                                            color = theme.colors.primary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                }
                            }
                        }
                        // Leere Zelle wenn ungerade Anzahl
                        if (row.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Divider(color = colors.surfaceVariant)
            Spacer(Modifier.height(20.dp))
        }

        // ── Map Customization ───────────────────────────────────────────────
        item {
            // Direkt aus dem reaktiven Singleton – initialer Wert stimmt immer
            var mapSettings by remember { mutableStateOf(MapSettingsManager.current) }

            Text(
                "KARTEN-ANPASSUNG",
                color = colors.textSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(12.dp))

            // Dark Mode Invert Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.surface)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Dark-Mode Invertierung", color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Switch(
                    checked = mapSettings.isInverted,
                    onCheckedChange = { newInv ->
                        val updated = mapSettings.copy(isInverted = newInv)
                        mapSettings = updated
                        MapSettingsManager.save(context, updated)
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = colors.primary, checkedTrackColor = colors.primary.copy(alpha = 0.3f))
                )
            }

            Spacer(Modifier.height(12.dp))

            // Kontrast Slider
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.surface)
                    .padding(14.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Kontrast", color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(String.format("%.1fx", mapSettings.contrast), color = colors.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = mapSettings.contrast,
                    onValueChange = { newVal ->
                        val updated = mapSettings.copy(contrast = newVal)
                        mapSettings = updated
                        MapSettingsManager.save(context, updated)
                    },
                    valueRange = 0.5f..2.5f,
                    colors = SliderDefaults.colors(thumbColor = colors.primary, activeTrackColor = colors.primary)
                )
            }

            Spacer(Modifier.height(12.dp))

            // Helligkeit Slider
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.surface)
                    .padding(14.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Helligkeit", color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(String.format("%+.0f", mapSettings.brightness), color = colors.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = mapSettings.brightness,
                    onValueChange = { newVal ->
                        val updated = mapSettings.copy(brightness = newVal)
                        mapSettings = updated
                        MapSettingsManager.save(context, updated)
                    },
                    valueRange = -100f..100f,
                    colors = SliderDefaults.colors(thumbColor = colors.primary, activeTrackColor = colors.primary)
                )
            }

            Spacer(Modifier.height(12.dp))

            // Nebel Deckkraft Slider
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.surface)
                    .padding(14.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Nebel-Deckkraft (Fog of War)", color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("${(mapSettings.fogOpacity * 100).toInt()}%", color = colors.accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = mapSettings.fogOpacity,
                    onValueChange = { newVal ->
                        val updated = mapSettings.copy(fogOpacity = newVal)
                        mapSettings = updated
                        MapSettingsManager.save(context, updated)
                    },
                    valueRange = 0.2f..1.0f,
                    colors = SliderDefaults.colors(thumbColor = colors.accent, activeTrackColor = colors.accent)
                )
            }

            Spacer(Modifier.height(10.dp))

            // Reset Button
            Button(
                onClick = {
                    val defaultSettings = MapSettings()
                    mapSettings = defaultSettings
                    MapSettingsManager.save(context, defaultSettings)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.textSecondary.copy(alpha = 0.3f))
            ) {
                Text("Standard-Kartenfilter zurücksetzen", color = colors.textSecondary, fontSize = 11.sp)
            }

            Spacer(Modifier.height(20.dp))
            Divider(color = colors.surfaceVariant)
            Spacer(Modifier.height(20.dp))
        }

        // ── Datensicherung ───────────────────────────────────────────────────
        item {
            Text(
                "DATENSICHERUNG",
                color = colors.textSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val fileName = "SAD_Backup_${System.currentTimeMillis()}.json"
                        exportLauncher.launch(fileName)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    shape = androidx.compose.foundation.shape.CutCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.primary.copy(alpha = 0.5f))
                ) {
                    Text("💾 EXPORTIEREN", color = colors.primary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                Button(
                    onClick = {
                        importLauncher.launch(arrayOf("application/json", "*/*"))
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    shape = androidx.compose.foundation.shape.CutCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.accent.copy(alpha = 0.5f))
                ) {
                    Text("📥 IMPORTIEREN", color = colors.accent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }

            Spacer(Modifier.height(20.dp))
            Divider(color = colors.surfaceVariant)
            Spacer(Modifier.height(20.dp))
        }

        // ── Share ─────────────────────────────────────────────────────────────
        item {
            val profile = PlayerProfile.load(context)
            val prefs = context.getSharedPreferences("player_profile", android.content.Context.MODE_PRIVATE)
            val unlockedCount = prefs.getStringSet("unlocked_achievements", emptySet())?.size ?: 0

            Text(
                "TEILEN",
                color = colors.textSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(8.dp))

            val shareText = "--- SYSTEM LOG: CITY AS A DUNGEON ---\n" +
                "ID: USER_${profile.title.uppercase()}_${profile.level}\n" +
                "LEVEL: ${profile.level} [${profile.title}]\n" +
                "EXPLORED: ${profile.exploredCount} NODES\n" +
                "DUNGEONS: ${profile.visitedDungeons} CLEARED\n" +
                "------------------------------------\n" +
                "Join the network: #CityAsADungeon #SAD"

            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(intent, "TERMINAL BROADCAST"))
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceVariant),
                shape = androidx.compose.foundation.shape.CutCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.primary.copy(alpha = 0.4f))
            ) {
                Text("📤 FORTSCHRITT TEILEN", color = colors.primary, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, letterSpacing = 2.sp)
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    // ── Import Confirmation Dialog ────────────────────────────────────────────
    if (pendingImportUri != null) {
        val targetUri = pendingImportUri!!
        AlertDialog(
            onDismissRequest = { pendingImportUri = null },
            containerColor = colors.surface,
            shape = RoundedCornerShape(16.dp),
            title = { Text("Backup wiederherstellen?", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "ACHTUNG: Dies wird deinen aktuellen Spielstand vollständig überschreiben!",
                    color = colors.textSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uriToProcess = targetUri
                        pendingImportUri = null
                        coroutineScope.launch {
                            val result = BackupManager.importBackup(context, uriToProcess)
                            if (result.isSuccess) {
                                Toast.makeText(context, "Fortschritt wiederhergestellt!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Fehler: ${result.exceptionOrNull()?.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("ÜBERSCHREIBEN", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportUri = null }) {
                    Text("ABBRECHEN", color = colors.textSecondary)
                }
            }
        )
    }
}
