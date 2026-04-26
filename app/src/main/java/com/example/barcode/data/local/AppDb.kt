package com.example.barcode.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.barcode.data.local.dao.ItemDao
import com.example.barcode.data.local.dao.ItemNoteDao
import com.example.barcode.data.local.dao.ShoppingListDao
import com.example.barcode.data.local.entities.ItemEntity
import com.example.barcode.data.local.entities.ItemNoteEntity
import com.example.barcode.data.local.entities.ShoppingListItemEntity

@TypeConverters(RoomConverters::class)
@Database(
    entities = [
        ItemEntity::class,
        ItemNoteEntity::class,
        ShoppingListItemEntity::class,
    ],
    version = 15,
    exportSchema = true
)
abstract class AppDb : RoomDatabase() {

    abstract fun itemDao(): ItemDao
    abstract fun itemNoteDao(): ItemNoteDao
    abstract fun shoppingListDao(): ShoppingListDao

    companion object {

        @Volatile
        private var INSTANCE: AppDb? = null

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
                db.execSQL("ALTER TABLE items ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'PENDING_CREATE'")
                db.execSQL("ALTER TABLE items ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                if (!columnExists(db, "items", "deletedAt")) {
                    db.execSQL("ALTER TABLE items ADD COLUMN deletedAt INTEGER")
                }

                db.execSQL("CREATE INDEX IF NOT EXISTS index_items_deletedAt ON items(deletedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_items_syncStatus ON items(syncStatus)")
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

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                if (!columnExists(db, "item_notes", "pinned")) {
                    db.execSQL("ALTER TABLE item_notes ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0")
                }
                db.execSQL("CREATE INDEX IF NOT EXISTS index_item_notes_itemId_pinned_createdAt ON item_notes(itemId, pinned, createdAt)")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE items ADD COLUMN manualType TEXT")
                db.execSQL("ALTER TABLE items ADD COLUMN manualSubtype TEXT")
                db.execSQL("ALTER TABLE items ADD COLUMN manualMetaJson TEXT")
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE items ADD COLUMN photoId TEXT")
            }
        }

        /**
         * v11 -> v12 : première table locale de liste de courses
         */
        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS shopping_list_items (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        quantity TEXT,
                        note TEXT,
                        isImportant INTEGER NOT NULL,
                        isFavorite INTEGER NOT NULL,
                        isChecked INTEGER NOT NULL,
                        scope TEXT NOT NULL,
                        category TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * v12 -> v13 : ajoute le contexte home/user + audit simple
         */
        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS shopping_list_items_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        homeId TEXT NOT NULL,
                        scope TEXT NOT NULL,
                        ownerUserId TEXT,
                        name TEXT NOT NULL,
                        quantity TEXT,
                        note TEXT,
                        isImportant INTEGER NOT NULL,
                        isFavorite INTEGER NOT NULL,
                        isChecked INTEGER NOT NULL,
                        category TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        createdByUserId TEXT NOT NULL,
                        updatedByUserId TEXT NOT NULL
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    INSERT INTO shopping_list_items_new (
                        id,
                        homeId,
                        scope,
                        ownerUserId,
                        name,
                        quantity,
                        note,
                        isImportant,
                        isFavorite,
                        isChecked,
                        category,
                        createdAt,
                        updatedAt,
                        createdByUserId,
                        updatedByUserId
                    )
                    SELECT
                        id,
                        '${ShoppingListItemEntity.LOCAL_HOME_ID}',
                        scope,
                        CASE
                            WHEN scope = 'PERSONAL' THEN '${ShoppingListItemEntity.LOCAL_USER_ID}'
                            ELSE NULL
                        END,
                        name,
                        quantity,
                        note,
                        isImportant,
                        isFavorite,
                        isChecked,
                        category,
                        createdAt,
                        updatedAt,
                        '${ShoppingListItemEntity.LOCAL_USER_ID}',
                        '${ShoppingListItemEntity.LOCAL_USER_ID}'
                    FROM shopping_list_items
                    """.trimIndent()
                )

                db.execSQL("DROP TABLE shopping_list_items")
                db.execSQL("ALTER TABLE shopping_list_items_new RENAME TO shopping_list_items")
            }
        }

        /**
         * v13 -> v14 : passe la table shopping au vrai schéma syncable
         */
        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS shopping_list_items_new (
                        id TEXT NOT NULL PRIMARY KEY,

                        pendingOperation TEXT NOT NULL DEFAULT 'NONE',
                        syncState TEXT NOT NULL DEFAULT 'OK',
                        lastSyncError TEXT,
                        failedAt INTEGER,
                        updatedAt INTEGER NOT NULL DEFAULT 0,
                        serverUpdatedAt INTEGER,

                        homeId TEXT NOT NULL,
                        scope TEXT NOT NULL,
                        ownerUserId TEXT,

                        name TEXT NOT NULL,
                        quantity TEXT,
                        note TEXT,
                        isImportant INTEGER NOT NULL DEFAULT 0,
                        isFavorite INTEGER NOT NULL DEFAULT 0,
                        isChecked INTEGER NOT NULL DEFAULT 0,
                        category TEXT NOT NULL,

                        createdAt INTEGER NOT NULL DEFAULT 0,
                        deletedAt INTEGER,

                        createdByUserId TEXT NOT NULL,
                        updatedByUserId TEXT NOT NULL
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    INSERT INTO shopping_list_items_new (
                        id,
                        pendingOperation,
                        syncState,
                        lastSyncError,
                        failedAt,
                        updatedAt,
                        serverUpdatedAt,
                        homeId,
                        scope,
                        ownerUserId,
                        name,
                        quantity,
                        note,
                        isImportant,
                        isFavorite,
                        isChecked,
                        category,
                        createdAt,
                        deletedAt,
                        createdByUserId,
                        updatedByUserId
                    )
                    SELECT
                        id,
                        'NONE',
                        'OK',
                        NULL,
                        NULL,
                        updatedAt,
                        NULL,
                        homeId,
                        scope,
                        ownerUserId,
                        name,
                        quantity,
                        note,
                        isImportant,
                        isFavorite,
                        isChecked,
                        category,
                        createdAt,
                        NULL,
                        createdByUserId,
                        updatedByUserId
                    FROM shopping_list_items
                    """.trimIndent()
                )

                db.execSQL("DROP TABLE shopping_list_items")
                db.execSQL("ALTER TABLE shopping_list_items_new RENAME TO shopping_list_items")

                db.execSQL("CREATE INDEX IF NOT EXISTS index_shopping_list_items_deletedAt ON shopping_list_items(deletedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_shopping_list_items_pendingOperation ON shopping_list_items(pendingOperation)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_shopping_list_items_syncState ON shopping_list_items(syncState)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_shopping_list_items_serverUpdatedAt ON shopping_list_items(serverUpdatedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_shopping_list_items_homeId ON shopping_list_items(homeId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_shopping_list_items_scope ON shopping_list_items(scope)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_shopping_list_items_ownerUserId ON shopping_list_items(ownerUserId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_shopping_list_items_isChecked ON shopping_list_items(isChecked)")
            }
        }


        // ShoppingListItem: category devient nullable (default OTHER) + nouvelles clés métier.
        /**
         * v14 -> v15 : remplace les anciennes catégories shopping par les nouvelles clés métier.
         *
         * La catégorie reste obligatoire.
         * Si aucune catégorie valide n'est trouvée, on force OTHER.
         */
        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS shopping_list_items_new (
                id TEXT NOT NULL PRIMARY KEY,

                pendingOperation TEXT NOT NULL DEFAULT 'NONE',
                syncState TEXT NOT NULL DEFAULT 'OK',
                lastSyncError TEXT,
                failedAt INTEGER,
                updatedAt INTEGER NOT NULL DEFAULT 0,
                serverUpdatedAt INTEGER,

                homeId TEXT NOT NULL,
                scope TEXT NOT NULL,
                ownerUserId TEXT,

                name TEXT NOT NULL,
                quantity TEXT,
                note TEXT,
                isImportant INTEGER NOT NULL DEFAULT 0,
                isFavorite INTEGER NOT NULL DEFAULT 0,
                isChecked INTEGER NOT NULL DEFAULT 0,
                category TEXT NOT NULL DEFAULT 'OTHER',

                createdAt INTEGER NOT NULL DEFAULT 0,
                deletedAt INTEGER,

                createdByUserId TEXT NOT NULL,
                updatedByUserId TEXT NOT NULL
            )
            """.trimIndent()
                )

                db.execSQL(
                    """
            INSERT INTO shopping_list_items_new (
                id,
                pendingOperation,
                syncState,
                lastSyncError,
                failedAt,
                updatedAt,
                serverUpdatedAt,
                homeId,
                scope,
                ownerUserId,
                name,
                quantity,
                note,
                isImportant,
                isFavorite,
                isChecked,
                category,
                createdAt,
                deletedAt,
                createdByUserId,
                updatedByUserId
            )
            SELECT
                id,
                pendingOperation,
                syncState,
                lastSyncError,
                failedAt,
                updatedAt,
                serverUpdatedAt,
                homeId,
                scope,
                ownerUserId,
                name,
                quantity,
                note,
                isImportant,
                isFavorite,
                isChecked,
                CASE category
                    WHEN 'FRAIS' THEN 'FRESH'
                    WHEN 'FRUITS_LEGUMES' THEN 'FRUITS/VEGE'
                    WHEN 'VIANDE' THEN 'MEAT'
                    WHEN 'POISSON' THEN 'FISH'
                    WHEN 'MAISON' THEN 'HOME'

                    WHEN 'FRESH' THEN 'FRESH'
                    WHEN 'FRUITS/VEGE' THEN 'FRUITS/VEGE'
                    WHEN 'MEAT' THEN 'MEAT'
                    WHEN 'FISH' THEN 'FISH'
                    WHEN 'SWEET' THEN 'SWEET'
                    WHEN 'SALTY' THEN 'SALTY'
                    WHEN 'FROZEN' THEN 'FROZEN'
                    WHEN 'HOME' THEN 'HOME'
                    WHEN 'OTHER' THEN 'OTHER'

                    ELSE 'OTHER'
                END,
                createdAt,
                deletedAt,
                createdByUserId,
                updatedByUserId
            FROM shopping_list_items
            """.trimIndent()
                )

                db.execSQL("DROP TABLE shopping_list_items")
                db.execSQL("ALTER TABLE shopping_list_items_new RENAME TO shopping_list_items")

                db.execSQL("CREATE INDEX IF NOT EXISTS index_shopping_list_items_deletedAt ON shopping_list_items(deletedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_shopping_list_items_pendingOperation ON shopping_list_items(pendingOperation)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_shopping_list_items_syncState ON shopping_list_items(syncState)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_shopping_list_items_serverUpdatedAt ON shopping_list_items(serverUpdatedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_shopping_list_items_homeId ON shopping_list_items(homeId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_shopping_list_items_scope ON shopping_list_items(scope)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_shopping_list_items_ownerUserId ON shopping_list_items(ownerUserId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_shopping_list_items_isChecked ON shopping_list_items(isChecked)")
            }
        }




        private fun columnExists(
            db: SupportSQLiteDatabase,
            table: String,
            column: String
        ): Boolean {
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
                    .addMigrations(MIGRATION_11_12)
                    .addMigrations(MIGRATION_12_13)
                    .addMigrations(MIGRATION_13_14)
                    .addMigrations(MIGRATION_14_15)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}