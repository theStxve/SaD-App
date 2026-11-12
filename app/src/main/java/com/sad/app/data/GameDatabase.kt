package com.sad.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Separate Datenbank nur für Spielstand-Daten (explored_areas, etc.)
// Diese wird von Room selbst erstellt - NICHT aus den Assets.
//
// WICHTIG: Hier liegt der Fortschritt der Spieler. Bei Schema-Änderungen IMMER
// eine Migration schreiben, niemals destruktiv migrieren!
@Database(entities = [ExploredArea::class, Rumor::class, VisitedDungeon::class], version = 4, exportSchema = true)
abstract class GameDatabase : RoomDatabase() {
    abstract fun exploredAreaDao(): ExploredAreaDao
    abstract fun rumorDao(): RumorDao
    abstract fun visitedDungeonDao(): VisitedDungeonDao

    companion object {
        @Volatile
        private var INSTANCE: GameDatabase? = null

        /** v4: Index auf explored_areas(lat, lon) für schnelle Umkreis-Abfragen. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_explored_areas_lat_lon` ON `explored_areas` (`lat`, `lon`)")
            }
        }

        fun getDatabase(context: Context): GameDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    GameDatabase::class.java,
                    "game.db"
                )
                .addMigrations(MIGRATION_3_4)
                // Nur uralte Vorab-Versionen dürfen noch verworfen werden
                .fallbackToDestructiveMigrationFrom(1, 2)
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}
