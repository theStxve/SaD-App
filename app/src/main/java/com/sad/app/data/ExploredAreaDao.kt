package com.sad.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ExploredAreaDao {
    @Query("SELECT * FROM explored_areas ORDER BY id ASC")
    suspend fun getAll(): List<ExploredArea>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(area: ExploredArea)

    // Bounding-Box-Abfrage (nutzt den Index auf lat/lon statt Full Table Scan)
    @Query("SELECT COUNT(*) FROM explored_areas WHERE lat BETWEEN :minLat AND :maxLat AND lon BETWEEN :minLon AND :maxLon")
    suspend fun countInBox(minLat: Double, maxLat: Double, minLon: Double, maxLon: Double): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(areas: List<ExploredArea>)

    @Query("UPDATE explored_areas SET radius = :newRadius")
    suspend fun updateAllRadius(newRadius: Double)

    @Query("DELETE FROM explored_areas")
    suspend fun deleteAll()
}
