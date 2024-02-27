package com.resonance.hawk.db

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface RequestHistoryDao {
    @Insert
    fun insert(requestHistory: RequestHistory)

    @Query("SELECT * FROM RequestHistory")
    fun getAll(): PagingSource<Int, RequestHistory>

    @Query("DELETE FROM RequestHistory")
    fun truncate()
}