package com.sad.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface VisitedDungeonDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE) // IGNORE = kein Doppeleintrag möglich
    suspend fun insert(dungeon: VisitedDungeon)

    @Query("SELECT COUNT(*) FROM visited_dungeons WHERE osm_id = :osmId")
    suspend fun alreadyVisited(osmId: String): Int
    
    @Query("SELECT COUNT(*) FROM visited_dungeons")
    suspend fun totalCount(): Int
    
    @Query("SELECT * FROM visited_dungeons")
    suspend fun getAll(): List<VisitedDungeon>

    @Query("SELECT * FROM visited_dungeons")
    fun getAllFlow(): kotlinx.coroutines.flow.Flow<List<VisitedDungeon>>
}
