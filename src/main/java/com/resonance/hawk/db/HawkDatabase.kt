package com.resonance.hawk.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [RequestHistory::class], version = 1)
abstract class HawkDatabase: RoomDatabase() {
    abstract fun getRequestHistoryDao(): RequestHistoryDao
}