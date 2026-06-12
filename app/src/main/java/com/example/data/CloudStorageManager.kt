package com.example.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Handles Google Drive authentication state, OAuth token validation,
 * and provides methods to specifically list files from the user's root folder.
 */
class CloudStorageManager {

    private var currentAccessToken: String? = "mock-oauth-access-token-998877"
    private var isUserAuthenticated = false

    // Required Google Drive scopes
    val requiredScopes = listOf(
        "https://www.googleapis.com/auth/drive.readonly",
        "https://www.googleapis.com/auth/drive.metadata.readonly"
    )

    /**
     * Authenticates with Google Drive using a given access token.
     */
    fun authenticate(accessToken: String): Boolean {
        if (accessToken.isNotBlank()) {
            currentAccessToken = accessToken
            isUserAuthenticated = true
            Log.d("CloudStorageManager", "Google Drive Authenticated successfully with token.")
            return true
        }
        return false
    }

    /**
     * Checks if currently authenticated.
     */
    fun isAuthenticated(): Boolean {
        return isUserAuthenticated || currentAccessToken != null
    }

    /**
     * Clears authentication credentials on sign out.
     */
    fun clearAuth() {
        currentAccessToken = null
        isUserAuthenticated = false
        Log.d("CloudStorageManager", "Credentials cleared.")
    }

    /**
     * Lists files from the user's Google Drive Root folder ('root' in parents).
     * Maps them seamlessly to ScannedFile models for standard rendering in the app.
     */
    suspend fun listRootFolderFiles(): List<ScannedFile> = withContext(Dispatchers.IO) {
        val token = currentAccessToken ?: return@withContext emptyList()
        val authHeader = "Bearer $token"
        
        // This query specifically lists items in the 'root' parent folder
        val q = "'root' in parents and mimeType != 'application/vnd.google-apps.folder' and trashed = false"
        
        Log.d("CloudStorageManager", "Fetching root files with query: $q")
        
        return@withContext try {
            val response = GoogleDriveRetrofitClient.service.listFiles(
                authorization = authHeader,
                query = q,
                pageSize = 100
            )
            val driveFiles = response.files ?: emptyList()
            Log.d("CloudStorageManager", "Successfully retrieved ${driveFiles.size} files from Drive root.")
            
            driveFiles.map { df ->
                val epochMs = try {
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.ROOT)
                    sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                    sdf.parse(df.modifiedTime ?: "")?.time ?: System.currentTimeMillis()
                } catch (e: Exception) {
                    System.currentTimeMillis()
                }

                // Categorize based on mimeType / file extension
                val category = when {
                    df.mimeType.contains("image/", ignoreCase = true) -> "images"
                    df.mimeType.contains("video/", ignoreCase = true) -> "videos"
                    df.mimeType.contains("audio/", ignoreCase = true) -> "audio"
                    df.mimeType.contains("application/pdf", ignoreCase = true) || 
                            df.name.endsWith(".docx", ignoreCase = true) || 
                            df.name.endsWith(".pdf", ignoreCase = true) -> "documents"
                    df.mimeType.contains("application/vnd.android.package-archive", ignoreCase = true) -> "apps"
                    else -> "downloads"
                }

                ScannedFile(
                    path = "drive/${df.id}",
                    name = df.name,
                    size = df.size ?: 0L,
                    mimeType = df.mimeType,
                    category = category,
                    storageType = "DRIVE",
                    lastModified = epochMs,
                    tags = "Drive,Root",
                    aiDescription = "गूगल ड्राइव रूट फ़ाइल: ${df.name}"
                )
            }
        } catch (e: Exception) {
            Log.e("CloudStorageManager", "Failed to contact Google Drive API: ${e.message}", e)
            emptyList()
        }
    }
}
