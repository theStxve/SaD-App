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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sad.app.data.GameDatabase
import com.sad.app.data.Rumor
import com.sad.app.nearby.RumorSyncManager
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RumorsScreen() {
    val context = LocalContext.current
    val colors = LocalAppColors.current
    val gameDb = remember { GameDatabase.getDatabase(context) }
    val rumors by gameDb.rumorDao().getAllFlow().collectAsState(initial = emptyList())
    
    var syncStatus by remember { mutableStateOf("Bereit zum Verbinden") }
    var isScanning by remember { mutableStateOf(false) }

    val syncManager = remember {
        RumorSyncManager(context, gameDb) { status ->
            syncStatus = status
        }
    }

    DisposableEffect(Unit) {
        onDispose { syncManager.stopAll() }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Column {
                Text("GERÜCHTE", color = colors.primary, fontSize = 11.sp, letterSpacing = 4.sp)
                Text("Spieler-Netzwerk", color = colors.textPrimary, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(4.dp))
                Text("Triff andere Spieler und tausche Geheimnisse aus.", color = colors.textSecondary, fontSize = 13.sp)
                Spacer(Modifier.height(16.dp))

                // Status Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.primary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(if (isScanning) colors.primary else colors.textSecondary)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(syncStatus, color = colors.textPrimary, fontSize = 13.sp)
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                isScanning = !isScanning
                                if (isScanning) {
                                    syncManager.startAdvertisingAndDiscovery()
                                } else {
                                    syncManager.stopAll()
                                    syncStatus = "Bereit zum Verbinden"
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isScanning) colors.accent.copy(alpha = 0.2f) else colors.primary.copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            border = ButtonDefaults.outlinedButtonBorder
                        ) {
                            Text(
                                if (isScanning) "SUCHE STOPPEN" else "SPIELER IN DER NÄHE SUCHEN",
                                color = if (isScanning) colors.accent else colors.primary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (rumors.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Noch keine Gerüchte", color = colors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text("Triff andere Spieler um Geheimnisse\nüber Dungeons zu erfahren!", color = colors.textSecondary, fontSize = 13.sp)
                        }
                    }
                } else {
                    Text("GERÜCHTE", color = colors.accent, fontSize = 11.sp, letterSpacing = 3.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        items(rumors) { rumor ->
            RumorCard(rumor, colors)
        }
    }
}

@Composable
fun RumorCard(rumor: Rumor, colors: AppColors) {
    val timeFormat = remember { SimpleDateFormat("dd.MM · HH:mm", Locale.GERMANY) }
    val timeStr = timeFormat.format(Date(rumor.receivedAt))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .border(1.dp, if (!rumor.isRead) colors.primary.copy(alpha = 0.5f) else colors.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${rumor.fromPlayer}", color = colors.gold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(timeStr, color = colors.textSecondary, fontSize = 11.sp)
            }
            Spacer(Modifier.height(6.dp))
            Text(rumor.message, color = colors.textPrimary, fontSize = 14.sp)
            if (rumor.placeName.isNotEmpty() && rumor.placeName != "Spieler-Update") {
                Spacer(Modifier.height(6.dp))
                Text(rumor.placeName, color = colors.primary, fontSize = 12.sp)
            }
        }
    }
}
