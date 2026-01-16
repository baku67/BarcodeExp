package com.example.barcode.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.barcode.data.local.entities.ItemEntity
import com.example.barcode.data.local.dao.ItemDao

@Database(entities = [ItemEntity::class], version = 4, exportSchema = true)
abstract class AppDb : RoomDatabase() {

    abstract fun itemDao(): ItemDao

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


                    // Toujours à la fin
                    .build().also { INSTANCE = it }
            }
    }
}