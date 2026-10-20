package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents ORDER BY lastOpenedTimestamp DESC")
    fun getAllDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE isFavorite = 1 ORDER BY lastOpenedTimestamp DESC")
    fun getFavoriteDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE fileType = :type ORDER BY lastOpenedTimestamp DESC")
    fun getDocumentsByType(type: String): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id LIMIT 1")
    suspend fun getDocumentById(id: String): DocumentEntity?

    @Query("SELECT * FROM documents WHERE title LIKE '%' || :query || '%' ORDER BY lastOpenedTimestamp DESC")
    fun searchDocuments(query: String): Flow<List<DocumentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity)

    @Update
    suspend fun updateDocument(document: DocumentEntity)

    @Query("UPDATE documents SET lastPageRead = :page, progressPercentage = :progress, lastOpenedTimestamp = :timestamp WHERE id = :id")
    suspend fun updateReadingProgress(id: String, page: Int, progress: Int, timestamp: Long)

    @Query("UPDATE documents SET isFavorite = :isFav WHERE id = :id")
    suspend fun updateFavorite(id: String, isFav: Boolean)

    @Delete
    suspend fun deleteDocument(document: DocumentEntity)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteDocumentById(id: String)

    // Bookmarks
    @Query("SELECT * FROM bookmarks WHERE documentId = :documentId ORDER BY pageNumber ASC")
    fun getBookmarksForDocument(documentId: String): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmarkById(id: Long)

    @Query("DELETE FROM bookmarks WHERE documentId = :documentId AND pageNumber = :page")
    suspend fun deleteBookmarkForPage(documentId: String, page: Int)

    @Query("SELECT COUNT(*) > 0 FROM bookmarks WHERE documentId = :documentId AND pageNumber = :page")
    suspend fun isPageBookmarked(documentId: String, page: Int): Boolean
}
