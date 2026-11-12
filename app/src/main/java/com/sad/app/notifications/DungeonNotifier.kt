package com.sad.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.sad.app.MainActivity
import com.sad.app.data.PlaceEntity

object DungeonNotifier {
    private const val CHANNEL_ID = "dungeon_discovery"
    private const val CHANNEL_NAME = "Dungeon Entdeckungen"

    /** Muss vor der ersten Benachrichtigung laufen – ohne Channel verwirft Android 8+ sie still. */
    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Benachrichtigungen wenn du einen Dungeon plünderst"
                enableVibration(true)
            }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    fun notifyDungeonLooted(context: Context, dungeon: PlaceEntity, xpGained: Int) {
        val rarityLabel = when (dungeon.rarity) {
            "epic" -> "Epischer "; "rare" -> "Seltener "; "uncommon" -> "Ungewöhnlicher "; else -> ""
        }
        val openApp = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        try {
            createChannel(context)
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setContentTitle("${rarityLabel}Dungeon geplündert!")
                .setContentText("${dungeon.name.ifEmpty { "Unbekannter Ort" }} · +$xpGained XP")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(openApp)
                .setAutoCancel(true)
                .build()

            // Stabile ID pro Dungeon statt eines nicht thread-sicheren Zählers
            NotificationManagerCompat.from(context).notify(dungeon.osm_id.hashCode(), notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS nicht erteilt – Spiel läuft trotzdem weiter
        }
    }
}
