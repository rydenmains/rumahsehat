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

@Database(entities = [Assessment::class, ScoreItem::class], version = 5, exportSchema = true)
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

        /**
         * v3→v4 (v1.7): tambah kolom status 3-tier. Backfill dari isHealthy lama
         * (SEHAT↔true, TIDAK↔false); KURANG hanya untuk penilaian baru karena
         * subtotal inti tidak tersimpan di baris lama. Nol risiko: ADD + UPDATE.
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE assessments ADD COLUMN status TEXT NOT NULL DEFAULT 'TIDAK SEHAT'")
                db.execSQL("UPDATE assessments SET status = CASE WHEN isHealthy = 1 THEN 'SEHAT' ELSE 'TIDAK SEHAT' END")
            }
        }

        /** v4→v5: tambah kolom houseName (nama pemilik/alamat rumah). Nol risiko: ADD saja. */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE assessments ADD COLUMN houseName TEXT NOT NULL DEFAULT ''")
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
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING) // PRD Constraint: WAL Mode enabled
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
