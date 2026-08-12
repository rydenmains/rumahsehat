package com.rumahsehat.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rumahsehat.data.dao.AssessmentDao
import com.rumahsehat.data.model.Assessment
import com.rumahsehat.data.model.ScoreItem

@Database(entities = [Assessment::class, ScoreItem::class], version = 3, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun assessmentDao(): AssessmentDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE assessments ADD COLUMN photoPathsJson TEXT")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE score_items ADD COLUMN optionIndex INTEGER NOT NULL DEFAULT -1")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rumah_sehat_db"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING) // PRD Constraint: WAL Mode enabled
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
