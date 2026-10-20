package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.BookmarkEntity
import com.example.data.local.DocumentEntity
import com.example.data.model.ReadingMode
import com.example.data.repository.DocumentRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class FilterTab(val title: String) {
    ALL("All Docs"),
    PDF("PDFs"),
    PPTX("Presentations"),
    FAVORITES("Favorites")
}

@OptIn(ExperimentalCoroutinesApi::class)
class DocumentViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DocumentRepository

    val activeTab = MutableStateFlow(FilterTab.ALL)
    val searchQuery = MutableStateFlow("")

    private val _selectedDocId = MutableStateFlow<String?>(null)
    private val _currentDoc = MutableStateFlow<DocumentEntity?>(null)
    val currentDocument: StateFlow<DocumentEntity?> = _currentDoc.asStateFlow()

    private val _readingMode = MutableStateFlow(ReadingMode.LIGHT)
    val readingMode: StateFlow<ReadingMode> = _readingMode.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = DocumentRepository(application, db.documentDao())
        viewModelScope.launch {
            repository.ensureSamplesLoaded()
        }
    }

    val allBookmarks: StateFlow<List<BookmarkEntity>> = repository.getAllBookmarks()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val documents: StateFlow<List<DocumentEntity>> = combine(
        repository.getAllDocuments(),
        activeTab,
        searchQuery
    ) { allDocs, tab, query ->
        var list = allDocs
        when (tab) {
            FilterTab.ALL -> Unit
            FilterTab.PDF -> list = list.filter { it.fileType == "PDF" }
            FilterTab.PPTX -> list = list.filter { it.fileType == "PPTX" || it.fileType == "PPT" }
            FilterTab.FAVORITES -> list = list.filter { it.isFavorite }
        }

        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            list = list.filter {
                it.title.lowercase().contains(q) || it.summary.lowercase().contains(q)
            }
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentDocumentBookmarks: StateFlow<List<BookmarkEntity>> = _selectedDocId
        .flatMapLatest { id ->
            if (id != null) {
                repository.getBookmarksForDocument(id)
            } else {
                kotlinx.coroutines.flow.flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectDocument(document: DocumentEntity) {
        _selectedDocId.value = document.id
        _currentDoc.value = document
    }

    fun selectDocumentById(id: String) {
        _selectedDocId.value = id
        viewModelScope.launch {
            val doc = repository.getDocumentById(id)
            _currentDoc.value = doc
        }
    }

    fun clearSelectedDocument() {
        _selectedDocId.value = null
        _currentDoc.value = null
    }

    fun importUri(uri: Uri, onResult: (DocumentEntity?) -> Unit) {
        viewModelScope.launch {
            _isImporting.value = true
            val imported = repository.importDocumentFromUri(uri)
            _isImporting.value = false
            if (imported != null) {
                selectDocument(imported)
            }
            onResult(imported)
        }
    }

    fun updateProgress(page: Int) {
        val doc = _currentDoc.value ?: return
        viewModelScope.launch {
            repository.updateReadingProgress(doc.id, page, doc.pageCount)
            _currentDoc.value = doc.copy(
                lastPageRead = page,
                progressPercentage = ((page.toFloat() / doc.pageCount.toFloat()) * 100).toInt().coerceIn(0, 100),
                lastOpenedTimestamp = System.currentTimeMillis()
            )
        }
    }

    fun toggleFavorite(document: DocumentEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(document.id, document.isFavorite)
            if (_currentDoc.value?.id == document.id) {
                _currentDoc.value = _currentDoc.value?.copy(isFavorite = !document.isFavorite)
            }
        }
    }

    fun deleteDocument(document: DocumentEntity) {
        viewModelScope.launch {
            repository.deleteDocument(document)
            if (_currentDoc.value?.id == document.id) {
                clearSelectedDocument()
            }
        }
    }

    fun addBookmark(pageNumber: Int, title: String, note: String) {
        val docId = _selectedDocId.value ?: return
        viewModelScope.launch {
            repository.addBookmark(docId, pageNumber, title, note)
        }
    }

    fun removeBookmark(id: Long) {
        viewModelScope.launch {
            repository.removeBookmark(id)
        }
    }

    fun setReadingMode(mode: ReadingMode) {
        _readingMode.value = mode
    }

    fun setActiveTab(tab: FilterTab) {
        activeTab.value = tab
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }
}
