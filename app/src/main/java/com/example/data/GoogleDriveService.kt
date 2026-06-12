package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class DriveFileResponse(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "mimeType") val mimeType: String,
    @Json(name = "size") val size: Long? = null,
    @Json(name = "modifiedTime") val modifiedTime: String? = null
)

@JsonClass(generateAdapter = true)
data class DriveFileListResponse(
    @Json(name = "files") val files: List<DriveFileResponse>? = null,
    @Json(name = "nextPageToken") val nextPageToken: String? = null
)

interface GoogleDriveApiService {
    @GET("drive/v3/files")
    suspend fun listFiles(
        @Header("Authorization") authorization: String,
        @Query("q") query: String? = null,
        @Query("fields") fields: String = "files(id, name, mimeType, size, modifiedTime), nextPageToken",
        @Query("pageSize") pageSize: Int = 100
    ): DriveFileListResponse

    @DELETE("drive/v3/files/{fileId}")
    suspend fun deleteFile(
        @Header("Authorization") authorization: String,
        @Path("fileId") fileId: String
    ): ResponseBody
}

object GoogleDriveRetrofitClient {
    private const val BASE_URL = "https://www.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val service: GoogleDriveApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
        retrofit.create(GoogleDriveApiService::class.java)
    }
}

class GoogleDriveService {
    suspend fun fetchCloudFiles(accessToken: String): List<ScannedFile> {
        val authHeader = "Bearer $accessToken"
        val q = "'mimeType' != 'application/vnd.google-apps.folder' and trashed = false"
        return try {
            val response = GoogleDriveRetrofitClient.service.listFiles(authHeader, q)
            val driveFiles = response.files ?: emptyList()
            driveFiles.map { df ->
                // Parse modifiedTime to epoch ms
                val epochMs = try {
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.ROOT)
                    sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                    sdf.parse(df.modifiedTime ?: "")?.time ?: System.currentTimeMillis()
                } catch (e: Exception) {
                    System.currentTimeMillis()
                }

                // Map mimeType to our custom categories
                val category = when {
                    df.mimeType.contains("image/", ignoreCase = true) -> "images"
                    df.mimeType.contains("video/", ignoreCase = true) -> "videos"
                    df.mimeType.contains("audio/", ignoreCase = true) -> "audio"
                    df.mimeType.contains("application/pdf", ignoreCase = true) || df.name.endsWith(".docx", ignoreCase = true) || df.name.endsWith(".pdf", ignoreCase = true) -> "documents"
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
                    tags = "DRIVE",
                    aiDescription = "गूगल ड्राइव फ़ाइल: ${df.name}"
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun deleteCloudFile(accessToken: String, fileId: String): Boolean {
        val authHeader = "Bearer $accessToken"
        return try {
            GoogleDriveRetrofitClient.service.deleteFile(authHeader, fileId)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
