package com.sad.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

import androidx.room.Index

@Entity(
    tableName = "places",
    indices = [
        Index(value = ["lat", "lon"], name = "idx_coords"),
        Index(value = ["category"], name = "idx_category"),
        Index(value = ["rarity"], name = "idx_rarity")
    ]
)
data class PlaceEntity(
    @PrimaryKey
    val osm_id: String,
    val name: String,
    val category: String,
    val type: String,
    val rarity: String,
    val lat: Double,
    val lon: Double
)
