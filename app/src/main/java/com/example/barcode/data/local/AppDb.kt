package com.example.barcode.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.barcode.data.local.entities.ItemEntity
import com.example.barcode.data.local.dao.ItemDao
import com.example.barcode.data.local.dao.ItemNoteDao
import com.example.barcode.data.local.entities.ItemNoteEntity

@TypeConverters(RoomConverters::class)
@Database(entities = [ItemEntity::class, ItemNoteEntity::class], version = 11, exportSchema = true)
abstract class AppDb : RoomDatabase() {

    abstract fun itemDao(): ItemDao
    abstract fun itemNoteDao(): ItemNoteDao

    companion object {

        @Volatile private var INSTANCE: AppDb? = null

        // Migration 1 -> 2 (ajout de colonnes) penser à +1 le num version au dessus
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE items ADD COLUMN imageIngredientsUrl TEXT")
                db.execSQL("ALTER TABLE items ADD COLUMN imageNutritionUrl TEXT")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE items ADD COLUMN addMode TEXT NOT NULL DEFAULT 'barcode_scan'")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE items ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'PENDING_CREATE';")
                db.execSQL("ALTER TABLE items ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {

                // ✅ Ajout du tombstone deletedAt (NULL par défaut)
                if (!columnExists(db, "items", "deletedAt")) {
                    db.execSQL("ALTER TABLE items ADD COLUMN deletedAt INTEGER")
                }

                // ✅ Optionnel mais utile: index pour filtrer vite (deletedAt IS NULL) + syncStatus
                db.execSQL("CREATE INDEX IF NOT EXISTS index_items_deletedAt ON items(deletedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_items_syncStatus ON items(syncStatus)")

                // ✅ Propre: évite updatedAt=0 sur les rows existantes (sinon ta sync peut avoir des comportements bizarres)
                db.execSQL("UPDATE items SET updatedAt = (strftime('%s','now') * 1000) WHERE updatedAt = 0")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                if (!columnExists(db, "items", "serverUpdatedAt")) {
                    db.execSQL("ALTER TABLE items ADD COLUMN serverUpdatedAt INTEGER")
                }
                db.execSQL("CREATE INDEX IF NOT EXISTS index_items_serverUpdatedAt ON items(serverUpdatedAt)")
            }
        }


        // remplacement syncStatus par pendingOperation + syncState
        // + ajout lastSyncError / failedAt
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {

                db.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS items_new (
                id TEXT NOT NULL PRIMARY KEY,
                pendingOperation TEXT NOT NULL DEFAULT 'NONE',
                syncState TEXT NOT NULL DEFAULT 'OK',
                lastSyncError TEXT,
                failedAt INTEGER,
                updatedAt INTEGER NOT NULL DEFAULT 0,
                serverUpdatedAt INTEGER,

                barcode TEXT,
                name TEXT,
                brand TEXT,
                imageUrl TEXT,
                imageIngredientsUrl TEXT,
                imageNutritionUrl TEXT,
                nutriScore TEXT,
                addedAt INTEGER,
                deletedAt INTEGER,
                expiryDate INTEGER,
                addMode TEXT NOT NULL DEFAULT 'barcode_scan'
            )
            """.trimIndent()
                )

                db.execSQL(
                    """
            INSERT INTO items_new (
                id, pendingOperation, syncState, lastSyncError, failedAt,
                updatedAt, serverUpdatedAt,
                barcode, name, brand,
                imageUrl, imageIngredientsUrl, imageNutritionUrl, nutriScore,
                addedAt, deletedAt, expiryDate, addMode
            )
            SELECT
                id,
                CASE syncStatus
                    WHEN 'PENDING_CREATE' THEN 'CREATE'
                    WHEN 'PENDING_EDIT'   THEN 'UPDATE'
                    WHEN 'PENDING_DELETE' THEN 'DELETE'
                    ELSE 'NONE'
                END AS pendingOperation,
                CASE syncStatus
                    WHEN 'FAILED' THEN 'FAILED'
                    ELSE 'OK'
                END AS syncState,
                NULL AS lastSyncError,
                NULL AS failedAt,
                updatedAt,
                serverUpdatedAt,
                barcode, name, brand,
                imageUrl, imageIngredientsUrl, imageNutritionUrl, nutriScore,
                addedAt, deletedAt, expiryDate,
                COALESCE(addMode, 'barcode_scan')
            FROM items
            """.trimIndent()
                )

                db.execSQL("DROP TABLE items")
                db.execSQL("ALTER TABLE items_new RENAME TO items")

                db.execSQL("CREATE INDEX IF NOT EXISTS index_items_deletedAt ON items(deletedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_items_pendingOperation ON items(pendingOperation)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_items_syncState ON items(syncState)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_items_serverUpdatedAt ON items(serverUpdatedAt)")
            }
        }


        // Ajout ItemNote
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS item_notes (
                id TEXT NOT NULL PRIMARY KEY,
                itemId TEXT NOT NULL,
                body TEXT NOT NULL,
                createdAt INTEGER NOT NULL DEFAULT 0,
                deletedAt INTEGER,
                pendingOperation TEXT NOT NULL DEFAULT 'NONE',
                syncState TEXT NOT NULL DEFAULT 'OK',
                lastSyncError TEXT,
                failedAt INTEGER,
                updatedAt INTEGER NOT NULL DEFAULT 0,
                serverUpdatedAt INTEGER
            )
            """.trimIndent()
                )

                db.execSQL("CREATE INDEX IF NOT EXISTS index_item_notes_itemId ON item_notes(itemId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_item_notes_deletedAt ON item_notes(deletedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_item_notes_pendingOperation ON item_notes(pendingOperation)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_item_notes_syncState ON item_notes(syncState)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_item_notes_serverUpdatedAt ON item_notes(serverUpdatedAt)")
            }
        }

        // Ajout pinned sur ItemNote
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                if (!columnExists(db, "item_notes", "pinned")) {
                    db.execSQL("ALTER TABLE item_notes ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0")
                }
                db.execSQL("CREATE INDEX IF NOT EXISTS index_item_notes_itemId_pinned_createdAt ON item_notes(itemId, pinned, createdAt)")
            }
        }

        // Scan/manual différentiation
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE items ADD COLUMN manualType TEXT")
                db.execSQL("ALTER TABLE items ADD COLUMN manualSubtype TEXT")
                db.execSQL("ALTER TABLE items ADD COLUMN manualMetaJson TEXT")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE items ADD COLUMN photoId TEXT")
            }
        }


        private fun columnExists(db: SupportSQLiteDatabase, table: String, column: String): Boolean {
            db.query("PRAGMA table_info($table)").use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) {
                    if (cursor.getString(nameIndex) == column) return true
                }
            }
            return false
        }

        fun get(context: Context): AppDb =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDb::class.java,
                    "frigozen.db"
                )
                    // TODO: ATTENTION Erase la bdd a chaque changement de version de schéma etc...
                    // Permet de pas crash lors de changement de schéma sans nouvelle migration
                    // Mais nécessite quand même de changer le numero de version
                    // TODO En prod faire et passer des migrations !!!
                    // ** .fallbackToDestructiveMigration()

                    // Choix Migration en Dev (plus propre):
                    .addMigrations(MIGRATION_1_2)
                    .addMigrations(MIGRATION_2_3)
                    .addMigrations(MIGRATION_3_4)
                    .addMigrations(MIGRATION_4_5)
                    .addMigrations(MIGRATION_5_6)
                    .addMigrations(MIGRATION_6_7)
                    .addMigrations(MIGRATION_7_8)
                    .addMigrations(MIGRATION_8_9)
                    .addMigrations(MIGRATION_9_10)
                    .addMigrations(MIGRATION_10_11)

                    // Toujours à la fin
                    .build().also { INSTANCE = it }
            }
    }
}