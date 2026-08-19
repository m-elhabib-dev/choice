package com.choice.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [CoinEntity::class, ChoiceEntity::class, DecisionEntity::class],
    version = 2,
)
abstract class ChoiceDatabase : RoomDatabase() {
    abstract fun coinDao(): CoinDao

    companion object {
        private const val DATABASE_NAME = "choice.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE coins ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE coins ADD COLUMN weightedEnabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE coins ADD COLUMN avoidLastResultEnabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE choices ADD COLUMN weight INTEGER DEFAULT NULL")
                db.execSQL(
                    """
                    CREATE TABLE decisions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        coinId INTEGER NOT NULL,
                        choiceId INTEGER,
                        choiceTextSnapshot TEXT NOT NULL,
                        decidedAt INTEGER NOT NULL,
                        FOREIGN KEY (coinId) REFERENCES coins(id) ON DELETE CASCADE,
                        FOREIGN KEY (choiceId) REFERENCES choices(id) ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX index_decisions_coinId ON decisions(coinId)")
                db.execSQL("CREATE INDEX index_decisions_choiceId ON decisions(choiceId)")
            }
        }

        fun create(context: Context): ChoiceDatabase =
            Room.databaseBuilder(context, ChoiceDatabase::class.java, DATABASE_NAME)
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
