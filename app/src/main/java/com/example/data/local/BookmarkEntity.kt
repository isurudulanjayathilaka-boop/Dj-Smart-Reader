package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val documentId: String,
    val pageNumber: Int,
    val title: String,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
