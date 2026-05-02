package com.sad.app.data

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaceDao {
    // Finde alle Orte in einer bounding box (sehr effizient dank SQLite Index)
    @Query("SELECT * FROM places WHERE lat BETWEEN :minLat AND :maxLat AND lon BETWEEN :minLon AND :maxLon LIMIT 1500")
    suspend fun getPlacesInArea(minLat: Double, maxLat: Double, minLon: Double, maxLon: Double): List<PlaceEntity>
    
    @Query("SELECT * FROM places WHERE osm_id = :id")
    suspend fun getPlaceById(id: String): PlaceEntity?
}
