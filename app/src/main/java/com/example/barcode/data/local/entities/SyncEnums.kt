package com.example.barcode.data.local.entities

enum class PendingOperation {
    NONE, CREATE, UPDATE, DELETE
}

enum class SyncState {
    OK, FAILED
}