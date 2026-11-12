package com.sad.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.pm.ServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.sad.app.data.GameEngine
import com.sad.app.notifications.DungeonNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class LocationTrackingService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
        DungeonNotifier.createChannel(this)

        val notification = NotificationCompat.Builder(this, "location_tracking_channel")
            .setContentTitle("SAD: Radar Aktiv")
            .setContentText("Erkundet die Stadt und Dungeons im Hintergrund...")
            .setSmallIcon(android.R.drawable.ic_dialog_map) // System Fallback icon
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        // Nach einem START_STICKY-Neustart kann die Berechtigung inzwischen entzogen sein –
        // startForeground(type=location) würde dann ab Android 14 eine SecurityException werfen.
        val hasLocation = androidx.core.content.ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!hasLocation) {
            stopSelf()
            return
        }

        // Als Foreground Service starten, damit Android ihn nicht tötet
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(1337, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
            } else {
                startForeground(1337, notification)
            }
        } catch (e: Exception) {
            // z.B. ForegroundServiceStartNotAllowedException wenn aus dem Hintergrund gestartet
            stopSelf()
            return
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

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    serviceScope.launch {
                        GameEngine.processLocation(
                            this@LocationTrackingService,
                            location.latitude, location.longitude, location.time
                        )
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
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
        serviceScope.cancel()
    }
}
