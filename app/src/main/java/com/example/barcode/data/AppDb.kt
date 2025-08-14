package com.example.barcode.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Item::class], version = 1, exportSchema = true)
abstract class AppDb : RoomDatabase() {
    abstract fun itemDao(): ItemDao

    companion object {
        @Volatile private var INSTANCE: AppDb? = null

        fun get(context: Context): AppDb =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDb::class.java,
                    "frigozen.db"
                )
                    // Pour un MVP, ça évite les crashs si tu changes le schéma.
                    // (En prod, préfère des vraies migrations.)
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
    }
}