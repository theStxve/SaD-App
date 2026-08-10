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

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import com.sad.app.data.MusicManager
import com.sad.app.data.PlaybackMode

@Composable
fun SettingsScreen(onThemeChanged: (AppTheme) -> Unit = {}) {
    val context = LocalContext.current
    val colors = LocalAppColors.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTheme by remember { mutableStateOf(ThemeManager.load(context)) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var pendingAddonUri by remember { mutableStateOf<Uri?>(null) }
    var addonNameInput by remember { mutableStateOf("") }
    var showAddonDialog by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }

    var playerNameInput by remember { mutableStateOf(PlayerProfile.load(context).playerName) }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val res = MusicManager.addCustomSong(context, it)
            res.onSuccess { item ->
                Toast.makeText(context, "Track '${item.title}' hinzugefügt!", Toast.LENGTH_SHORT).show()
            }.onFailure { err ->
                Toast.makeText(context, "Fehler beim Laden: ${err.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val addonPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            pendingAddonUri = it
            addonNameInput = ""
            showAddonDialog = true
        }
    }

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

        // ── Spielerprofil ────────────────────────────────────────────────────
        item {
            Text(
                "SPIELERPROFIL",
                color = colors.textSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.surface)
                    .padding(14.dp)
            ) {
                Text("Dein Operative-Name", color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = playerNameInput,
                    onValueChange = { newName ->
                        playerNameInput = newName
                        PlayerProfile.setPlayerName(context, newName)
                    },
                    singleLine = true,
                    placeholder = { Text("z.B. CyberRunner99", color = colors.textSecondary.copy(alpha = 0.5f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.surfaceVariant,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(20.dp))
            Divider(color = colors.surfaceVariant)
            Spacer(Modifier.height(20.dp))
        }

        // ── Audio & Hintergrundmusik ───────────────────────────────────────
        item {
            Text(
                "AUDIO & HINTERGRUNDMUSIK",
                color = colors.textSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.surface)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Master Toggle Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Hintergrundmusik", color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Schleife im Vordergrund", color = colors.textSecondary, fontSize = 11.sp)
                        }
                    }
                    Switch(
                        checked = MusicManager.isEnabled,
                        onCheckedChange = { enabled -> MusicManager.toggleEnabled(context, enabled) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.bg,
                            checkedTrackColor = colors.primary
                        )
                    )
                }

                if (MusicManager.isEnabled) {
                    Divider(color = colors.surfaceVariant.copy(alpha = 0.5f))

                    // Volume Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Lautstärke", color = colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text("${(MusicManager.volume * 100).toInt()}%", color = colors.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = MusicManager.volume,
                            onValueChange = { vol -> MusicManager.setVolume(context, vol) },
                            colors = SliderDefaults.colors(
                                thumbColor = colors.primary,
                                activeTrackColor = colors.primary,
                                inactiveTrackColor = colors.surfaceVariant
                            )
                        )
                    }

                    // Wiedergabemodus Selection
                    Column {
                        Text("Wiedergabemodus", color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PlaybackMode.entries.forEach { mode ->
                                val isSel = MusicManager.playbackMode == mode
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { MusicManager.setMode(context, mode) },
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSel) colors.primary.copy(alpha = 0.2f) else colors.bg,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) colors.primary else colors.surfaceVariant)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(mode.label, color = if (isSel) colors.primary else colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Now Playing HUD Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = colors.bg),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.primary.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(MusicManager.currentTrackTitle, color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                    if (MusicManager.currentTrackArtist.isNotBlank()) {
                                        Text(MusicManager.currentTrackArtist, color = colors.textSecondary, fontSize = 11.sp, maxLines = 1)
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { MusicManager.previousTrack(context) },
                                        enabled = MusicManager.playlist.isNotEmpty(),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.SkipPrevious, contentDescription = null, tint = if (MusicManager.playlist.isNotEmpty()) colors.textPrimary else colors.textSecondary.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        onClick = { MusicManager.togglePlayPause(context) },
                                        enabled = MusicManager.playlist.isNotEmpty(),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            if (MusicManager.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = if (MusicManager.playlist.isNotEmpty()) colors.primary else colors.textSecondary.copy(alpha = 0.4f),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { MusicManager.nextTrack(context) },
                                        enabled = MusicManager.playlist.isNotEmpty(),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.SkipNext, contentDescription = null, tint = if (MusicManager.playlist.isNotEmpty()) colors.textPrimary else colors.textSecondary.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            // Song Fortschrittsleiste / Player Seek Slider
                            if (MusicManager.playlist.isNotEmpty() && MusicManager.durationMs > 0) {
                                var isSeeking by remember { mutableStateOf(false) }
                                var sliderPos by remember { mutableFloatStateOf(0f) }

                                val currentPos = if (isSeeking) sliderPos.toLong() else MusicManager.currentPositionMs
                                val maxDur = MusicManager.durationMs.toFloat().coerceAtLeast(1f)

                                Spacer(Modifier.height(6.dp))
                                Slider(
                                    value = currentPos.toFloat().coerceIn(0f, maxDur),
                                    onValueChange = { newPos ->
                                        isSeeking = true
                                        sliderPos = newPos
                                    },
                                    onValueChangeFinished = {
                                        MusicManager.seekTo(sliderPos.toLong())
                                        isSeeking = false
                                    },
                                    valueRange = 0f..maxDur,
                                    colors = SliderDefaults.colors(
                                        thumbColor = colors.primary,
                                        activeTrackColor = colors.primary,
                                        inactiveTrackColor = colors.surfaceVariant
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(24.dp)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(MusicManager.formatTime(currentPos), color = colors.textSecondary, fontSize = 10.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                    Text(MusicManager.formatTime(MusicManager.durationMs), color = colors.textSecondary, fontSize = 10.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                }
                            }
                        }
                    }

                    // Songs Playlist
                    Column {
                        Text("PLAYLIST (${MusicManager.playlist.size})", color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        if (MusicManager.playlist.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(colors.bg)
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Keine Songs in der Playlist. Tippe unten auf 'EIGENEN SONG HINZUFÜGEN'.", color = colors.textSecondary, fontSize = 11.sp)
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                MusicManager.playlist.forEachIndexed { idx, song ->
                                    val isCurrent = idx == MusicManager.currentSongIndex
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isCurrent) colors.primary.copy(alpha = 0.15f) else colors.bg)
                                            .border(1.dp, if (isCurrent) colors.primary.copy(alpha = 0.5f) else colors.surfaceVariant, RoundedCornerShape(6.dp))
                                            .clickable { MusicManager.playTrackAtIndex(context, idx) }
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                            Text(if (isCurrent && MusicManager.isPlaying) ">" else "${idx + 1}.", color = if (isCurrent) colors.primary else colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(20.dp))
                                            Column {
                                                Text(song.title, color = if (isCurrent) colors.primary else colors.textPrimary, fontSize = 12.sp, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal, maxLines = 1)
                                                Text(song.artist, color = colors.textSecondary, fontSize = 10.sp, maxLines = 1)
                                            }
                                        }
                                        IconButton(
                                            onClick = { MusicManager.removeCustomSong(context, song.id) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Entfernen", tint = colors.accent.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Button: Add Song
                    Button(
                        onClick = { audioPickerLauncher.launch(arrayOf("audio/*")) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.primary)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("EIGENEN SONG HINZUFÜGEN (.mp3, .wav, .ogg, .flac)", color = colors.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Divider(color = colors.surfaceVariant)
            Spacer(Modifier.height(20.dp))
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

        // ── Marker / Rarity Farben ───────────────────────────────────────────
        item {
            val rcm = com.sad.app.data.RarityColorManager

            Text(
                "MARKER FARBEN",
                color = colors.textSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Passe die Farben jedes Seltenheitsgrades an. Hex-Wert eingeben (#RRGGBB).",
                color = colors.textSecondary,
                fontSize = 11.sp
            )
            Spacer(Modifier.height(10.dp))

            // State-Holder für Eingabefelder (damit Tipp-Fehler nicht sofort übernommen werden)
            var epicInput     by remember { mutableStateOf(rcm.epicColor) }
            var rareInput     by remember { mutableStateOf(rcm.rareColor) }
            var uncommonInput by remember { mutableStateOf(rcm.uncommonColor) }
            var commonInput   by remember { mutableStateOf(rcm.commonColor) }

            data class RarityRow(
                val label: String,
                val sublabel: String,
                val input: String,
                val liveHex: String,
                val onInputChange: (String) -> Unit,
                val onApply: () -> Unit
            )

            val rarityRows = listOf(
                RarityRow("Epic", "Legendaer",   epicInput,     rcm.epicColor,
                    { epicInput = it },
                    { rcm.setEpic(context, epicInput.trim()) }),
                RarityRow("Rare", "Sehr selten", rareInput,     rcm.rareColor,
                    { rareInput = it },
                    { rcm.setRare(context, rareInput.trim()) }),
                RarityRow("Uncommon", "Selten",  uncommonInput, rcm.uncommonColor,
                    { uncommonInput = it },
                    { rcm.setUncommon(context, uncommonInput.trim()) }),
                RarityRow("Common", "Haeufig",   commonInput,   rcm.commonColor,
                    { commonInput = it },
                    { rcm.setCommon(context, commonInput.trim()) }),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.surface)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                rarityRows.forEach { row ->
                    val parsedColor = try {
                        val raw = row.liveHex.trim().let { if (it.startsWith("#")) it else "#$it" }
                        Color(android.graphics.Color.parseColor(raw))
                    } catch (e: Exception) { colors.textSecondary }

                    val inputValid = rcm.isValidHex(row.input.trim())

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Live-Farb-Dot (zeigt aktuell gespeicherte Farbe)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(parsedColor)
                                .border(1.5.dp, colors.surfaceVariant, CircleShape)
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(row.label, color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(6.dp))
                                Text(row.sublabel, color = colors.textSecondary, fontSize = 11.sp)
                            }
                            Spacer(Modifier.height(4.dp))
                            OutlinedTextField(
                                value = row.input,
                                onValueChange = row.onInputChange,
                                singleLine = true,
                                placeholder = { Text(row.liveHex, color = colors.textSecondary.copy(alpha = 0.4f), fontSize = 12.sp) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = if (inputValid) parsedColor else colors.accent,
                                    unfocusedBorderColor = if (inputValid) parsedColor.copy(alpha = 0.5f) else colors.surfaceVariant,
                                    focusedTextColor = colors.textPrimary,
                                    unfocusedTextColor = colors.textPrimary
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontSize = 12.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                            )
                        }

                        // Anwenden-Button
                        Button(
                            onClick = {
                                row.onApply()
                                Toast.makeText(context, "${row.label} Farbe gespeichert", Toast.LENGTH_SHORT).show()
                            },
                            enabled = inputValid,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = parsedColor.copy(alpha = 0.2f),
                                disabledContainerColor = colors.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(48.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp)
                        ) {
                            Text("OK", color = if (inputValid) parsedColor else colors.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }

                Divider(color = colors.surfaceVariant.copy(alpha = 0.5f))

                // Reset-Button
                Button(
                    onClick = {
                        rcm.resetAll(context)
                        epicInput     = rcm.epicColor
                        rareInput     = rcm.rareColor
                        uncommonInput = rcm.uncommonColor
                        commonInput   = rcm.commonColor
                        Toast.makeText(context, "Farben zurueckgesetzt", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("STANDARD WIEDERHERSTELLEN", color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(20.dp))
            Divider(color = colors.surfaceVariant)
            Spacer(Modifier.height(20.dp))
        }

        // ── Map Customization ───────────────────────────────────────────────
        item {
            // Reaktives State das sich bei MapSettingsManager.current Änderungen sofort aktualisiert
            var mapSettings by remember(MapSettingsManager.current) { mutableStateOf(MapSettingsManager.current) }

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

            Spacer(Modifier.height(12.dp))

            // Präzisionsmode Toggle (20m Radius statt 150m)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.surface)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text("Präzisionsmodus", color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("Kleinere Erkundungskreise (20m) – spiegelt genau deinen Pfad wider", color = colors.textSecondary, fontSize = 11.sp)
                }
                Switch(
                    checked = mapSettings.precisionModeEnabled,
                    onCheckedChange = { newVal ->
                        val updated = mapSettings.copy(precisionModeEnabled = newVal)
                        mapSettings = updated
                        MapSettingsManager.save(context, updated)
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = colors.primary, checkedTrackColor = colors.primary.copy(alpha = 0.3f))
                )
            }

            Spacer(Modifier.height(12.dp))

            // Verbindungsmode Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.surface)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text("Verbindungsmodus", color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("Deckt Pfad/Linie zwischen nacheinander gemessenen Punkten auf", color = colors.textSecondary, fontSize = 11.sp)
                }
                Switch(
                    checked = mapSettings.connectionModeEnabled,
                    onCheckedChange = { newVal ->
                        val updated = mapSettings.copy(connectionModeEnabled = newVal)
                        mapSettings = updated
                        MapSettingsManager.save(context, updated)
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = colors.accent, checkedTrackColor = colors.accent.copy(alpha = 0.3f))
                )
            }

            Spacer(Modifier.height(12.dp))

            // Besuchte Dungeons global anzeigen Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.surface)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text("Besuchte Dungeons immer anzeigen", color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("Zeigt bereits erkundete (graue) Dungeons weltweit auf der Karte an, egal wie weit entfernt", color = colors.textSecondary, fontSize = 11.sp)
                }
                Switch(
                    checked = mapSettings.showVisitedDungeonsGlobally,
                    onCheckedChange = { newVal ->
                        val updated = mapSettings.copy(showVisitedDungeonsGlobally = newVal)
                        mapSettings = updated
                        MapSettingsManager.save(context, updated)
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = colors.primary, checkedTrackColor = colors.primary.copy(alpha = 0.3f))
                )
            }

            Spacer(Modifier.height(12.dp))

            // Alle Wege als Präzisionspfade Toggle (mit Lückenfüllung)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.surface)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text("Alle Wege als Präzisionspfade", color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("Konvertiert die Darstellung aller bisherigen Wege in schmale Präzisionspfade & füllt Lücken sauber auf", color = colors.textSecondary, fontSize = 11.sp)
                }
                Switch(
                    checked = mapSettings.forcePrecisionPaths,
                    onCheckedChange = { newVal ->
                        val updated = mapSettings.copy(forcePrecisionPaths = newVal)
                        mapSettings = updated
                        MapSettingsManager.save(context, updated)
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = colors.accent, checkedTrackColor = colors.accent.copy(alpha = 0.3f))
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

        // ── Navigationsleiste & Tabs ──────────────────────────────────────────
        item {
            val tabVisibility = TabVisibilityManager.current

            Text(
                "NAVIGATIONSLEISTE & TABS",
                color = colors.textSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Blende ungenutzte Tabs in der Navigationsleiste unten aus (Karte & Optionen bleiben immer sichtbar).",
                color = colors.textSecondary,
                fontSize = 11.sp
            )
            Spacer(Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.surface)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Quests
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Quests Tab", color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Tägliche & wöchentliche Aufgaben", color = colors.textSecondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = tabVisibility.showQuests,
                        onCheckedChange = { isChecked ->
                            TabVisibilityManager.save(context, tabVisibility.copy(showQuests = isChecked))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.bg,
                            checkedTrackColor = colors.primary,
                            uncheckedThumbColor = colors.textSecondary,
                            uncheckedTrackColor = colors.surfaceVariant
                        )
                    )
                }

                Divider(color = colors.surfaceVariant)

                // Gerüchte
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Gerüchte Tab", color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("P2P Spieler-Netzwerk & Gerüchte", color = colors.textSecondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = tabVisibility.showRumors,
                        onCheckedChange = { isChecked ->
                            TabVisibilityManager.save(context, tabVisibility.copy(showRumors = isChecked))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.bg,
                            checkedTrackColor = colors.primary,
                            uncheckedThumbColor = colors.textSecondary,
                            uncheckedTrackColor = colors.surfaceVariant
                        )
                    )
                }

                Divider(color = colors.surfaceVariant)

                // Addons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Addon Hub Tab", color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Modding, Community-Packs & Exporter", color = colors.textSecondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = tabVisibility.showAddons,
                        onCheckedChange = { isChecked ->
                            TabVisibilityManager.save(context, tabVisibility.copy(showAddons = isChecked))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.bg,
                            checkedTrackColor = colors.primary,
                            uncheckedThumbColor = colors.textSecondary,
                            uncheckedTrackColor = colors.surfaceVariant
                        )
                    )
                }

                Divider(color = colors.surfaceVariant)

                // Erfolge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Erfolge Tab", color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Achievements & Dev-Optionen", color = colors.textSecondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = tabVisibility.showAchievements,
                        onCheckedChange = { isChecked ->
                            TabVisibilityManager.save(context, tabVisibility.copy(showAchievements = isChecked))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.bg,
                            checkedTrackColor = colors.primary,
                            uncheckedThumbColor = colors.textSecondary,
                            uncheckedTrackColor = colors.surfaceVariant
                        )
                    )
                }
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
                onClick = { showShareSheet = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceVariant),
                shape = androidx.compose.foundation.shape.CutCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.primary.copy(alpha = 0.4f))
            ) {
                Text("📤 PROFILKARTE TEILEN", color = colors.primary, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, letterSpacing = 2.sp)
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

    // ── Addon Name Dialog ─────────────────────────────────────────────────────
    if (showAddonDialog && pendingAddonUri != null) {
        val targetUri = pendingAddonUri!!
        AlertDialog(
            onDismissRequest = {
                showAddonDialog = false
                pendingAddonUri = null
            },
            containerColor = colors.surface,
            shape = RoundedCornerShape(16.dp),
            title = { Text("Addon / Dungeon-Pack benennen", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Gib diesem Pack einen Namen (z.B. 'Berlin Lost Places'):", color = colors.textSecondary, fontSize = 13.sp)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = addonNameInput,
                        onValueChange = { addonNameInput = it },
                        singleLine = true,
                        placeholder = { Text("Addon Name", color = colors.textSecondary.copy(alpha = 0.5f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.surfaceVariant,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uriToProcess = targetUri
                        val nameToUse = addonNameInput
                        showAddonDialog = false
                        pendingAddonUri = null
                        coroutineScope.launch {
                            val res = com.sad.app.data.AddonManager.importAddon(context, uriToProcess, nameToUse)
                            if (res.isSuccess) {
                                val addon = res.getOrNull()
                                Toast.makeText(context, "Addon '${addon?.name}' mit ${addon?.placeCount} Dungeons importiert!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Import-Fehler: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("IMPORTIEREN", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddonDialog = false
                        pendingAddonUri = null
                    }
                ) {
                    Text("ABBRECHEN", color = colors.textSecondary)
                }
            }
        )
    }

    // ── Share Profile Sheet ───────────────────────────────────────────────────
    if (showShareSheet) {
        ShareProfileSheet(
            profile = PlayerProfile.load(context),
            onDismiss = { showShareSheet = false }
        )
    }
}
