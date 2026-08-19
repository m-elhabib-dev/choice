package com.choice.app.data.local

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class ChoiceDatabaseMigrationTest {

    private val TEST_DB_NAME = "migration-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ChoiceDatabase::class.java,
    )

    @Test
    fun migrate1To2_preservesExistingCoinsAndChoices() {
        helper.createDatabase(TEST_DB_NAME, 1).apply {
            execSQL("INSERT INTO coins (name, interaction_count, created_at, last_interaction_at) VALUES ('Breakfast', 5, 1000, 2000)")
            execSQL("INSERT INTO coins (name, interaction_count, created_at, last_interaction_at) VALUES ('Lunch', 0, 3000, NULL)")

            val coin1Id = compileStatement("SELECT last_insert_rowid()").simpleQueryForLong()
            execSQL("INSERT INTO choices (coin_id, text, position) VALUES ($coin1Id, 'Ful', 0)")
            execSQL("INSERT INTO choices (coin_id, text, position) VALUES ($coin1Id, 'Eggs', 1)")

            val coin2Id = compileStatement("SELECT last_insert_rowid()").simpleQueryForLong()
            execSQL("INSERT INTO choices (coin_id, text, position) VALUES ($coin2Id, 'Rice', 0)")
            execSQL("INSERT INTO choices (coin_id, text, position) VALUES ($coin2Id, 'Pasta', 1)")

            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB_NAME, 2, true, ChoiceDatabase.MIGRATION_1_2)

        val cursor = db.query("SELECT name, interaction_count, created_at, last_interaction_at, isFavorite, weightedEnabled, avoidLastResultEnabled FROM coins ORDER BY name")
        assertEquals(2, cursor.count)

        cursor.moveToFirst()
        assertEquals("Breakfast", cursor.getString(0))
        assertEquals(5, cursor.getInt(1))
        assertEquals(1000L, cursor.getLong(2))
        assertEquals(2000L, cursor.getLong(3))
        assertEquals(0, cursor.getInt(4))
        assertEquals(0, cursor.getInt(5))
        assertEquals(0, cursor.getInt(6))

        cursor.moveToNext()
        assertEquals("Lunch", cursor.getString(0))
        assertEquals(0, cursor.getInt(1))
        assertEquals(3000L, cursor.getLong(2))
        assertNull(cursor.getString(3))
        assertEquals(0, cursor.getInt(4))
        assertEquals(0, cursor.getInt(5))
        assertEquals(0, cursor.getInt(6))

        cursor.close()
    }

    @Test
    fun migrate1To2_choicesWeightDefaultIsNull() {
        helper.createDatabase(TEST_DB_NAME, 1).apply {
            execSQL("INSERT INTO coins (name, interaction_count, created_at, last_interaction_at) VALUES ('Test', 0, 1000, NULL)")
            val coinId = compileStatement("SELECT last_insert_rowid()").simpleQueryForLong()
            execSQL("INSERT INTO choices (coin_id, text, position) VALUES ($coinId, 'A', 0)")
            execSQL("INSERT INTO choices (coin_id, text, position) VALUES ($coinId, 'B', 1)")
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB_NAME, 2, true, ChoiceDatabase.MIGRATION_1_2)

        val cursor = db.query("SELECT text, weight FROM choices ORDER BY position")
        assertEquals(2, cursor.count)

        cursor.moveToFirst()
        assertEquals("A", cursor.getString(0))
        assertNull(cursor.getString(1))

        cursor.moveToNext()
        assertEquals("B", cursor.getString(0))
        assertNull(cursor.getString(1))

        cursor.close()
    }

    @Test
    fun migrate1To2_decisionsTableIsEmpty() {
        helper.createDatabase(TEST_DB_NAME, 1).apply {
            execSQL("INSERT INTO coins (name, interaction_count, created_at, last_interaction_at) VALUES ('Test', 0, 1000, NULL)")
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB_NAME, 2, true, ChoiceDatabase.MIGRATION_1_2)

        val cursor = db.query("SELECT COUNT(*) FROM decisions")
        cursor.moveToFirst()
        assertEquals(0, cursor.getInt(0))
        cursor.close()
    }

    @Test
    fun migrate1To2_canInsertAndQueryDecisions() {
        helper.createDatabase(TEST_DB_NAME, 1).apply {
            execSQL("INSERT INTO coins (name, interaction_count, created_at, last_interaction_at) VALUES ('Test', 0, 1000, NULL)")
            val coinId = compileStatement("SELECT last_insert_rowid()").simpleQueryForLong()
            execSQL("INSERT INTO choices (coin_id, text, position) VALUES ($coinId, 'A', 0)")
            val choiceId = compileStatement("SELECT last_insert_rowid()").simpleQueryForLong()
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB_NAME, 2, true, ChoiceDatabase.MIGRATION_1_2)

        db.execSQL("INSERT INTO decisions (coinId, choiceId, choiceTextSnapshot, decidedAt) VALUES (1, 1, 'A', 5000)")

        val cursor = db.query("SELECT coinId, choiceId, choiceTextSnapshot, decidedAt FROM decisions")
        assertEquals(1, cursor.count)
        cursor.moveToFirst()
        assertEquals(1L, cursor.getLong(0))
        assertEquals(1L, cursor.getLong(1))
        assertEquals("A", cursor.getString(2))
        assertEquals(5000L, cursor.getLong(3))
        cursor.close()
    }
}
