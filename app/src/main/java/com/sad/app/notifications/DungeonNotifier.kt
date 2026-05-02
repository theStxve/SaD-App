package com.sad.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.sad.app.data.PlaceEntity

object DungeonNotifier {
    private const val CHANNEL_ID = "dungeon_discovery"
    private const val CHANNEL_NAME = "Dungeon Entdeckungen"
    private var notificationId = 1000

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Benachrichtigungen wenn du einen Dungeon entdeckst"
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun notifyDungeonNearby(context: Context, dungeon: PlaceEntity) {
        val xpReward = when(dungeon.rarity) {
            "epic" -> 200; "rare" -> 100; "uncommon" -> 50; else -> 25
        }
        val rarityLabel = when(dungeon.rarity) {
            "epic" -> "EPISCHER"; "rare" -> "SELTENER"; "uncommon" -> "UNGEWOEHNLICHER"; else -> ""
        }
        
        try {
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setContentTitle("$rarityLabel Dungeon in Sichtweite!")
                .setContentText("${dungeon.name.ifEmpty { "Unbekannter Ort" }} · Betritt ihn für +$xpReward XP")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()
            
            NotificationManagerCompat.from(context).notify(notificationId++, notification)
        } catch (e: SecurityException) {
            // Notification permission nicht erteilt - kein Problem, App läuft weiter
        }
    }
    
    fun notifyDungeonEntered(context: Context, dungeon: PlaceEntity, xpGained: Int) {
        try {
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentTitle("Dungeon abgeschlossen!")
                .setContentText("${dungeon.name.ifEmpty { "Dungeon" }} · +$xpGained XP verdient")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(true)
                .build()
            
            NotificationManagerCompat.from(context).notify(notificationId++, notification)
        } catch (e: SecurityException) { }
    }
}
