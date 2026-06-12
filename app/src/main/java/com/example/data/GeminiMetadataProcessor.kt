package com.example.data

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

class GeminiMetadataProcessor {

    private val apiKey: String = BuildConfig.GEMINI_API_KEY

    /**
     * Analyzes file metadata using Gemini to generate descriptive tags (e.g., "Invoice", "Finance").
     */
    suspend fun generateTagsForFile(file: ScannedFile): List<String> = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.d("GeminiMetadataProcessor", "API Key is empty or mock. Using fallback tag generator.")
            return@withContext getFallbackTags(file)
        }

        val prompt = """
            You are a metadata assistant.
            Analyze the following file details to generate 3 to 5 highly relevant, single-word organizing tags (in English or Hindi).
            File Name: ${file.name}
            File Path: ${file.path}
            Category: ${file.category}
            File Size: ${file.size} bytes
            Storage Type: ${file.storageType}
            AI Description: ${file.aiDescription ?: "None"}

            Respond with ONLY a comma-separated list of tags, with no quotes, no markdown formatting, and no extra text.
            Example: Receipt,Finance,Travel,Tax
        """.trimIndent()

        try {
            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt))))
            )
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val textResponse = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim() ?: ""
            if (textResponse.isNotEmpty()) {
                val tags = textResponse.split(",")
                    .map { it.trim().removeSurrounding("\"").removeSurrounding("'") }
                    .filter { it.isNotEmpty() && it.length < 20 }
                if (tags.isNotEmpty()) {
                    return@withContext tags
                }
            }
        } catch (e: Exception) {
            Log.e("GeminiMetadataProcessor", "Error suggesting tags with Gemini: ${e.message}", e)
        }
        return@withContext getFallbackTags(file)
    }

    /**
     * Performs a semantic search over a list of ScannedFiles using their name, paths, category, and AI descriptions.
     * Returns a list of paths of the matching files, sorted by relevance.
     */
    suspend fun performSemanticSearch(query: String, files: List<ScannedFile>): List<String> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        if (files.isEmpty()) return@withContext emptyList()

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.d("GeminiMetadataProcessor", "API Key is empty or mock. Falling back to local search.")
            return@withContext localFallbackSearch(query, files)
        }

        // To fit context limits and optimize, we only pass up to 50 files for semantic ranking
        val candidateFiles = files.take(50)
        
        val filesMetadataJson = JSONArray().apply {
            candidateFiles.forEach { file ->
                put(org.json.JSONObject().apply {
                    put("path", file.path)
                    put("name", file.name)
                    put("category", file.category)
                    put("ai_description", file.aiDescription ?: "")
                    put("tags", file.tags)
                })
            }
        }.toString()

        val prompt = """
            You are a semantic search engine for the "Narad Explorer" file organizer.
            A user is searching their filesystem for: "$query"
            
            Here with is a JSON array representing the available files and their metadata descriptions:
            $filesMetadataJson
            
            Identify which of these files match the user's semantic query. Consider synonym matching, contextual connection, categories, and AI descriptions (for example, if they search for "urgent work document", match PDFs/DOCX described as business contracts or reports).
            
            Return ONLY a valid JSON array of matching unique file paths in order of relevance (highest to lowest). Ensure the paths match exactly. If no files match, return [].
            Do NOT include any markdown formatting, no ```json tags, no explanations, separate text, or headings. Your entire response must be a single parsable JSON array.
        """.trimIndent()

        try {
            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt))))
            )
            val response = RetrofitClient.service.generateContent(apiKey, request)
            var textResponse = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim() ?: ""
            
            // Strip markdown block formatting if present
            if (textResponse.startsWith("```")) {
                textResponse = textResponse.replace("```json", "").replace("```", "").trim()
            }

            if (textResponse.startsWith("[") && textResponse.endsWith("]")) {
                val jsonArray = JSONArray(textResponse)
                val results = mutableListOf<String>()
                for (i in 0 until jsonArray.length()) {
                    val path = jsonArray.optString(i)
                    if (path.isNotEmpty()) {
                        results.add(path)
                    }
                }
                if (results.isNotEmpty()) {
                    return@withContext results
                }
            }
        } catch (e: Exception) {
            Log.e("GeminiMetadataProcessor", "Error performing semantic search with Gemini: ${e.message}", e)
        }

        return@withContext localFallbackSearch(query, files)
    }

    private fun getFallbackTags(file: ScannedFile): List<String> {
        val fallback = mutableListOf<String>()
        val extension = file.name.substringAfterLast('.', "").uppercase()
        if (extension.isNotEmpty()) {
            fallback.add(extension)
        }
        val storageLabel = file.storageType.lowercase().replaceFirstChar { it.uppercase() }
        fallback.add(storageLabel)

        when (file.category) {
            "images" -> fallback.addAll(listOf("Media", "Photo"))
            "videos" -> fallback.addAll(listOf("Media", "Video"))
            "audio" -> fallback.addAll(listOf("Media", "Audio"))
            "documents" -> fallback.addAll(listOf("Doc", "Read-Only"))
            "apps" -> fallback.addAll(listOf("System", "App"))
            else -> fallback.add("FolderItem")
        }
        return fallback.take(4)
    }

    private fun localFallbackSearch(query: String, files: List<ScannedFile>): List<String> {
        // Broad local case-insensitive text check
        val terms = query.lowercase().split(" ").filter { it.length > 1 }
        if (terms.isEmpty()) {
            return files.filter { it.name.contains(query, ignoreCase = true) }.map { it.path }
        }

        return files.filter { file ->
            val nameLower = file.name.lowercase()
            val tagsLower = file.tags.lowercase()
            val descLower = (file.aiDescription ?: "").lowercase()
            val catLower = file.category.lowercase()
            terms.any { term ->
                nameLower.contains(term) || tagsLower.contains(term) || descLower.contains(term) || catLower.contains(term)
            }
        }.sortedByDescending { file ->
            // Scoring heuristic
            var score = 0
            val nameLower = file.name.lowercase()
            terms.forEach { term ->
                if (nameLower.startsWith(term)) score += 10
                else if (nameLower.contains(term)) score += 5
                if (file.tags.lowercase().contains(term)) score += 3
                if ((file.aiDescription ?: "").lowercase().contains(term)) score += 2
            }
            score
        }.map { it.path }
    }
}
