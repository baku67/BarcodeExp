package com.example.barcode.data.local

import androidx.room.TypeConverter
import com.example.barcode.data.local.entities.PendingOperation
import com.example.barcode.data.local.entities.SyncState

class RoomConverters {

    @TypeConverter
    fun pendingOperationToString(value: PendingOperation?): String? = value?.name

    @TypeConverter
    fun stringToPendingOperation(value: String?): PendingOperation =
        try { PendingOperation.valueOf(value ?: PendingOperation.NONE.name) }
        catch (_: Exception) { PendingOperation.NONE }

    @TypeConverter
    fun syncStateToString(value: SyncState?): String? = value?.name

    @TypeConverter
    fun stringToSyncState(value: String?): SyncState =
        try { SyncState.valueOf(value ?: SyncState.OK.name) }
        catch (_: Exception) { SyncState.OK }
}