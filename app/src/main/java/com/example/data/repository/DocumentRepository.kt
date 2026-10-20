package com.example.data.repository

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.local.BookmarkEntity
import com.example.data.local.DocumentDao
import com.example.data.local.DocumentEntity
import com.example.data.model.DocumentType
import com.example.data.sample.SampleDocumentProvider
import com.example.engine.PdfRendererEngine
import com.example.engine.PptxParserEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.util.UUID

class DocumentRepository(
    private val context: Context,
    private val dao: DocumentDao
) {

    suspend fun ensureSamplesLoaded() = withContext(Dispatchers.IO) {
        val existing = dao.getAllDocuments().firstOrNull()
        if (existing.isNullOrEmpty()) {
            val samples = SampleDocumentProvider.getInitialSampleDocuments(context)
            for (sample in samples) {
                dao.insertDocument(sample)
            }
        }
    }

    fun getAllDocuments(): Flow<List<DocumentEntity>> = dao.getAllDocuments()

    fun getFavoriteDocuments(): Flow<List<DocumentEntity>> = dao.getFavoriteDocuments()

    fun getDocumentsByType(type: String): Flow<List<DocumentEntity>> = dao.getDocumentsByType(type)

    fun searchDocuments(query: String): Flow<List<DocumentEntity>> = dao.searchDocuments(query)

    suspend fun getDocumentById(id: String): DocumentEntity? = dao.getDocumentById(id)

    suspend fun importDocumentFromUri(uri: Uri): DocumentEntity? = withContext(Dispatchers.IO) {
        try {
            var displayName = "Document_${System.currentTimeMillis()}"
            var fileSize = 0L

            // Try to resolve display name and size
            if (uri.scheme == "content") {
                val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                        if (nameIndex != -1) {
                            displayName = it.getString(nameIndex) ?: displayName
                        }
                        if (sizeIndex != -1) {
                            fileSize = it.getLong(sizeIndex)
                        }
                    }
                }
            } else if (uri.scheme == "file") {
                val path = uri.path
                if (path != null) {
                    val file = java.io.File(path)
                    displayName = file.name
                    fileSize = file.length()
                }
            }

            val docType = DocumentType.fromExtensionOrName(displayName)
            var pageCount = 1

            if (docType == DocumentType.PDF) {
                val engine = PdfRendererEngine(context, uri)
                val initResult = engine.initialize()
                if (initResult.isSuccess) {
                    pageCount = initResult.getOrDefault(1).coerceAtLeast(1)
                }
                engine.close()
            } else if (docType == DocumentType.PPTX || docType == DocumentType.PPT) {
                val slides = PptxParserEngine.parsePptx(context, uri)
                pageCount = slides.size.coerceAtLeast(1)
            }

            val entity = DocumentEntity(
                id = UUID.randomUUID().toString(),
                title = displayName,
                uriString = uri.toString(),
                fileType = docType.name,
                pageCount = pageCount,
                lastPageRead = 1,
                progressPercentage = 0,
                fileSizeBytes = fileSize,
                lastOpenedTimestamp = System.currentTimeMillis(),
                isFavorite = false,
                isSample = false,
                summary = "Imported ${docType.displayName} ($pageCount ${if (docType == DocumentType.PDF) "pages" else "slides"})"
            )

            dao.insertDocument(entity)
            entity
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun updateReadingProgress(documentId: String, page: Int, totalPages: Int) = withContext(Dispatchers.IO) {
        val total = totalPages.coerceAtLeast(1)
        val progress = ((page.toFloat() / total.toFloat()) * 100).toInt().coerceIn(0, 100)
        dao.updateReadingProgress(documentId, page, progress, System.currentTimeMillis())
    }

    suspend fun toggleFavorite(documentId: String, currentFav: Boolean) = withContext(Dispatchers.IO) {
        dao.updateFavorite(documentId, !currentFav)
    }

    suspend fun deleteDocument(document: DocumentEntity) = withContext(Dispatchers.IO) {
        dao.deleteDocument(document)
    }

    // Bookmarks
    fun getBookmarksForDocument(documentId: String): Flow<List<BookmarkEntity>> =
        dao.getBookmarksForDocument(documentId)

    fun getAllBookmarks(): Flow<List<BookmarkEntity>> = dao.getAllBookmarks()

    suspend fun addBookmark(documentId: String, pageNumber: Int, title: String, note: String) = withContext(Dispatchers.IO) {
        val bookmark = BookmarkEntity(
            documentId = documentId,
            pageNumber = pageNumber,
            title = title.ifBlank { "Page $pageNumber" },
            note = note,
            createdAt = System.currentTimeMillis()
        )
        dao.insertBookmark(bookmark)
    }

    suspend fun removeBookmark(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteBookmarkById(id)
    }

    suspend fun removeBookmarkForPage(documentId: String, page: Int) = withContext(Dispatchers.IO) {
        dao.deleteBookmarkForPage(documentId, page)
    }

    suspend fun isPageBookmarked(documentId: String, page: Int): Boolean = withContext(Dispatchers.IO) {
        dao.isPageBookmarked(documentId, page)
    }
}
