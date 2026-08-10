package com.sad.app.data

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaceDao {
    @Query("SELECT * FROM places WHERE lat BETWEEN :minLat AND :maxLat AND lon BETWEEN :minLon AND :maxLon LIMIT 1500")
    suspend fun getPlacesInArea(minLat: Double, maxLat: Double, minLon: Double, maxLon: Double): List<PlaceEntity>
    
    @Query("SELECT * FROM places WHERE osm_id = :id")
    suspend fun getPlaceById(id: String): PlaceEntity?

    @Query("SELECT * FROM places WHERE osm_id IN (:ids)")
    suspend fun getPlacesByIds(ids: List<String>): List<PlaceEntity>

    // Fuer retroaktive Rarity-Stats: Gibt Rarity-Strings fuer eine Liste von osm_ids zurueck
    @Query("SELECT rarity FROM places WHERE osm_id IN (:ids)")
    suspend fun getRaritiesForIds(ids: List<String>): List<String>
}

