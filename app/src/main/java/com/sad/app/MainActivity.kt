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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.toArgb
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
import com.sad.app.ui.AppTheme
import com.sad.app.ui.LocalAppColors
import com.sad.app.ui.MapScreen
import com.sad.app.ui.QuestScreen
import com.sad.app.ui.RumorsScreen
import com.sad.app.ui.MapSettingsManager
import com.sad.app.ui.SettingsScreen
import com.sad.app.ui.ThemeManager
import org.osmdroid.config.Configuration
import android.preference.PreferenceManager
import androidx.compose.ui.platform.LocalContext

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Map : Screen("map", "Karte", Icons.Default.Map)
    object Quests : Screen("quests", "Quests", Icons.Default.Star)
    object Rumors : Screen("rumors", "Gerüchte", Icons.Default.Forum)
    object Achievements : Screen("achievements", "Erfolge", Icons.Default.EmojiEvents)
    object Settings : Screen("settings", "Optionen", Icons.Default.Settings)
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
    val context = LocalContext.current
    // Einmalig beim Start: gespeicherte Map-Settings und Addons in den reaktiven State laden
    remember { 
        MapSettingsManager.init(context)
        com.sad.app.data.AddonManager.init(context)
        Unit 
    }
    var currentTheme by remember { mutableStateOf(ThemeManager.load(context)) }
    val colors = currentTheme.colors

    // Statusleiste und Navigationsleiste unten dynamisch an das Theme anpassen
    val activity = (context as? ComponentActivity)
    SideEffect {
        activity?.window?.apply {
            statusBarColor = android.graphics.Color.parseColor(
                String.format("#%06X", (0xFFFFFF and colors.bg.toArgb()))
            )
            navigationBarColor = android.graphics.Color.parseColor(
                String.format("#%06X", (0xFFFFFF and colors.surface.toArgb()))
            )
        }
    }

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

    val gameDb = remember { com.sad.app.data.GameDatabase.getDatabase(context) }
    val unreadRumorsCount by gameDb.rumorDao().unreadCount().collectAsState(initial = 0)

    LaunchedEffect(selectedScreen) {
        if (selectedScreen == Screen.Rumors) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                gameDb.rumorDao().markAllRead()
            }
        }
    }

    CompositionLocalProvider(LocalAppColors provides colors) {
        Box(modifier = Modifier.fillMaxSize().background(colors.bg)) {
            Box(modifier = Modifier.fillMaxSize().padding(bottom = 68.dp)) {
                when (selectedScreen) {
                    Screen.Map          -> MapScreen()
                    Screen.Quests       -> QuestScreen(globalRefreshTrigger)
                    Screen.Rumors       -> RumorsScreen()
                    Screen.Achievements -> AchievementsScreen(onRefreshRequested = { globalRefreshTrigger++ })
                    Screen.Settings     -> SettingsScreen(onThemeChanged = { currentTheme = it })
                }
            }

            // Custom Bottom Navigation
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(68.dp)
                    .background(colors.surface)
            ) {
                Divider(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = 1.dp,
                    color = colors.primary.copy(alpha = 0.2f)
                )
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(Screen.Map, Screen.Quests, Screen.Rumors, Screen.Achievements, Screen.Settings).forEach { screen ->
                        val isSelected = selectedScreen == screen
                        val accentColor = when (screen) {
                            Screen.Achievements -> colors.accent
                            Screen.Rumors -> colors.gold
                            Screen.Settings -> colors.primary
                            else -> colors.primary
                        }
                        val showBadge = (screen == Screen.Rumors && unreadRumorsCount > 0)
                        NavItem(screen, isSelected, accentColor, showBadge) { selectedScreen = screen }
                    }
                }
            }
        }
    }
}

@Composable
fun NavItem(screen: Screen, isSelected: Boolean, selectedColor: Color, showBadge: Boolean = false, onClick: () -> Unit) {
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
                if (showBadge) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .align(Alignment.TopEnd)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(Color(0xFFFF00E6))
                    )
                }
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
