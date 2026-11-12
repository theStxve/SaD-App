package com.sad.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "explored_areas",
    indices = [Index(value = ["lat", "lon"])]
)
data class ExploredArea(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val lat: Double,
    val lon: Double,
    val radius: Double = 150.0,
    val timestamp: Long = System.currentTimeMillis()
)
