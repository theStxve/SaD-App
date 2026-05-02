package com.sad.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RumorDao {
    @Query("SELECT * FROM rumors ORDER BY receivedAt DESC LIMIT 50")
    fun getAllFlow(): Flow<List<Rumor>>

    @Insert
    suspend fun insert(rumor: Rumor)

    @Query("SELECT COUNT(*) FROM rumors WHERE isRead = 0")
    fun unreadCount(): Flow<Int>

    @Query("UPDATE rumors SET isRead = 1")
    suspend fun markAllRead()
}
