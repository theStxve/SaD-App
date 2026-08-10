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
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sad.app.ui.AchievementsScreen
import com.sad.app.ui.AddonScreen
import com.sad.app.ui.AppTheme
import com.sad.app.ui.LocalAppColors
import com.sad.app.ui.MapScreen
import com.sad.app.ui.MapSettingsManager
import com.sad.app.ui.QuestScreen
import com.sad.app.ui.RumorsScreen
import com.sad.app.ui.SettingsScreen
import com.sad.app.ui.TabVisibilityManager
import com.sad.app.ui.ThemeManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.sad.app.data.MusicManager
import org.osmdroid.util.GeoPoint

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Map : Screen("map", "Karte", Icons.Default.Map)
    object Quests : Screen("quests", "Quests", Icons.Default.Star)
    object Rumors : Screen("rumors", "Gerüchte", Icons.Default.Forum)
    object Addons : Screen("addons", "Addons", Icons.Default.Extension)
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
        
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.NEARBY_WIFI_DEVICES
            )
        )

        setContent {
            SADApp()
        }
    }
}

@Composable
fun SADApp() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Einmalig beim Start: gespeicherte Map-Settings, Addons und MusicManager laden
    remember { 
        MapSettingsManager.init(context)
        TabVisibilityManager.init(context)
        com.sad.app.data.AddonManager.init(context)
        MusicManager.init(context)
        com.sad.app.data.RarityColorManager.init(context)
        Unit 
    }

    // Musiksteuerung bei App-Vordergrund/Hintergrund Wechsel
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START, Lifecycle.Event.ON_RESUME -> {
                    MusicManager.onAppForeground(context)
                }
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    MusicManager.onAppBackground()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
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
    var targetMapLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var globalRefreshTrigger by remember { mutableStateOf(0) }

    val tabVis = TabVisibilityManager.current
    val visibleScreens = remember(tabVis) {
        buildList {
            add(Screen.Map)
            if (tabVis.showQuests) add(Screen.Quests)
            if (tabVis.showRumors) add(Screen.Rumors)
            if (tabVis.showAddons) add(Screen.Addons)
            if (tabVis.showAchievements) add(Screen.Achievements)
            add(Screen.Settings)
        }
    }

    LaunchedEffect(visibleScreens) {
        if (selectedScreen !in visibleScreens) {
            selectedScreen = Screen.Map
        }
    }

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
                    Screen.Map          -> MapScreen(targetLocation = targetMapLocation)
                    Screen.Quests       -> QuestScreen(globalRefreshTrigger)
                    Screen.Rumors       -> RumorsScreen(
                        onNavigateToMap = { location ->
                            targetMapLocation = location
                            selectedScreen = Screen.Map
                        }
                    )
                    Screen.Addons       -> AddonScreen()
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
                    modifier = Modifier.fillMaxSize().padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    visibleScreens.forEach { screen ->
                        val isSelected = selectedScreen == screen
                        val accentColor = when (screen) {
                            Screen.Achievements -> colors.accent
                            Screen.Rumors -> colors.gold
                            Screen.Addons -> colors.primary
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
    IconButton(onClick = onClick, modifier = Modifier.size(60.dp)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) selectedColor.copy(alpha = 0.15f) else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = screen.icon,
                    contentDescription = screen.label,
                    tint = if (isSelected) selectedColor else Color(0xFF445566),
                    modifier = Modifier.size(18.dp)
                )
                if (showBadge) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .align(Alignment.TopEnd)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(Color(0xFFFF00E6))
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                screen.label,
                fontSize = 8.5.sp,
                color = if (isSelected) selectedColor else Color(0xFF445566),
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                letterSpacing = 0.3.sp
            )
        }
    }
}
