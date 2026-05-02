package com.sad.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rumors")
data class Rumor(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val placeName: String,       // Name des Dungeons
    val placeType: String,       // Art des Dungeons (historic, amenity, etc.)
    val lat: Double,
    val lon: Double,
    val message: String,         // Der eigentliche Gerüchte-Text
    val fromPlayer: String,      // Generierter Spieler-Nickname des Senders
    val receivedAt: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
