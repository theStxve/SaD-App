package com.sad.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ExploredAreaDao {
    @Query("SELECT * FROM explored_areas")
    suspend fun getAll(): List<ExploredArea>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(area: ExploredArea)
    
    // Prüfe ob der Punkt schon erkundet wurde (innerhalb von ~50m)
    // Das verhindert, dass wir tausende identische Einträge speichern
    @Query("SELECT COUNT(*) FROM explored_areas WHERE ABS(lat - :lat) < 0.0005 AND ABS(lon - :lon) < 0.0005")
    suspend fun isNearbyExplored(lat: Double, lon: Double): Int
}
