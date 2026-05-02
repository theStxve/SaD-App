package com.sad.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sad.app.notifications.DungeonNotifier
import com.sad.app.ui.AchievementsScreen
import com.sad.app.ui.MapScreen
import com.sad.app.ui.QuestScreen
import com.sad.app.ui.RumorsScreen
import org.osmdroid.config.Configuration
import android.preference.PreferenceManager
import androidx.compose.ui.platform.LocalContext

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Map : Screen("map", "Karte", Icons.Default.Map)
    object Quests : Screen("quests", "Quests", Icons.Default.Star)
    object Rumors : Screen("rumors", "Gerüchte", Icons.Default.Forum)
    object Achievements : Screen("achievements", "Erfolge", Icons.Default.EmojiEvents)
}

class MainActivity : ComponentActivity() {

    // Alle Permissions auf einmal anfragen
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            val serviceIntent = android.content.Intent(this, LocationTrackingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Statusleiste und Navigationsleiste unten an das dunkle Cyberpunk-Theme anpassen
        window.statusBarColor = android.graphics.Color.parseColor("#0A0A12")
        window.navigationBarColor = android.graphics.Color.parseColor("#0F0F1E")

        // Notification Channel einmalig erstellen
        DungeonNotifier.createChannel(this)

        // Alle nötigen Permissions anfragen
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
                add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_ADVERTISE)
                add(Manifest.permission.BLUETOOTH_CONNECT)
                add(Manifest.permission.BLUETOOTH_SCAN)
            }
        }
        // OSMDroid Konfiguration: Cache-Größe begrenzen!
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        Configuration.getInstance().load(this, prefs)
        // Max 100MB Cache (verhindert Bamboo-Wachstum)
        Configuration.getInstance().setTileFileSystemCacheMaxBytes(100L * 1024 * 1024)
        Configuration.getInstance().setTileFileSystemCacheTrimBytes(80L * 1024 * 1024)
        Configuration.getInstance().userAgentValue = packageName

        // Permissions anfragen
        permissionLauncher.launch(permissions.toTypedArray())

        setContent {
            SADApp()
        }
    }
}

@Composable
fun SADApp() {
    val bg = Color(0xFF0A0A12)
    val cyan = Color(0xFF00F3FF)
    val pink = Color(0xFFFF00E6)
    val surface = Color(0xFF0F0F1E)

    val context = LocalContext.current
    var selectedScreen by remember { mutableStateOf<Screen>(Screen.Map) }
    var globalRefreshTrigger by remember { mutableStateOf(0) }

    // Service beim App-Start nur triggern, wenn Permission bereits da ist
    LaunchedEffect(Unit) {
        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        
        if (hasPermission) {
            val serviceIntent = android.content.Intent(context, LocationTrackingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(bg)) {
        Box(modifier = Modifier.fillMaxSize().padding(bottom = 68.dp)) {
            when (selectedScreen) {
                Screen.Map          -> MapScreen()
                Screen.Quests       -> QuestScreen(globalRefreshTrigger)
                Screen.Rumors       -> RumorsScreen()
                Screen.Achievements -> AchievementsScreen(onRefreshRequested = { globalRefreshTrigger++ })
            }
        }

        // Custom Bottom Navigation
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(68.dp)
                .background(surface)
        ) {
            Divider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp,
                color = cyan.copy(alpha = 0.2f)
            )
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(Screen.Map, Screen.Quests, Screen.Rumors, Screen.Achievements).forEach { screen ->
                    val isSelected = selectedScreen == screen
                    val accentColor = when (screen) {
                        Screen.Achievements -> pink
                        Screen.Rumors -> Color(0xFFFFD700)
                        else -> cyan
                    }
                    NavItem(screen, isSelected, accentColor) { selectedScreen = screen }
                }
            }
        }
    }
}

@Composable
fun NavItem(screen: Screen, isSelected: Boolean, selectedColor: Color, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(72.dp)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) selectedColor.copy(alpha = 0.15f) else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = screen.icon,
                    contentDescription = screen.label,
                    tint = if (isSelected) selectedColor else Color(0xFF445566),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                screen.label,
                fontSize = 9.sp,
                color = if (isSelected) selectedColor else Color(0xFF445566),
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                letterSpacing = 0.5.sp
            )
        }
    }
}
