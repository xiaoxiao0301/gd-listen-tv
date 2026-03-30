package com.xiaoxiao0301.amberplay.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "search_history",
    indices   = [Index(value = ["keyword"], unique = true)]
)
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")           val id: Long = 0L,
    @ColumnInfo(name = "keyword")      val keyword: String,
    @ColumnInfo(name = "searched_at")  val searchedAt: Long,
)
