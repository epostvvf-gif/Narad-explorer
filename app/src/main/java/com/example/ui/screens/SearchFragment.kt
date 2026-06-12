package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.GeminiMetadataProcessor
import com.example.data.ScannedFile
import com.example.ui.viewmodel.NaradViewModel
import kotlinx.coroutines.launch

/**
 * A beautiful dedicated semantic search component styled as 'SearchFragment' (following Compose MVVM design).
 * It includes an elegant search bar to trigger deep semantic queries via GeminiMetadataProcessor,
 * rendering high-contrast lists of matching files and their absolute/cloud paths.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SearchFragment(
    viewModel: NaradViewModel,
    modifier: Modifier = Modifier,
    onFileClick: (ScannedFile) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val allFiles by viewModel.allFiles.collectAsStateWithLifecycle()
    
    // Search states
    var queryText by remember { mutableStateOf("") }
    var isSearchingSemantic by remember { mutableStateOf(false) }
    var matchedPaths by remember { mutableStateOf<List<String>>(emptyList()) }
    var searchExecuted by remember { mutableStateOf(false) }

    val geminiProcessor = remember { GeminiMetadataProcessor() }

    // Derive matched file objects from paths
    val matchedFiles = remember(matchedPaths, allFiles) {
        matchedPaths.mapNotNull { path ->
            allFiles.find { it.path == path }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Heading / Instructions
        Column {
            Text(
                text = "एआई नया सेमेंटिक सर्च (SearchFragment)",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("search_fragment_title")
            )
            Text(
                text = "Find files by conceptual meaning, context, or synonyms using Gemini AI",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Search Bar container
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = queryText,
                    onValueChange = {
                        queryText = it
                        if (it.isEmpty()) {
                            matchedPaths = emptyList()
                            searchExecuted = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_fragment_input"),
                    placeholder = { Text("खोजें (e.g., 'office work', 'photos of beach')", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = {
                        if (queryText.isNotEmpty()) {
                            IconButton(onClick = { 
                                queryText = "" 
                                matchedPaths = emptyList()
                                searchExecuted = false
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search query")
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                // Trigger Button
                Button(
                    onClick = {
                        if (queryText.isNotBlank()) {
                            coroutineScope.launch {
                                isSearchingSemantic = true
                                matchedPaths = geminiProcessor.performSemanticSearch(queryText, allFiles)
                                searchExecuted = true
                                isSearchingSemantic = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("trigger_semantic_search_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp),
                    enabled = queryText.isNotBlank() && !isSearchingSemantic
                ) {
                    if (isSearchingSemantic) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("जेमिनी सेमेंटिक सर्च (Search with Gemini)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Sample Contextual Tags Suggestions when search is active or blank
        if (!searchExecuted && !isSearchingSemantic) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "त्वरित संदर्भ खोज (Tap to Try):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                val sampleQueries = listOf(
                    "Work receipts and tax bills" to "important receipts of taxes",
                    "Offline download archives" to "downloads",
                    "Media sounds or music logs" to "music and audio recordings",
                    "Backup storage indexes" to "DRIVE"
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sampleQueries.forEach { (label, q) ->
                        SuggestionChip(
                            onClick = {
                                queryText = q
                                coroutineScope.launch {
                                    isSearchingSemantic = true
                                    matchedPaths = geminiProcessor.performSemanticSearch(q, allFiles)
                                    searchExecuted = true
                                    isSearchingSemantic = false
                                }
                            },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                }
            }
        }

        // Search Result Title / Count
        if (searchExecuted || isSearchingSemantic) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isSearchingSemantic) "सर्च जारी है..." else "संबद्ध फ़ाइल पथ (${matchedFiles.size} Matches)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                if (isSearchingSemantic) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .width(80.dp)
                            .height(3.dp)
                    )
                }
            }
        }

        // Search Output List rendering matched files with absolute paths
        if (searchExecuted && !isSearchingSemantic) {
            if (matchedFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FindInPage,
                            contentDescription = "Not found",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "कोई सम्बद्ध फ़ाइल पथ नहीं मिला।",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Please broaden your prompt description or check indexing.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(matchedFiles) { file ->
                        PathResultRow(
                            file = file,
                            onClick = { onFileClick(file) }
                        )
                    }
                }
            }
        } else {
            // Placeholder when no search has been run
            if (!isSearchingSemantic) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.SavedSearch,
                            contentDescription = "Enter search",
                            tint = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "एक सेमेंटिक खोज शुरू करें",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "Enter sentences describing what you need under Narad index.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Optimized list card item specifically emphasizing the full file path.
 */
@Composable
fun PathResultRow(
    file: ScannedFile,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("path_result_${file.name}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val (icon, color) = when (file.category) {
                    "images" -> Icons.Default.Image to Color(0xFF4CAF50)
                    "videos" -> Icons.Default.PlayCircle to Color(0xFFFF5722)
                    "audio" -> Icons.Default.MusicNote to Color(0xFF00BCD4)
                    "documents" -> Icons.Default.Description to Color(0xFF2196F3)
                    "apps" -> Icons.Default.Android to Color(0xFF9C27B0)
                    else -> Icons.Default.FolderOpen to Color(0xFFFFC107)
                }

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )

                Text(
                    text = file.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = file.storageType,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (file.storageType == "DRIVE") Color(0xFF4CAF50) else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .background(
                            color = (if (file.storageType == "DRIVE") Color(0xFF4CAF50) else MaterialTheme.colorScheme.secondary).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Emphasize the matching file path in a code structure
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(11.dp)
                )
                Text(
                    text = file.path,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    modifier = Modifier.size(11.dp)
                )
            }

            if (!file.aiDescription.isNullOrBlank()) {
                Text(
                    text = "Context: ${file.aiDescription}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
