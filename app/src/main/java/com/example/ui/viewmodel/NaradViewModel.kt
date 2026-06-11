package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface ScanUiState {
    object Idle : ScanUiState
    data class Scanning(val progress: Int, val description: String) : ScanUiState
    data class Success(val filesFound: Int) : ScanUiState
}

data class StorageSpaceInfo(
    val totalSize: Long = 128_000_000_000L, // 128 GB Total simulated
    val usedSize: Long = 0L,
    val junkSize: Long = 0L,
    val duplicateCount: Int = 0,
    val largeFilesCount: Int = 0,
    val apkCount: Int = 0,
    val tempFilesCount: Int = 0
)

class NaradViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FileRepository(application)

    // Scanning Status
    private val _scanState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val scanState: StateFlow<ScanUiState> = _scanState.asStateFlow()

    // File Collections
    val allFiles: StateFlow<List<ScannedFile>> = repository.allFilesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val starredFiles: StateFlow<List<ScannedFile>> = repository.starredFilesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tagsList: StateFlow<List<FileTag>> = repository.allTagsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Category lists
    val imagesList: StateFlow<List<ScannedFile>> = repository.getFilesByCategory("images")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val videosList: StateFlow<List<ScannedFile>> = repository.getFilesByCategory("videos")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val audioList: StateFlow<List<ScannedFile>> = repository.getFilesByCategory("audio")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val docsList: StateFlow<List<ScannedFile>> = repository.getFilesByCategory("documents")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val appsList: StateFlow<List<ScannedFile>> = repository.getFilesByCategory("apps")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadsList: StateFlow<List<ScannedFile>> = repository.getFilesByCategory("downloads")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search results (Regular & Semantic)
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSemanticSearch = MutableStateFlow(false)
    val isSemanticSearch: StateFlow<Boolean> = _isSemanticSearch.asStateFlow()

    private val _searchResults = MutableStateFlow<List<ScannedFile>>(emptyList())
    val searchResults: StateFlow<List<ScannedFile>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // Storage Utilization Space Calculator
    val storageSpace: StateFlow<StorageSpaceInfo> = allFiles.map { filesList ->
        val totalUsed = filesList.sumOf { it.size }
        val duplicates = filesList.groupBy { it.name }.filter { it.value.size > 1 }.values.flatten()
        val largeFiles = filesList.filter { it.size > 15_000_000L } // > 15MB
        val apks = filesList.filter { it.mimeType == "application/vnd.android.package-archive" || it.name.endsWith(".apk") }
        val tempFiles = filesList.filter { it.name.contains(".tmp") || it.name.contains(".log") || it.name.contains(".cache") }

        // Junk size = apks + temp + duplicates (excluding one master file)
        val dupJunkSize = filesList.groupBy { it.name }
            .filter { it.value.size > 1 }
            .map { it.value.drop(1).sumOf { f -> f.size } }
            .sum()
        val tempJunkSize = tempFiles.sumOf { it.size }
        val apkJunkSize = apks.sumOf { it.size }

        StorageSpaceInfo(
            usedSize = totalUsed,
            junkSize = dupJunkSize + tempJunkSize + apkJunkSize,
            duplicateCount = duplicates.size / 2,
            largeFilesCount = largeFiles.size,
            apkCount = apks.size,
            tempFilesCount = tempFiles.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StorageSpaceInfo())

    init {
        // Run initial fast scan to set up database
        triggerScan(includePhone = true, includeSdCard = true, includeDrive = true)
        
        // Setup initial default tags in db
        viewModelScope.launch {
            repository.insertTag(FileTag("Work", "#00ACC1"))
            repository.insertTag(FileTag("Personal", "#4CAF50"))
            repository.insertTag(FileTag("Taxes", "#E65100"))
            repository.insertTag(FileTag("Urgent", "#D84315"))
        }

        // Auto filter list search of local DB
        viewModelScope.launch {
            searchQuery.collectLatest { query ->
                if (query.isNotEmpty() && !isSemanticSearch.value) {
                    _isSearching.value = true
                    repository.searchFilesFlow(query).collect { list ->
                        _searchResults.value = list
                        _isSearching.value = false
                    }
                } else if (query.isEmpty()) {
                    _searchResults.value = emptyList()
                }
            }
        }
    }

    // Trigger full background scan
    fun triggerScan(includePhone: Boolean, includeSdCard: Boolean, includeDrive: Boolean) {
        viewModelScope.launch {
            _scanState.value = ScanUiState.Scanning(0, "स्कैन प्रारंभ...")
            try {
                val found = repository.scanStorage(includePhone, includeSdCard, includeDrive) { count, phase ->
                    _scanState.value = ScanUiState.Scanning(count, phase)
                }
                _scanState.value = ScanUiState.Success(found)
            } catch (e: Exception) {
                _scanState.value = ScanUiState.Success(0)
            }
        }
    }

    // Change Search mode
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isEmpty()) {
            _searchResults.value = emptyList()
        }
    }

    fun setSemanticSearchMode(enabled: Boolean) {
        _isSemanticSearch.value = enabled
        // Re-execute search if query is active
        if (_searchQuery.value.isNotEmpty()) {
            triggerSearch()
        }
    }

    fun triggerSearch() {
        val query = _searchQuery.value
        if (query.isEmpty()) return

        viewModelScope.launch {
            _isSearching.value = true
            if (_isSemanticSearch.value) {
                // Run deep AI semantical analysis
                val list = repository.performSemanticSearch(query)
                _searchResults.value = list
            } else {
                // Instant local query
                repository.searchFilesFlow(query).firstOrNull()?.let { list ->
                    _searchResults.value = list
                }
            }
            _isSearching.value = false
        }
    }

    // Perform Local Clean-up of junk files (Duplicated / Obsolete installers)
    fun cleanSelectedJunk() {
        viewModelScope.launch {
            val filesList = allFiles.value
            // Gather temp, duplicates (skipping master), and APKs to clean
            val temps = filesList.filter { it.name.contains(".tmp") || it.name.contains(".log") || it.name.contains(".cache") }
            val apks = filesList.filter { it.name.endsWith(".apk") }
            val dups = filesList.groupBy { it.name }
                .filter { it.value.size > 1 }
                .flatMap { it.value.drop(1) } // drop first master

            val toClean = temps + apks + dups
            for (f in toClean) {
                repository.deleteFileRecord(f.path)
            }
        }
    }

    // File operations
    fun toggleStarred(file: ScannedFile) {
        viewModelScope.launch {
            repository.toggleStar(file)
        }
    }

    fun deleteFile(file: ScannedFile) {
        viewModelScope.launch {
            repository.deleteFileRecord(file.path)
        }
    }

    fun addTagToFile(file: ScannedFile, tag: String) {
        viewModelScope.launch {
            val updatedTags = if (file.tags.isEmpty()) {
                tag
            } else {
                val list = file.tags.split(",").map { it.trim() }.toMutableList()
                if (!list.contains(tag)) {
                    list.add(tag)
                    // Create tag globally if not present
                    if (!tagsList.value.any { it.name.equals(tag, ignoreCase = true) }) {
                        repository.insertTag(FileTag(tag, "#E65100"))
                    }
                }
                list.joinToString(",")
            }
            repository.updateFileTags(file.path, updatedTags)
        }
    }

    fun removeTagFromFile(file: ScannedFile, tag: String) {
        viewModelScope.launch {
            val list = file.tags.split(",").map { it.trim() }.toMutableList()
            list.remove(tag)
            repository.updateFileTags(file.path, list.joinToString(","))
        }
    }

    // Dynamic AI Tag Recommendations
    private val _suggestedTags = MutableStateFlow<List<String>>(emptyList())
    val suggestedTags: StateFlow<List<String>> = _suggestedTags.asStateFlow()

    private val _isLoadingSuggestions = MutableStateFlow(false)
    val isLoadingSuggestions: StateFlow<Boolean> = _isLoadingSuggestions.asStateFlow()

    fun fetchAiTagSuggestions(file: ScannedFile) {
        viewModelScope.launch {
            _isLoadingSuggestions.value = true
            val recs = repository.suggestTagsWithAi(file.name, file.tags)
            _suggestedTags.value = recs
            _isLoadingSuggestions.value = false
        }
    }
}
