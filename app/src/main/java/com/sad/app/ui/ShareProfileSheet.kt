package com.sad.app.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.sad.app.data.PlayerProfile
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareProfileSheet(
    profile: PlayerProfile,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalAppColors.current

    var selectedMode by remember { mutableStateOf(CardLayoutMode.CARD_ID) }
    var selectedTheme by remember { mutableStateOf(CardTheme.CYBER) }

    val generatedBitmap by produceState<Bitmap?>(initialValue = null, profile, selectedMode, selectedTheme) {
        value = ProfileCardRenderer.generateCard(profile, selectedMode, selectedTheme)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        scrimColor = Color.Black.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "SPIELERPROFIL TEILEN",
                color = colors.primary,
                fontSize = 11.sp,
                letterSpacing = 3.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Visuelle Profilkarte generieren",
                color = colors.textPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black
            )

            Spacer(Modifier.height(16.dp))

            // ── 1. Live Vorschau ───────────────────────────────────────────────
            generatedBitmap?.let { bitmap ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.bg)
                        .border(1.dp, colors.primary.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Profilkarte Vorschau",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                    )
                }
            } ?: Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.bg),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = colors.primary)
            }

            Spacer(Modifier.height(20.dp))

            // ── 2. Layout-Wahl ────────────────────────────────────────────────
            Text("LAYOUT WÄHLEN", color = colors.textSecondary, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CardLayoutMode.entries.forEach { mode ->
                    val isSelected = selectedMode == mode
                    Button(
                        onClick = { selectedMode = mode },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) colors.primary.copy(alpha = 0.2f) else colors.bg
                        ),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) colors.primary else colors.surfaceVariant
                        )
                    ) {
                        Text(
                            mode.displayName,
                            color = if (isSelected) colors.primary else colors.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── 3. Farbschema-Wahl ────────────────────────────────────────────
            Text("FARBSCHEMA WÄHLEN", color = colors.textSecondary, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CardTheme.entries.forEach { theme ->
                    val isSelected = selectedTheme == theme
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { selectedTheme = theme }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(theme.bgColor))
                                .border(
                                    if (isSelected) 3.dp else 1.dp,
                                    if (isSelected) colors.primary else Color(theme.primaryColor),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color(theme.primaryColor))
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            theme.displayName,
                            fontSize = 10.sp,
                            color = if (isSelected) colors.primary else colors.textSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── 4. Teilen Button ──────────────────────────────────────────────
            Button(
                onClick = {
                    val bitmap = generatedBitmap ?: return@Button
                    shareProfileImage(context, bitmap, profile)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    "📤 ALS BILD TEILEN",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

private fun shareProfileImage(context: Context, bitmap: Bitmap, profile: PlayerProfile) {
    try {
        val cachePath = File(context.cacheDir, "images").apply { if (!exists()) mkdirs() }
        val file = File(cachePath, "SAD_Profile_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        val contentUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareText = "--- CITY AS A DUNGEON ---\n" +
                "Agent: ${profile.displayName} [${profile.title}]\n" +
                "Level: ${profile.level} | Erkundet: ${profile.exploredCount} Sektoren | Dungeons: ${profile.visitedDungeons}\n" +
                "#CityAsADungeon #SAD"

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_TEXT, shareText)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "PROFILKARTE TEILEN"))
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Fehler beim Erstellen der Karte: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
