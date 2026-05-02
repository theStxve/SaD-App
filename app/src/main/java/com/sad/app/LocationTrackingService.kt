package com.sad.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.pm.ServiceInfo
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.sad.app.data.AppDatabase
import com.sad.app.data.ExploredArea
import com.sad.app.data.GameDatabase
import com.sad.app.data.PlayerProfile
import com.sad.app.data.VisitedDungeon
import com.sad.app.notifications.DungeonNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LocationTrackingService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()

        val notification = NotificationCompat.Builder(this, "location_tracking_channel")
            .setContentTitle("SAD: Radar Aktiv")
            .setContentText("Erkundet die Stadt und Dungeons im Hintergrund...")
            .setSmallIcon(android.R.drawable.ic_dialog_map) // System Fallback icon
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        // Als Foreground Service starten, damit Android ihn nicht tötet
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1337, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(1337, notification)
        }
        startTracking()
    }

    private fun startTracking() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10000L // Im Hintergrund reicht alle 10 Sekunden (schont den Akku etwas)
        ).apply {
            setMinUpdateDistanceMeters(5f) // Nur aktualisieren bei echten Bewegungen
        }.build()

        val db = AppDatabase.getDatabase(this)
        val gameDb = GameDatabase.getDatabase(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    serviceScope.launch {
                        processLocation(location, db, gameDb)
                    }
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, mainLooper)
        } catch (e: SecurityException) {
            // Permission fehlt (wird in MainActivity angefragt)
        }
    }

    private suspend fun processLocation(location: Location, db: AppDatabase, gameDb: GameDatabase) {
        val prefs = getSharedPreferences("player_profile", Context.MODE_PRIVATE)
        val latOffset = prefs.getFloat("lat_offset", 0f)
        val lonOffset = prefs.getFloat("lon_offset", 0f)
        
        val lat = location.latitude + latOffset
        val lon = location.longitude + lonOffset

        // 1. Fog of War im Hintergrund aufdecken!
        val alreadyExplored = gameDb.exploredAreaDao().isNearbyExplored(lat, lon)
        if (alreadyExplored == 0) {
            gameDb.exploredAreaDao().insert(ExploredArea(lat = lat, lon = lon))
            PlayerProfile.incrementExplored(this@LocationTrackingService)
        }

        // 2. Dungeons im Hintergrund automatisch looten/entdecken!
        val rawPlaces = db.placeDao().getPlacesInArea(
            lat - 0.005, lat + 0.005,
            lon - 0.005, lon + 0.005
        )
        
        // 30 Meter Toleranz für den Hintergrund (damit man nicht an der Hauswand scheitert)
        // Im Magnet-Modus auf 500m erhöht
        val isMagnetMode = prefs.getBoolean("dev_magnet_mode", false)
        val detectionRadius = if (isMagnetMode) 500f else 30f

        val nearbyDungeons = rawPlaces.filter { place ->
            val results = FloatArray(1)
            Location.distanceBetween(lat, lon, place.lat, place.lon, results)
            results[0] <= detectionRadius 
        }

        for (dungeon in nearbyDungeons) {
            val alreadyDone = gameDb.visitedDungeonDao().alreadyVisited(dungeon.osm_id)
            if (alreadyDone == 0) {
                val xpReward = when(dungeon.rarity) {
                    "epic" -> 200; "rare" -> 100; "uncommon" -> 50; else -> 25
                }
                gameDb.visitedDungeonDao().insert(VisitedDungeon(osm_id = dungeon.osm_id, xpEarned = xpReward))
                PlayerProfile.incrementDungeons(this@LocationTrackingService)
                PlayerProfile.addXP(this@LocationTrackingService, xpReward)

                // Push-Benachrichtigung an den Spieler!
                DungeonNotifier.notifyDungeonNearby(this@LocationTrackingService, dungeon)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "location_tracking_channel",
                "Hintergrund-Radar",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Zwingt das System, den Service neu zu starten, falls er doch mal gekillt wird (RAM voll)
        return START_STICKY 
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}
