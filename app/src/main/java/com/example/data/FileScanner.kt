package com.example.data

import android.content.Context
import android.util.Log
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class FileScanner {

    // Common system or hidden directories that should be excluded during local space indexing
    private val systemOrIrrelevantFolders = setOf(
        "android", 
        "data", 
        "obb", 
        "lost+found", 
        ".thumbnails", 
        ".trash", 
        "system volume information", 
        ".git", 
        ".idea", 
        "node_modules", 
        ".android",
        "cache"
    )

    /**
     * Recursively scans a root directory for local files.
     * Offers non-blocking I/O dispatching and processes metadata suitable for Gemini.
     *
     * @param rootDir The parent directory from which to begin recursive scanning.
     * @param storageType Either "PHONE" or "SDCARD" to denote path classification.
     * @param onProgress Callback invoked periodically with count progress and folder updates.
     */
    suspend fun scanDirectoryRecursively(
        rootDir: File,
        storageType: String = "PHONE",
        onProgress: (scannedCount: Int, currentFolder: String) -> Unit = { _, _ -> }
    ): List<ScannedFile> = withContext(Dispatchers.IO) {
        val fileList = mutableListOf<ScannedFile>()
        if (!rootDir.exists() || !rootDir.isDirectory) {
            Log.w("FileScanner", "Provided scanning path does not exist or isn't a directory: ${rootDir.absolutePath}")
            return@withContext emptyList()
        }

        var count = 0
        try {
            rootDir.walkTopDown()
                .onEnter { dir ->
                    val isSecretOrHidden = dir.name.startsWith(".") && dir.name != "."
                    val isSystemDir = systemOrIrrelevantFolders.contains(dir.name.lowercase())
                    val isAllowed = !isSecretOrHidden && !isSystemDir
                    
                    if (isAllowed) {
                        onProgress(count, dir.name)
                    } else {
                        Log.v("FileScanner", "Skipping excluded directory: ${dir.absolutePath}")
                    }
                    isAllowed
                }
                .onFail { dir, exception ->
                    Log.e("FileScanner", "Failed to access directory: ${dir.absolutePath} due to ${exception.message}")
                }
                .forEach { file ->
                    if (file.isFile) {
                        try {
                            val scannedFile = prepareMetadataForFile(file, storageType)
                            fileList.add(scannedFile)
                            count++
                            
                            // Let the caller update UI or status logs every 50 files
                            if (count % 50 == 0) {
                                onProgress(count, file.parentFile?.name ?: "")
                            }
                        } catch (e: Exception) {
                            Log.e("FileScanner", "Error analyzing metadata for file ${file.name}: ${e.message}")
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e("FileScanner", "Error during walking directory tree: ${e.message}", e)
        }

        Log.i("FileScanner", "Scan finished successfully. Found $count matching files in ${rootDir.absolutePath}.")
        return@withContext fileList
    }

    /**
     * Maps an actual java.io.File back to the standard "Narad Explorer" ScannedFile data model.
     */
    fun prepareMetadataForFile(file: File, storageType: String = "PHONE"): ScannedFile {
        val extension = file.extension.lowercase()
        val name = file.name
        val path = file.absolutePath
        val size = file.length()
        val lastModified = file.lastModified()

        // Extract system MIME-type if available, else derive standard generic type
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: when (extension) {
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            "pdf" -> "application/pdf"
            "apk" -> "application/vnd.android.package-archive"
            else -> "application/octet-stream"
        }

        // Standard Narad Explorer Categories mapping: "images", "videos", "audio", "documents", "apps", "downloads"
        val category = when (extension) {
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "svg", "heic" -> "images"
            "mp4", "mkv", "avi", "mov", "wmv", "flv", "3gp", "mpeg", "webm" -> "videos"
            "mp3", "wav", "flac", "ogg", "m4a", "wma", "aac", "mid" -> "audio"
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf", "odt", "csv", "html", "htm", "xml", "json" -> "documents"
            "apk", "aab" -> "apps"
            else -> "downloads"
        }

        return ScannedFile(
            path = path,
            name = name,
            size = size,
            mimeType = mimeType,
            category = category,
            storageType = storageType,
            lastModified = lastModified,
            tags = "Scanner,Local",
            aiDescription = "Local index scan of '$name' in ${file.parentFile?.name ?: "root"}."
        )
    }
}
