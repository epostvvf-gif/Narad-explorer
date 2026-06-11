package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "scanned_files")
data class ScannedFile(
    @PrimaryKey val path: String,       // Full absolute path or Google Drive document ID
    val name: String,
    val size: Long,
    val mimeType: String,
    val category: String,               // "images", "videos", "audio", "documents", "apps", "downloads"
    val storageType: String,            // "PHONE", "SDCARD", "DRIVE"
    val lastModified: Long,
    val tags: String = "",              // Comma-separated tags, e.g. "Work,Urgent,Bill"
    val aiDescription: String? = null,  // AI suggested semantic context
    val isStarred: Boolean = false
)

@Entity(tableName = "file_tags")
data class FileTag(
    @PrimaryKey val name: String,       // E.g., "Work", "Personal", "Taxes", "Urgent"
    val colorHex: String,               // Accent color hex
    val isAiGenerated: Boolean = false
)

@Dao
interface FileDao {
    @Query("SELECT * FROM scanned_files ORDER BY lastModified DESC")
    fun getAllFilesFlow(): Flow<List<ScannedFile>>

    @Query("SELECT * FROM scanned_files ORDER BY lastModified DESC")
    suspend fun getAllFiles(): List<ScannedFile>

    @Query("SELECT * FROM scanned_files WHERE category = :category ORDER BY lastModified DESC")
    fun getFilesByCategory(category: String): Flow<List<ScannedFile>>

    @Query("SELECT * FROM scanned_files WHERE isStarred = 1 ORDER BY lastModified DESC")
    fun getStarredFiles(): Flow<List<ScannedFile>>

    @Query("SELECT * FROM scanned_files WHERE path = :path LIMIT 1")
    suspend fun getFileByPath(path: String): ScannedFile?

    @Query("SELECT * FROM scanned_files WHERE name LIKE :query OR tags LIKE :query OR aiDescription LIKE :query OR path LIKE :query")
    fun searchFiles(query: String): Flow<List<ScannedFile>>

    @Query("SELECT * FROM scanned_files WHERE tags LIKE :tagQuery")
    fun getFilesByTag(tagQuery: String): Flow<List<ScannedFile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiles(files: List<ScannedFile>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: ScannedFile)

    @Update
    suspend fun updateFile(file: ScannedFile)

    @Query("DELETE FROM scanned_files WHERE path = :path")
    suspend fun deleteFileByPath(path: String)

    @Query("DELETE FROM scanned_files")
    suspend fun clearAllFiles()

    // Tags Management
    @Query("SELECT * FROM file_tags ORDER BY name ASC")
    fun getAllTagsFlow(): Flow<List<FileTag>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: FileTag)

    @Query("DELETE FROM file_tags WHERE name = :name")
    suspend fun deleteTag(name: String)

    @Query("SELECT COUNT(*) FROM scanned_files")
    suspend fun getFilesCount(): Int
}

@Database(entities = [ScannedFile::class, FileTag::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fileDao(): FileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "narad_explorer_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
