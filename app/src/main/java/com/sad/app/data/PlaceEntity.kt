package com.sad.app.data

import androidx.room.Entity
import androidx.room.Ignore
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
) {
    // Addon-Erweiterungsfelder: werden nur im RAM gehalten (nicht in der Haupt-DB)
    // Room ignoriert diese Felder beim Schema-Check
    @Ignore var description: String? = null
    @Ignore var lore: String? = null
    @Ignore var xpReward: Int? = null
    @Ignore var questHint: String? = null
    @Ignore var iconColor: String? = null
    @Ignore var minZoom: Float? = null
}
