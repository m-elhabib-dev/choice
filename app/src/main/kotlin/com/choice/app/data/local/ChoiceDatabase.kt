package com.choice.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [CoinEntity::class, ChoiceEntity::class],
    version = 1,
)
abstract class ChoiceDatabase : RoomDatabase() {
    abstract fun coinDao(): CoinDao

    companion object {
        private const val DATABASE_NAME = "choice.db"

        fun create(context: Context): ChoiceDatabase =
            Room.databaseBuilder(context, ChoiceDatabase::class.java, DATABASE_NAME)
                .build()
    }
}
