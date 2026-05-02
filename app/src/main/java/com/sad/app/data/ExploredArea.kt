package com.sad.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "explored_areas")
data class ExploredArea(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val lat: Double,
    val lon: Double,
    val radius: Double = 150.0,
    val timestamp: Long = System.currentTimeMillis()
)
