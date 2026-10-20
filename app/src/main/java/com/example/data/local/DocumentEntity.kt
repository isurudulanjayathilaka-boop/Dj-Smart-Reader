package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val uriString: String,
    val fileType: String, // "PDF", "PPTX", "TXT", "DOCX"
    val pageCount: Int,
    val lastPageRead: Int = 1,
    val progressPercentage: Int = 0,
    val fileSizeBytes: Long = 0L,
    val lastOpenedTimestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val isSample: Boolean = false,
    val summary: String = ""
)
