package com.sad.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "visited_dungeons")
data class VisitedDungeon(
    @PrimaryKey
    val osm_id: String,
    val visitedAt: Long = System.currentTimeMillis(),
    val xpEarned: Int = 0
)
