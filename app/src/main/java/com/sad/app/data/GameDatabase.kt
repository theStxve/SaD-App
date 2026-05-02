package com.sad.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Separate Datenbank nur für Spielstand-Daten (explored_areas, etc.)
// Diese wird von Room selbst erstellt - NICHT aus den Assets
@Database(entities = [ExploredArea::class, Rumor::class, VisitedDungeon::class], version = 3, exportSchema = false)
abstract class GameDatabase : RoomDatabase() {
    abstract fun exploredAreaDao(): ExploredAreaDao
    abstract fun rumorDao(): RumorDao
    abstract fun visitedDungeonDao(): VisitedDungeonDao

    companion object {
        @Volatile
        private var INSTANCE: GameDatabase? = null

        fun getDatabase(context: Context): GameDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GameDatabase::class.java,
                    "game.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
