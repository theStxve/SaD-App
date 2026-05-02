package com.sad.app.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import android.graphics.Color
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sad.app.data.AppDatabase
import com.sad.app.data.ExploredArea
import com.sad.app.data.GameDatabase
import com.sad.app.data.PlaceEntity
import com.sad.app.data.PlayerProfile
import com.sad.app.data.Rumor
import com.sad.app.data.VisitedDungeon
import com.sad.app.notifications.DungeonNotifier
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

// Gamification Themes
val CyberpunkBackground = androidx.compose.ui.graphics.Color(0xFF0F0F1A)
val CyberpunkNeonCyan = androidx.compose.ui.graphics.Color(0xFF00F3FF)
val CyberpunkNeonPink = androidx.compose.ui.graphics.Color(0xFFFF00E6)

@Composable
fun MapScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }
    val gameDb = remember { GameDatabase.getDatabase(context) }
    
    var hasLocationPermission by remember {
        mutableStateOf(
            ActivityCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> hasLocationPermission = isGranted }
    )

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    if (hasLocationPermission) {
        // Startposition auf null setzen, damit der "INITIATING SCAN..." Ladebildschirm gezeigt wird, bis echtes GPS da ist
        var userLocation by remember { mutableStateOf<GeoPoint?>(null) }
        var places by remember { mutableStateOf<List<PlaceEntity>>(emptyList()) }
        var exploredCenters by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
        var followPlayer by remember { mutableStateOf(true) }
        var nearbyDungeon by remember { mutableStateOf<PlaceEntity?>(null) }
        var dungeonJustEntered by remember { mutableStateOf(false) }
        var dungeonAlreadyVisited by remember { mutableStateOf(false) }
        // xKours-Style stufenloser Zoom: direktes Level (12.0 bis 20.0)
        var currentZoom by remember { mutableStateOf(17f) }
        val rumors by gameDb.rumorDao().getAllFlow().collectAsState(initial = emptyList())
        val visitedDungeons by gameDb.visitedDungeonDao().getAllFlow().collectAsState(initial = emptyList())
        val visitedIds = remember(visitedDungeons) { visitedDungeons.map { it.osm_id }.toSet() }

        // 1. Beim Start: Alle früher erkundeten Bereiche aus DB laden
        LaunchedEffect(Unit) {
            val saved = withContext(Dispatchers.IO) { gameDb.exploredAreaDao().getAll() }
            exploredCenters = saved.map { GeoPoint(it.lat, it.lon) }
            // Falls noch nie etwas erkundet wurde, bleibt die Liste erstmal leer, 
            // bis das GPS das erste echte Signal schickt.
        }

        // 2. Initiales Laden entfernt: Warten jetzt auf echtes GPS-Signal

        // 3. ECHTES kontinuierliches GPS-Tracking
        DisposableEffect(Unit) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, // MUSS HIGH_ACCURACY für ein Laufspiel sein
                5000L // Update alle 5 Sekunden
            ).apply {
                setMinUpdateDistanceMeters(2f) // Nur wenn 2m Bewegung
                setWaitForAccurateLocation(false)
            }.build()

            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let { location ->
                        val prefs = context.getSharedPreferences("player_profile", Context.MODE_PRIVATE)
                        val latOffset = prefs.getFloat("lat_offset", 0f)
                        val lonOffset = prefs.getFloat("lon_offset", 0f)
                        
                        val newGeoPoint = GeoPoint(location.latitude + latOffset, location.longitude + lonOffset)
                        userLocation = newGeoPoint

                        // POIs nachladen wenn wir uns weit genug bewegt haben
                        coroutineScope.launch {
                            val newPlaces = withContext(Dispatchers.IO) {
                                val rawPlaces = db.placeDao().getPlacesInArea(
                                    location.latitude - 0.015, location.latitude + 0.015,
                                    location.longitude - 0.015, location.longitude + 0.015
                                )
                                rawPlaces.filter { place ->
                                    val results = FloatArray(1)
                                    Location.distanceBetween(location.latitude, location.longitude, place.lat, place.lon, results)
                                    results[0] <= 1000f
                                }
                            }
                            places = newPlaces
                        }
                    }
                }
            }

            try {
                fusedLocationClient.requestLocationUpdates(locationRequest, callback, context.mainLooper)
            } catch (e: SecurityException) { /* ignore */ }

            onDispose {
                fusedLocationClient.removeLocationUpdates(callback)
            }
        }

        // 4. Wenn der Spieler sich bewegt: neues Loch in den Nebel schneiden & in DB speichern
        LaunchedEffect(userLocation) {
            userLocation?.let { loc ->
                coroutineScope.launch(Dispatchers.IO) {
                    // Nur speichern wenn dieser Punkt noch nicht erkundet wurde
                    val alreadyExplored = gameDb.exploredAreaDao().isNearbyExplored(loc.latitude, loc.longitude)
                    if (alreadyExplored == 0) {
                        gameDb.exploredAreaDao().insert(ExploredArea(lat = loc.latitude, lon = loc.longitude))
                        // XP und Achievement-Counter erhoehen
                        PlayerProfile.incrementExplored(context)
                        val newCenter = GeoPoint(loc.latitude, loc.longitude)
                        withContext(Dispatchers.Main) {
                            exploredCenters = exploredCenters + listOf(newCenter)
                        }
                    }
                }
            }
        }

        // 5. Naechsten Dungeon pruefen und AUTOMATISCH loot (Passiv)
        LaunchedEffect(userLocation, places) {
            userLocation?.let { loc ->
                val prefs = context.getSharedPreferences("player_profile", Context.MODE_PRIVATE)
                val isMagnetMode = prefs.getBoolean("dev_magnet_mode", false)
                val detectionRadius = if (isMagnetMode) 500f else 20f

                val target = places.firstOrNull { place ->
                    val results = FloatArray(1)
                    android.location.Location.distanceBetween(
                        loc.latitude, loc.longitude,
                        place.lat, place.lon,
                        results
                    )
                    results[0] <= detectionRadius
                }

                target?.let { dungeon ->
                    coroutineScope.launch(Dispatchers.IO) {
                        val alreadyDone = gameDb.visitedDungeonDao().alreadyVisited(dungeon.osm_id)
                        if (alreadyDone == 0) {
                            val xpReward = when(dungeon.rarity) {
                                "epic" -> 200; "rare" -> 100; "uncommon" -> 50; else -> 25
                            }
                            gameDb.visitedDungeonDao().insert(VisitedDungeon(osm_id = dungeon.osm_id, xpEarned = xpReward))
                            PlayerProfile.incrementDungeons(context)
                            PlayerProfile.addXP(context, xpReward)
                            
                            withContext(Dispatchers.Main) {
                                dungeonJustEntered = true
                                // Notification
                                DungeonNotifier.notifyDungeonNearby(context, dungeon)
                            }
                        }
                    }
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize().background(CyberpunkBackground)) {
            if (userLocation != null) {
                OSMMapView(userLocation!!, places, exploredCenters, rumors, visitedIds, followPlayer, currentZoom)
                
                // --- HUD UNTEN ---
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // Links: Dungeon-Zähler
                        Surface(
                            color = CyberpunkBackground.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                Text("RADAR SCAN", color = CyberpunkNeonCyan, fontSize = 9.sp, letterSpacing = 2.sp)
                                Text("${places.size} Dungeons", color = androidx.compose.ui.graphics.Color.White,
                                     fontSize = 16.sp, fontWeight = FontWeight.Black)
                                Text("${exploredCenters.size} Sektoren", color = CyberpunkNeonPink, fontSize = 11.sp)
                            }
                        }
                        
                        // Rechts: Follow-Toggle Button
                        Surface(
                            color = if (followPlayer) CyberpunkNeonCyan.copy(alpha = 0.2f)
                                    else CyberpunkBackground.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(10.dp),
                            onClick = { followPlayer = !followPlayer }
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    if (followPlayer) "FOLGT" else "FREI",
                                    color = if (followPlayer) CyberpunkNeonCyan else androidx.compose.ui.graphics.Color.Gray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp
                                )
                            }
                        }
                    }
                }

                // Dynamic Zoom Gesture Area - RIGHT SIDE (xKours-Style)
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight(0.6f)
                        .width(48.dp)
                        .padding(end = 4.dp)
                        .pointerInput(Unit) {
                            detectVerticalDragGestures { _, dragAmount ->
                                // Drag nach oben = negativer dragAmount -> + zoom
                                // Faktor 150f macht es DEUTLICH weicher als 80f
                                val zoomDelta = -dragAmount / 150f
                                // "Infinite" Zoom-Boundaries (von Weltkarte bis maximal rein)
                                currentZoom = (currentZoom + zoomDelta).coerceIn(3f, 22f)
                            }
                        }
                ) {
                    // Visual indicator bar (Infinite-Style, kein füllender Balken mehr)
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(6.dp)
                            .fillMaxHeight(0.5f),
                        contentAlignment = Alignment.Center // Thumb sitzt im Leerlauf in der Mitte
                    ) {
                        // Hintergrund-Linie (Glow)
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(3.dp),
                            color = CyberpunkNeonCyan.copy(alpha = 0.1f),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, CyberpunkNeonCyan.copy(alpha = 0.3f))
                        ) {}
                        
                        // Kleiner Thumb in der Mitte, der einfach anzeigt "Hier kann gewischt werden"
                        // Füllt sich absichtlich nicht mehr, damit es sich endlos/infinite anfühlt!
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(20.dp)
                                .background(
                                    color = CyberpunkNeonCyan,
                                    shape = RoundedCornerShape(3.dp)
                                )
                        )
                    }
                }
            } else {
                Text("INITIATING SCAN...", color = CyberpunkNeonCyan, modifier = Modifier.align(Alignment.Center))
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize().background(CyberpunkBackground), contentAlignment = Alignment.Center) {
            Text("REQUIRE DEV MODE (GPS)", color = CyberpunkNeonPink)
        }
    }
}

// Helfer für leuchtende custom Icons
fun createNeonMarker(context: Context, color: Int, isPlayer: Boolean = false): BitmapDrawable {
    val size = if (isPlayer) 70 else 60 // Größer gemacht für bessere Sichtbarkeit
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    
    // Äußerer Glow
    val glowPaint = Paint().apply {
        isAntiAlias = true
        this.color = color
        style = Paint.Style.FILL
        setShadowLayer(22f, 0f, 0f, color) // Stärkerer Neon Glow
    }
    
    // Hellerer Kern für Dungeons
    val corePaint = Paint().apply {
        isAntiAlias = true
        this.color = Color.WHITE
        style = Paint.Style.FILL
    }
    
    val strokePaint = Paint().apply {
        isAntiAlias = true
        this.color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = if (isPlayer) 4f else 2.5f
    }

    if (isPlayer) {
        canvas.drawCircle(size / 2f, size / 2f, size / 3f, glowPaint)
        canvas.drawCircle(size / 2f, size / 2f, size / 2.5f, strokePaint)
    } else {
        // Dungeon Marker: Starker farbiger Hintergrund + weißer Kern + feiner Rand
        canvas.drawCircle(size / 2f, size / 2f, size / 3.5f, glowPaint)
        canvas.drawCircle(size / 2f, size / 2f, size / 8f, corePaint)
        canvas.drawCircle(size / 2f, size / 2f, size / 3.5f, strokePaint)
    }
    
    return BitmapDrawable(context.resources, bitmap)
}

@Composable
fun OSMMapView(center: GeoPoint, places: List<PlaceEntity>, exploredCenters: List<GeoPoint>, rumors: List<Rumor>, visitedIds: Set<String>, followPlayer: Boolean = true, currentZoom: Float = 17f) {
    val context = LocalContext.current
    
    // Cache the marker icons to prevent 60fps bitmap recreation during zoom
    val epicIcon = remember { createNeonMarker(context, Color.parseColor("#FFFF00E6"), false) }
    val rareIcon = remember { createNeonMarker(context, Color.parseColor("#FFFFD700"), false) }
    val uncommonIcon = remember { createNeonMarker(context, Color.parseColor("#FF00FF00"), false) }
    val normalIcon = remember { createNeonMarker(context, Color.parseColor("#FF555555"), false) }
    val rumorIcon = remember { createNeonMarker(context, Color.parseColor("#FFFF8C00"), false) } // Dark Orange für Gerüchte
    val clearedIcon = remember { createNeonMarker(context, Color.parseColor("#FF222222"), false) } // Dunkelgrau für erledigt
    
    // OSMDroid Setup MUSS vor der MapView Erstellung passieren!
    Configuration.getInstance().load(context, context.getSharedPreferences("osm", Context.MODE_PRIVATE))
    
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    
    // Zoom flüssig updaten, ohne die Marker neu zu zeichnen
    LaunchedEffect(currentZoom) {
        mapViewRef?.controller?.setZoom(currentZoom.toDouble())
    }
    
    // Spieler-Position flüssig updaten, ohne Marker neu zu zeichnen
    LaunchedEffect(center, followPlayer) {
        mapViewRef?.let { map ->
            val player = map.overlays.find { it is Marker && it.title == "Du bist hier" } as? Marker
            player?.position = center
            
            // Neon-Rand flüssig nachziehen
            val fog = map.overlays.find { it is FogOfWarOverlay } as? FogOfWarOverlay
            fog?.currentLocation = center
            
            if (followPlayer) {
                map.controller.animateTo(center)
            }
            map.invalidate()
        }
    }

    // Nur wenn places, rumors, exploredCenters oder visitedIds sich ändern, updaten wir die Overlays
    LaunchedEffect(places, exploredCenters, rumors, visitedIds) {
        val prefs = context.getSharedPreferences("player_profile", Context.MODE_PRIVATE)
        val isGodsEye = prefs.getBoolean("dev_gods_eye", false)

        mapViewRef?.let { mapView ->
            // Alle alten Overlays löschen außer dem Spieler und dem FogOfWar
            mapView.overlays.removeAll { it is Marker && it.title != "Du bist hier" }
            mapView.overlays.removeAll { it is Polygon }
            
            // FogOfWarOverlay updaten oder entfernen (God's Eye)
            if (isGodsEye) {
                mapView.overlays.removeAll { it is FogOfWarOverlay }
            } else {
                var fog = mapView.overlays.find { it is FogOfWarOverlay } as? FogOfWarOverlay
                if (fog == null) {
                    fog = FogOfWarOverlay(exploredCenters, center)
                    mapView.overlays.add(0, fog)
                } else {
                    fog.exploredAreas = exploredCenters
                }
            }
            
            places.forEach { place ->
                val isVisited = place.osm_id in visitedIds
                
                val cachedIcon = when {
                    isVisited -> clearedIcon
                    place.rarity == "epic" -> epicIcon
                    place.rarity == "rare" -> rareIcon
                    place.rarity == "uncommon" -> uncommonIcon
                    else -> normalIcon
                }
                
                val marker = Marker(mapView).apply {
                    position = GeoPoint(place.lat, place.lon)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    title = place.name.ifEmpty { "Unbekannter Ort" }
                    subDescription = "Typ: ${place.type} | Rarität: ${place.rarity}"
                    icon = cachedIcon
                }
                mapView.overlays.add(marker)
            }
            
            // Gerüchte als orange Marker zeichnen
            rumors.forEach { rumor ->
                if (rumor.lat != 0.0 && rumor.lon != 0.0) {
                    val marker = Marker(mapView).apply {
                        position = GeoPoint(rumor.lat, rumor.lon)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        title = "Gerücht von ${rumor.fromPlayer}"
                        subDescription = rumor.message
                        icon = rumorIcon
                    }
                    mapView.overlays.add(marker)
                }
            }
            
            mapView.invalidate()
        }
    }

    AndroidView(
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                // Initiale Zoomstufe wird im update Block gesetzt
                setMultiTouchControls(true)
                setBuiltInZoomControls(false) // Standard +/- Buttons ausblenden

                // Tap auf Karte schließt Info-Fenster
                val mapEventsOverlay = org.osmdroid.views.overlay.MapEventsOverlay(object : org.osmdroid.events.MapEventsReceiver {
                    override fun singleTapConfirmedHelper(p: org.osmdroid.util.GeoPoint?): Boolean {
                        org.osmdroid.views.overlay.infowindow.InfoWindow.closeAllInfoWindowsOn(this@apply)
                        return true
                    }
                    override fun longPressHelper(p: org.osmdroid.util.GeoPoint?): Boolean = false
                })
                overlays.add(0, mapEventsOverlay)
                
                // Dark Mode Matrix für Cyberpunk Radar-Look
                val inverseMatrix = ColorMatrix(floatArrayOf(
                    -1.0f, 0.0f, 0.0f, 0.0f, 255f,
                    0.0f, -1.0f, 0.0f, 0.0f, 255f,
                    0.0f, 0.0f, -1.0f, 0.0f, 255f,
                    0.0f, 0.0f, 0.0f, 1.0f, 0.0f
                ))
                overlayManager.tilesOverlay.setColorFilter(ColorMatrixColorFilter(inverseMatrix))
                
                // Spieler Marker
                val playerMarker = Marker(this).apply {
                    position = center
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    title = "Du bist hier"
                    icon = createNeonMarker(ctx, Color.parseColor("#00F3FF"), true)
                }
                overlays.add(playerMarker)
                mapViewRef = this
            }
        },
        update = { mapView ->
            // Nichts tun hier! Update der Overlays passiert im LaunchedEffect
        },
        modifier = Modifier.fillMaxSize()
    )
}
