package com.sad.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface VisitedDungeonDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(dungeon: VisitedDungeon)

    @Query("SELECT COUNT(*) FROM visited_dungeons WHERE osm_id = :osmId")
    suspend fun alreadyVisited(osmId: String): Int
    
    @Query("SELECT COUNT(*) FROM visited_dungeons")
    suspend fun totalCount(): Int
    
    @Query("SELECT * FROM visited_dungeons")
    suspend fun getAll(): List<VisitedDungeon>

    @Query("SELECT * FROM visited_dungeons")
    fun getAllFlow(): kotlinx.coroutines.flow.Flow<List<VisitedDungeon>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(dungeons: List<VisitedDungeon>)

    @Query("DELETE FROM visited_dungeons")
    suspend fun deleteAll()

    // Fuer Daily/Weekly Quest Fortschritt
    @Query("SELECT COUNT(*) FROM visited_dungeons WHERE visitedAt >= :since")
    suspend fun countSince(since: Long): Int

    @Query("SELECT osm_id FROM visited_dungeons WHERE visitedAt >= :since")
    suspend fun getOsmIdsSince(since: Long): List<String>
}

