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
import androidx.compose.ui.BiasAlignment
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
import androidx.compose.ui.graphics.toArgb
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

fun calculateEffectiveExploredCenters(
    rawCenters: List<ExploredPoint>,
    forcePrecision: Boolean,
    precisionRadius: Double = 20.0,
    currentZoom: Float = 17f
): List<ExploredPoint> {
    if (rawCenters.isEmpty() || !forcePrecision) return rawCenters

    // LOD: Bei niedrigem Zoom keine teure Interpolation — Originalpunkte reichen
    // zoom < 13: Originalpunkte mit etwas größerem Radius zurückgeben (viel schneller)
    // zoom 13-15: Jeden 2. Schritt interpolieren
    // zoom >= 15: Volle Interpolation (8m Schritte)
    val (step, sampleEvery) = when {
        currentZoom < 13f -> return rawCenters.map { it.copy(radiusMeters = precisionRadius) }
        currentZoom < 15f -> Pair(16.0, 1)
        else              -> Pair(8.0, 1)
    }

    val result = ArrayList<ExploredPoint>(rawCenters.size * 5)
    var prev: ExploredPoint? = null
    val results = FloatArray(1)

    for (curr in rawCenters) {
        val p = prev
        if (p != null) {
            Location.distanceBetween(
                p.geoPoint.latitude, p.geoPoint.longitude,
                curr.geoPoint.latitude, curr.geoPoint.longitude,
                results
            )
            val dist = results[0].toDouble()
            if (dist > step && dist <= 350.0) {
                val stepsCount = kotlin.math.ceil(dist / step).toInt()
                for (i in 1 until stepsCount) {
                    val frac = i.toDouble() / stepsCount
                    val iLat = p.geoPoint.latitude + frac * (curr.geoPoint.latitude - p.geoPoint.latitude)
                    val iLon = p.geoPoint.longitude + frac * (curr.geoPoint.longitude - p.geoPoint.longitude)
                    result.add(ExploredPoint(GeoPoint(iLat, iLon), radiusMeters = precisionRadius))
                }
            }
        }
        result.add(ExploredPoint(curr.geoPoint, radiusMeters = precisionRadius))
        prev = curr
    }
    return result
}

@Composable
fun MapScreen(targetLocation: GeoPoint? = null) {
    val context = LocalContext.current
    val colors = LocalAppColors.current
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

    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                hasLocationPermission = ActivityCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (hasLocationPermission) {
        // Startposition auf null setzen, damit der "INITIATING SCAN..." Ladebildschirm gezeigt wird, bis echtes GPS da ist
        var userLocation by remember { mutableStateOf<GeoPoint?>(null) }
        var places by remember { mutableStateOf<List<PlaceEntity>>(emptyList()) }
        var exploredCenters by remember { mutableStateOf<List<ExploredPoint>>(emptyList()) }
        var followPlayer by remember { mutableStateOf(true) }
        var nearbyDungeon by remember { mutableStateOf<PlaceEntity?>(null) }
        var dungeonJustEntered by remember { mutableStateOf(false) }
        var dungeonAlreadyVisited by remember { mutableStateOf(false) }
        // xKours-Style stufenloser Zoom: direktes Level (3.0 bis 22.0)
        var currentZoom by remember { mutableStateOf(MapSettingsManager.current.rememberedZoom) }
        val rumors by gameDb.rumorDao().getAllFlow().collectAsState(initial = emptyList())
        val visitedDungeons by gameDb.visitedDungeonDao().getAllFlow().collectAsState(initial = emptyList())
        val visitedIds = remember(visitedDungeons) { visitedDungeons.map { it.osm_id }.toSet() }

        val mapSettings = MapSettingsManager.current

        val effectiveExploredCenters = remember(exploredCenters, mapSettings.forcePrecisionPaths, mapSettings.precisionModeEnabled, currentZoom) {
            calculateEffectiveExploredCenters(
                exploredCenters,
                mapSettings.forcePrecisionPaths,
                20.0,
                currentZoom
            )
        }

        // 1. Beim Start: Alle früher erkundeten Bereiche aus DB laden
        LaunchedEffect(Unit) {
            val saved = withContext(Dispatchers.IO) { gameDb.exploredAreaDao().getAll() }
            exploredCenters = saved.map { ExploredPoint(GeoPoint(it.lat, it.lon), radiusMeters = it.radius) }
        }

        // Caching für Addon Places um wiederholtes IO-Lesen bei jedem GPS-Tick zu vermeiden
        var cachedAddonPlaces by remember { mutableStateOf<List<PlaceEntity>>(emptyList()) }
        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                cachedAddonPlaces = com.sad.app.data.AddonManager.loadAllAddonPlaces(context)
            }
        }

        suspend fun loadPlacesForLocation(
            newGeoPoint: GeoPoint,
            vIds: Set<String>,
            showGlobalVisited: Boolean,
            addonPlaces: List<PlaceEntity>
        ): List<PlaceEntity> {
            val rawPlaces = db.placeDao().getPlacesInArea(
                newGeoPoint.latitude - 0.015, newGeoPoint.latitude + 0.015,
                newGeoPoint.longitude - 0.015, newGeoPoint.longitude + 0.015
            )
            val results = FloatArray(1)
            val mainFiltered = rawPlaces.filter { place ->
                Location.distanceBetween(newGeoPoint.latitude, newGeoPoint.longitude, place.lat, place.lon, results)
                results[0] <= 1000f
            }
            val addonFiltered = addonPlaces.filter { place ->
                Location.distanceBetween(newGeoPoint.latitude, newGeoPoint.longitude, place.lat, place.lon, results)
                results[0] <= 1000f
            }

            val globalVisitedPlaces = if (showGlobalVisited && vIds.isNotEmpty()) {
                val mainVisited = db.placeDao().getPlacesByIds(vIds.toList())
                val addonVisited = addonPlaces.filter { it.osm_id in vIds }
                mainVisited + addonVisited
            } else {
                emptyList()
            }

            return (mainFiltered + addonFiltered + globalVisitedPlaces).distinctBy { it.osm_id }
        }

        // POIs auch bei Einstellungs- oder Visited-Änderungen nachladen
        LaunchedEffect(userLocation, visitedIds, mapSettings.showVisitedDungeonsGlobally, cachedAddonPlaces) {
            userLocation?.let { loc ->
                val newPlaces = withContext(Dispatchers.IO) {
                    loadPlacesForLocation(loc, visitedIds, mapSettings.showVisitedDungeonsGlobally, cachedAddonPlaces)
                }
                places = newPlaces
            }
        }

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

                        // POIs nachladen – MIT Offset, damit Dev-Modus korrekt funktioniert
                        coroutineScope.launch(Dispatchers.IO) {
                            val newPlaces = loadPlacesForLocation(
                                newGeoPoint,
                                visitedIds,
                                MapSettingsManager.current.showVisitedDungeonsGlobally,
                                cachedAddonPlaces
                            )
                            withContext(Dispatchers.Main) {
                                places = newPlaces
                            }
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
                    val alreadyExplored = gameDb.exploredAreaDao().isNearbyExplored(loc.latitude, loc.longitude)
                    if (alreadyExplored == 0) {
                        val newCenter = GeoPoint(loc.latitude, loc.longitude)
                        val lastPoint = exploredCenters.lastOrNull()
                        val isConnectionMode = MapSettingsManager.current.connectionModeEnabled
                        val currentRadius = MapSettingsManager.current.visionRadiusMeters
                        
                        gameDb.exploredAreaDao().insert(ExploredArea(lat = loc.latitude, lon = loc.longitude, radius = currentRadius))
                        PlayerProfile.incrementExplored(context)
                        
                        val addedPoints = mutableListOf<ExploredPoint>()
                        if (isConnectionMode && lastPoint != null) {
                            val lastCenter = lastPoint.geoPoint
                            val step = currentRadius
                            val results = FloatArray(1)
                            Location.distanceBetween(lastCenter.latitude, lastCenter.longitude, newCenter.latitude, newCenter.longitude, results)
                            val dist = results[0].toDouble()
                            if (dist > step) {
                                val stepsCount = (dist / step).toInt()
                                for (i in 1 until stepsCount) {
                                    val frac = i.toDouble() / stepsCount
                                    val iLat = lastCenter.latitude + frac * (newCenter.latitude - lastCenter.latitude)
                                    val iLon = lastCenter.longitude + frac * (newCenter.longitude - lastCenter.longitude)
                                    gameDb.exploredAreaDao().insert(ExploredArea(lat = iLat, lon = iLon, radius = currentRadius))
                                    addedPoints.add(ExploredPoint(GeoPoint(iLat, iLon), radiusMeters = currentRadius))
                                }
                            }
                        }

                        val newExploredPoint = ExploredPoint(newCenter, radiusMeters = currentRadius)
                        withContext(Dispatchers.Main) {
                            val updateList = ArrayList<ExploredPoint>(exploredCenters.size + addedPoints.size + 1)
                            updateList.addAll(exploredCenters)
                            updateList.addAll(addedPoints)
                            updateList.add(newExploredPoint)
                            exploredCenters = updateList
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

        // Wenn ein Ortungs-Ziel von den Geruechten kommt: Follow Deaktivieren
        LaunchedEffect(targetLocation) {
            if (targetLocation != null) {
                followPlayer = false
            }
        }

        Box(modifier = Modifier.fillMaxSize().background(colors.bg)) {
            if (userLocation != null) {
                OSMMapView(userLocation!!, places, effectiveExploredCenters, rumors, visitedIds, followPlayer, currentZoom, targetLocation)
                
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
                            color = colors.surface.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                Text("RADAR SCAN", color = colors.primary, fontSize = 9.sp, letterSpacing = 2.sp)
                                Text("${places.size} Dungeons", color = colors.textPrimary,
                                     fontSize = 16.sp, fontWeight = FontWeight.Black)
                                Text("${exploredCenters.size} Sektoren", color = colors.accent, fontSize = 11.sp)
                            }
                        }
                        
                        // Rechts: Follow-Toggle Button
                        Surface(
                            color = if (followPlayer) colors.primary.copy(alpha = 0.2f)
                                    else colors.surface.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(10.dp),
                            onClick = { followPlayer = !followPlayer }
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    if (followPlayer) "FOLGT" else "FREI",
                                    color = if (followPlayer) colors.primary else colors.textSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp
                                )
                            }
                        }
                    }
                }

                // Dynamic Zoom Gesture Area - RIGHT SIDE
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight(0.6f)
                        .width(48.dp)
                        .padding(end = 4.dp)
                        .pointerInput(Unit) {
                            detectVerticalDragGestures { _, dragAmount ->
                                val zoomDelta = -dragAmount / 150f
                                currentZoom = (currentZoom + zoomDelta).coerceIn(3f, 22f)
                            }
                        }
                ) {
                    // Visual indicator bar
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(6.dp)
                            .fillMaxHeight(0.5f)
                    ) {
                        // Hintergrund-Linie (Glow)
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(3.dp),
                            color = colors.primary.copy(alpha = 0.1f),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.primary.copy(alpha = 0.3f))
                        ) {}
                        
                        // Dynamische Pille: zoom 22f (ganz oben) -> Bias -1f, zoom 3f (ganz unten) -> Bias +1f
                        val zoomFraction = ((currentZoom - 3f) / (22f - 3f)).coerceIn(0f, 1f)
                        val verticalBias = 1f - (zoomFraction * 2f) // 1f (unten) bis -1f (oben)

                        Box(
                            modifier = Modifier
                                .align(BiasAlignment(horizontalBias = 0f, verticalBias = verticalBias))
                                .fillMaxWidth()
                                .height(20.dp)
                                .background(
                                    color = colors.primary,
                                    shape = RoundedCornerShape(3.dp)
                                )
                        )
                    }
                }
            } else {
                Text("INITIATING SCAN...", color = colors.primary, modifier = Modifier.align(Alignment.Center))
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize().background(colors.bg), contentAlignment = Alignment.Center) {
            Text("AWAITING GPS PERMISSION...", color = colors.accent)
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
fun OSMMapView(
    center: GeoPoint,
    places: List<PlaceEntity>,
    exploredCenters: List<ExploredPoint>,
    rumors: List<Rumor>,
    visitedIds: Set<String>,
    followPlayer: Boolean = true,
    currentZoom: Float = 17f,
    targetLocation: GeoPoint? = null
) {
    val context = LocalContext.current
    val colors = LocalAppColors.current

    Configuration.getInstance().load(context, context.getSharedPreferences("osm", Context.MODE_PRIVATE))
    
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    // Cache the marker icons to prevent 60fps bitmap recreation during zoom
    val epicIcon = remember { createNeonMarker(context, Color.parseColor("#FFFF00E6"), false) }
    val rareIcon = remember { createNeonMarker(context, Color.parseColor("#FFFFD700"), false) }
    val uncommonIcon = remember { createNeonMarker(context, Color.parseColor("#FF00FF00"), false) }
    val normalIcon = remember { createNeonMarker(context, Color.parseColor("#FF555555"), false) }
    val rumorIcon = remember { createNeonMarker(context, Color.parseColor("#FFFF8C00"), false) } // Dark Orange für Gerüchte
    val clearedIcon = remember { createNeonMarker(context, Color.parseColor("#FF222222"), false) } // Dunkelgrau für erledigt

    // Wenn ein Zielort (z.B. aus Geruechten) uebergeben wird, Karte dorthin bewegen
    LaunchedEffect(targetLocation) {
        if (targetLocation != null) {
            mapViewRef?.controller?.animateTo(targetLocation)
        }
    }
    
    // Zoom flüssig updaten, ohne die Marker neu zu zeichnen
    LaunchedEffect(currentZoom) {
        mapViewRef?.let { map ->
            map.controller.setZoom(currentZoom.toDouble())
            if (followPlayer) {
                map.controller.setCenter(center)
            }
        }
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

    // Nur wenn places, rumors, exploredCenters, visitedIds oder Zoom sich ändern, updaten wir die Overlays
    LaunchedEffect(places, exploredCenters, rumors, visitedIds, currentZoom) {
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
                    fog = FogOfWarOverlay(
                        exploredCenters, center,
                        themeColor = colors.primary.toArgb(),
                        fogOpacity = MapSettingsManager.current.fogOpacity,
                        visionRadiusMeters = MapSettingsManager.current.visionRadiusMeters,
                        fogColor = colors.fogColor.toArgb()
                    )
                    mapView.overlays.add(0, fog)
                } else {
                    fog.exploredAreas = exploredCenters
                    fog.themeColor = colors.primary.toArgb()
                    fog.fogOpacity = MapSettingsManager.current.fogOpacity
                    fog.visionRadiusMeters = MapSettingsManager.current.visionRadiusMeters
                    fog.fogColor = colors.fogColor.toArgb()
                }
            }
            
            // LOD: Bei sehr niedrigem Zoom (Übersicht) nur wichtige Marker zeichnen
            // Nutzt currentZoom-Parameter (zuverlässig), nicht mapView.zoomLevelDouble (kann 0 sein beim Init)
            val mapZoom = currentZoom.toDouble()
            places.forEach { place ->
                val isVisited = place.osm_id in visitedIds

                // Zoom-basiertes Marker-Filtering – nur bei extremem Zoom-Out aktiv:
                // zoom < 10: nur Epic und Rare sichtbar
                // zoom 10-12: Epic, Rare, Uncommon (kein Normal/Visited-Spam)
                // zoom >= 12: ALLE Marker sichtbar (normaler Spielbereich)
                val skipMarker = when {
                    mapZoom < 10.0 -> place.rarity != "epic" && place.rarity != "rare"
                    mapZoom < 12.0 -> place.rarity == "common" || place.rarity == "" || (isVisited && place.rarity !in listOf("epic", "rare", "uncommon"))
                    else -> false
                }
                if (skipMarker) return@forEach

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

    // Reaktiver Kartenfilter & Nebel: update bei Theme-Wechsel ODER Slider/Setting-Änderung
    val mapSettings = MapSettingsManager.current
    LaunchedEffect(colors.isDark, colors.fogColor, mapSettings) {
        mapViewRef?.let { map ->
            // Wenn Light-Theme aktiv UND Invertierung aus → kein Filter
            val effectiveSettings = if (!colors.isDark)
                mapSettings.copy(isInverted = false)
            else
                mapSettings
            map.overlayManager.tilesOverlay.setColorFilter(
                MapSettingsManager.buildColorFilter(effectiveSettings)
            )
            // Nebel-Deckkraft, Radius & Farbe live aktualisieren
            (map.overlays.find { it is FogOfWarOverlay } as? FogOfWarOverlay)?.let {
                it.fogOpacity = mapSettings.fogOpacity
                it.visionRadiusMeters = mapSettings.visionRadiusMeters
                it.fogColor = colors.fogColor.toArgb()
                it.themeColor = colors.primary.toArgb()
            }
            map.invalidate()
        }
    }

    // Zoom-Stufe merken bei Änderung – debounced (300ms) damit nicht jeder Frame gespeichert wird
    LaunchedEffect(currentZoom) {
        if (currentZoom != mapSettings.rememberedZoom) {
            kotlinx.coroutines.delay(300L)
            MapSettingsManager.save(context, mapSettings.copy(rememberedZoom = currentZoom))
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
                
                // Initialer Kartenfilter (aus MapSettings laden)
                val initSettings = if (!colors.isDark)
                    MapSettingsManager.current.copy(isInverted = false)
                else
                    MapSettingsManager.current
                overlayManager.tilesOverlay.setColorFilter(
                    MapSettingsManager.buildColorFilter(initSettings)
                )
                
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
            // Tile-Filter live updaten wenn mapSettings sich ändert
            val effectiveSettings = if (!colors.isDark)
                mapSettings.copy(isInverted = false)
            else
                mapSettings
            mapView.overlayManager.tilesOverlay.setColorFilter(
                MapSettingsManager.buildColorFilter(effectiveSettings)
            )
            mapView.invalidate()
        },
        modifier = Modifier.fillMaxSize()
    )
}
