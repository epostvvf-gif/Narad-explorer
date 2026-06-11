package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.ScannedFile
import com.example.ui.theme.*
import com.example.ui.viewmodel.NaradViewModel
import com.example.ui.viewmodel.ScanUiState
import com.example.ui.viewmodel.StorageSpaceInfo
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: NaradViewModel, modifier: Modifier = Modifier) {
    var currentTab by remember { mutableStateOf(0) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var fileDetailTarget by remember { mutableStateOf<ScannedFile?>(null) }

    val scanState by viewModel.scanState.collectAsStateWithLifecycle()
    val allFiles by viewModel.allFiles.collectAsStateWithLifecycle()
    val storageSpace by viewModel.storageSpace.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.narad_logo),
                            contentDescription = "Narad Explorer Logo",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Column {
                            Text(
                                text = "नारद एक्सप्लोरर",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "AI फ़ाइल मैनेजर",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (selectedCategory != null) {
                        IconButton(onClick = { selectedCategory = null }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.triggerScan(true, true, true) },
                        modifier = Modifier.testTag("action_scan_refresh")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh storage index",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            if (selectedCategory == null) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    val tabs = listOf(
                        Triple("साफ़ करें", Icons.Default.CleaningServices, 0),
                        Triple("ब्राउज़ करें", Icons.Default.Folder, 1),
                        Triple("एआई खोज", Icons.Default.AutoAwesome, 2),
                        Triple("चिह्नित/टैग", Icons.Default.Favorite, 3)
                    )
                    tabs.forEach { (title, icon, index) ->
                        NavigationBarItem(
                            selected = currentTab == index,
                            onClick = {
                                currentTab = index
                                // Reset search state on tab switch
                                if (index != 2) {
                                    viewModel.updateSearchQuery("")
                                }
                            },
                            icon = { Icon(imageVector = icon, contentDescription = title) },
                            label = { Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            ),
                            modifier = Modifier.testTag("nav_tab_$index")
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Processing dialog if scanning is active
            if (scanState is ScanUiState.Scanning) {
                val sc = scanState as ScanUiState.Scanning
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f))
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .padding(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                            Text(
                                text = "स्टोरेज अनुक्रमण (Scanning)...",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = sc.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "अनुक्रमित फाइलें: ${sc.progress}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            AnimatedContent(
                targetState = if (selectedCategory != null) -1 else currentTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                },
                label = "TabTransition"
            ) { targetIndex ->
                when (targetIndex) {
                    -1 -> {
                        // Category browser screen
                        CategoryBrowserSection(
                            category = selectedCategory!!,
                            viewModel = viewModel,
                            onFileClick = { fileDetailTarget = it }
                        )
                    }
                    0 -> {
                        // Clean Up tab
                        CleanUpSection(
                            storageSpace = storageSpace,
                            scanState = scanState,
                            onCleanClick = { viewModel.cleanSelectedJunk() },
                            onTriggerScan = { viewModel.triggerScan(true, true, true) }
                        )
                    }
                    1 -> {
                        // Browse tab
                        BrowseSection(
                            allFilesCount = allFiles.size,
                            viewModel = viewModel,
                            onCategoryClick = { selectedCategory = it },
                            onFileClick = { fileDetailTarget = it }
                        )
                    }
                    2 -> {
                        // Semantic AI Search tab
                        AiSearchSection(
                            viewModel = viewModel,
                            onFileClick = { fileDetailTarget = it }
                        )
                    }
                    3 -> {
                        // Starred & Tags tab
                        StarredAndTagsSection(
                            viewModel = viewModel,
                            onFileClick = { fileDetailTarget = it }
                        )
                    }
                }
            }

            // File Details Bottom Sheet
            if (fileDetailTarget != null) {
                FileDetailsModal(
                    file = fileDetailTarget!!,
                    viewModel = viewModel,
                    onDismiss = { fileDetailTarget = null }
                )
            }
        }
    }
}

// 1. CLEAN UP TAB (Google Files style junk cleaner)
@Composable
fun CleanUpSection(
    storageSpace: StorageSpaceInfo,
    scanState: ScanUiState,
    onCleanClick: () -> Unit,
    onTriggerScan: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Space distribution card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("clean_storage_card")
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "स्टोरेज उपयोग (Storage Utilization)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    val usedPercent = (storageSpace.usedSize.toFloat() / storageSpace.totalSize.toFloat() * 100).coerceAtLeast(1f)
                    val formattedUsedPercent = String.format(Locale.ROOT, "%.1f", usedPercent)

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${formatFileSize(storageSpace.usedSize)} भरा हुआ",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "कुल ${formatFileSize(storageSpace.totalSize)} में से",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Progress Bar
                    LinearProgressIndicator(
                        progress = { usedPercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "आपका $formattedUsedPercent% स्टोरेज उपयोग में है।",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Clean Junk action cards
        item {
            Text(
                text = "क्लीनिंग सुझाव (Cleaning Suggestions)",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (storageSpace.junkSize > 0L) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                        .testTag("junk_clear_card")
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Junk Icon",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Column {
                                Text(
                                    text = "जंक फ़ाइलें साफ करें",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "डुप्लिकेट, अस्थायी (.tmp/.log) और अप्रचलित एपीके फ़ाइलें शामिल हैं।",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "मुफ़्त होने वाली जगह",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = formatFileSize(storageSpace.junkSize),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Button(
                                onClick = onCleanClick,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("clean_junk_button")
                            ) {
                                Text("अभी साफ़ करें", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DoneOutline,
                            contentDescription = "No junk",
                            tint = NaradEmerald,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "आपका फोन बिल्कुल साफ है!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "कोई फालतू जंक या निरर्थक फ़ाइलें नहीं पाई गईं।",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Details breakdown details
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "फ़ाइल ब्रेकडाउन विश्लेषण (Breakdown Archive)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    BreakdownRow(
                        title = "डुप्लिकेट कॉपी फ़ाइलें (Same files)",
                        count = "${storageSpace.duplicateCount} सेट",
                        icon = Icons.Default.CopyAll,
                        iconColor = MaterialTheme.colorScheme.secondary
                    )
                    Divider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    BreakdownRow(
                        title = "15MB से बड़ी भारी फ़ाइलें (Large Archive)",
                        count = "${storageSpace.largeFilesCount} फ़ाइलें",
                        icon = Icons.Default.SdCard,
                        iconColor = NaradFlameOrange
                    )
                    Divider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    BreakdownRow(
                        title = "अप्रचलित एंड्रॉइड एपीके (Unused APks)",
                        count = "${storageSpace.apkCount} एपीके",
                        icon = Icons.Default.Android,
                        iconColor = NaradEmerald
                    )
                    Divider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    BreakdownRow(
                        title = "अस्थायी कचरा फ़ाइलें (.tmp, .log, .cache)",
                        count = "${storageSpace.tempFilesCount} फ़ाइलें",
                        icon = Icons.Default.SyncProblem,
                        iconColor = NaradCosmicPurple
                    )
                }
            }
        }
    }
}

@Composable
fun BreakdownRow(
    title: String,
    count: String,
    icon: ImageVector,
    iconColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = title,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            text = count,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

// 2. BROWSE TAB (Google Files style grid)
@Composable
fun BrowseSection(
    allFilesCount: Int,
    viewModel: NaradViewModel,
    onCategoryClick: (String) -> Unit,
    onFileClick: (ScannedFile) -> Unit
) {
    val allFiles by viewModel.allFiles.collectAsStateWithLifecycle()
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Welcome and Status Title
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "फ़ाइलें ब्राउज़ करें",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "आपके सभी स्टोरेज (फ़ोन, एसडी कार्ड और ड्राइव) व्यवस्थित हैं।",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Action Trigger info
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "scanned info",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "कुल अनुक्रमित फाइलें (Capacity Indexed): 50,000 लिमिट सक्रिय। वर्तमान: $allFilesCount फ़ाइलें",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Category grid
        item {
            Text(
                text = "श्रेणियाँ (Categories)",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        item {
            val categories = listOf(
                CategoryItem("छवि (Images)", "images", Icons.Default.Image, NaradOceanBlue),
                CategoryItem("वीडियो (Videos)", "videos", Icons.Default.VideoLibrary, NaradFlameOrange),
                CategoryItem("ऑडियो (Audio)", "audio", Icons.Default.Audiotrack, NaradCosmicPurple),
                CategoryItem("दस्तावेज़ (Docs)", "documents", Icons.Default.Description, NaradGold),
                CategoryItem("ऐप्स (Apps/APKs)", "apps", Icons.Default.Android, NaradEmerald),
                CategoryItem("डाउनलोड (Downloads)", "downloads", Icons.Default.Download, Color(0xFF607D8B))
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                items(categories) { cat ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { onCategoryClick(cat.id) }
                            .testTag("id_category_${cat.id}")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(cat.color.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = cat.icon,
                                    contentDescription = cat.title,
                                    tint = cat.color,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = cat.title,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Recent Files / Scans List
        item {
            Text(
                text = "हाल ही में जोड़ी गई फ़ाइलें (Recent Scans)",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        if (allFiles.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "कोई फ़ाइल अनुक्रमित नहीं है। रिफ्रेश बटन दबाएँ।",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(allFiles.take(15)) { file ->
                FileItemRow(file = file, onItemClick = onFileClick, onStarClick = { viewModel.toggleStarred(file) })
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

data class CategoryItem(
    val title: String,
    val id: String,
    val icon: ImageVector,
    val color: Color
)

// File row styling
@Composable
fun FileItemRow(
    file: ScannedFile,
    onItemClick: (ScannedFile) -> Unit,
    onStarClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick(file) }
            .testTag("file_row_${file.name}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Category Specific Vector Background
                val iconColor = when (file.category) {
                    "images" -> NaradOceanBlue
                    "videos" -> NaradFlameOrange
                    "audio" -> NaradCosmicPurple
                    "documents" -> NaradGold
                    "apps" -> NaradEmerald
                    else -> Color(0xFF607D8B)
                }

                val icon = when (file.category) {
                    "images" -> Icons.Default.Image
                    "videos" -> Icons.Default.VideoLibrary
                    "audio" -> Icons.Default.Audiotrack
                    "documents" -> Icons.Default.Description
                    "apps" -> Icons.Default.Android
                    else -> Icons.Default.Download
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = file.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Storage badge
                        Text(
                            text = file.storageType,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (file.storageType) {
                                "DRIVE" -> NaradSkyBlue
                                "SDCARD" -> NaradGold
                                else -> NaradEmerald
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    when (file.storageType) {
                                        "DRIVE" -> NaradSkyBlue.copy(alpha = 0.15f)
                                        "SDCARD" -> NaradGold.copy(alpha = 0.15f)
                                        else -> NaradEmerald.copy(alpha = 0.15f)
                                    }
                                )
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                        Text(
                            text = formatFileSize(file.size),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Row {
                IconButton(onClick = onStarClick) {
                    Icon(
                        imageVector = if (file.isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Star file",
                        tint = if (file.isStarred) NaradGold else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

// 3. SEMANTIC AI SEARCH TAB
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiSearchSection(
    viewModel: NaradViewModel,
    onFileClick: (ScannedFile) -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isSemanticSearch by viewModel.isSemanticSearch.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = "एआई सेमेंटिक खोज (AI Search Engine)",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "प्राकृतिक हिंदी आवाज या शब्दों में फाइलें ढूंढें (जैसे: 'कार का बीमा')",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Search card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    placeholder = { Text("खोजने के लिए शब्द लिखें...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_search_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    singleLine = true
                )

                // Search mode selectors
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Switch(
                            checked = isSemanticSearch,
                            onCheckedChange = { viewModel.setSemanticSearchMode(it) },
                            modifier = Modifier.testTag("switch_semantic")
                        )
                        Column {
                            Text(
                                text = "एआई सेमेंटिक सक्रिय (Gemini AI)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSemanticSearch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "संदर्भ और अर्थ के आधार पर मिलान",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = { viewModel.triggerSearch() },
                        enabled = searchQuery.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("run_search_button")
                    ) {
                        Text("सर्च करें", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Suggestions when search is empty
        if (searchQuery.isEmpty()) {
            Text(
                text = "ये खोजकर देखें (Sample Prompts):",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.secondary
            )
            val suggestions = listOf(
                "टैक्स और आयकर दस्तावेज" to "tax",
                "शिमले की पारिवारिक तस्वीरें" to "shimla",
                "अरिजीत के गन मन की ध्वनि" to "arijit",
                "महत्वपूर्ण सरकारी रसीद / बिल" to "receipt",
                "अस्थायी कैश फ़ाइलें" to "tmp"
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                suggestions.forEach { (text, query) ->
                    SuggestionChip(
                        onClick = {
                            viewModel.updateSearchQuery(query)
                            viewModel.setSemanticSearchMode(true)
                        },
                        label = { Text(text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        } else {
            // Search Results Title with details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "तलाश परिणाम (${searchResults.size} फ़ाइलें)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                if (isSearching) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                }
            }

            if (searchResults.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "कोई सम्बद्ध फ़ाइल नहीं मिली।",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "कृपया अपनी खोज बदलें या शब्द बदलें।",
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
                    items(searchResults) { file ->
                        FileItemRow(file = file, onItemClick = onFileClick, onStarClick = { viewModel.toggleStarred(file) })
                    }
                }
            }
        }
    }
}

// 4. STARRED & TAGS MANAGEMENT TAB
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StarredAndTagsSection(
    viewModel: NaradViewModel,
    onFileClick: (ScannedFile) -> Unit
) {
    val starredFiles by viewModel.starredFiles.collectAsStateWithLifecycle()
    val tagsList by viewModel.tagsList.collectAsStateWithLifecycle()

    val allFiles by viewModel.allFiles.collectAsStateWithLifecycle()

    var activeTagFilter by remember { mutableStateOf<String?>(null) }
    val filteredFilesList = remember(activeTagFilter, allFiles) {
        val tag = activeTagFilter
        if (tag == null) {
            allFiles
        } else {
            allFiles.filter { it.tags.split(",").map { t -> t.trim() }.contains(tag) }
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Column {
                Text(
                    text = "चिह्नित और फ़ाइल टैग",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "फाइलों को कैटेगरी टैग से सहेजना या स्टार चिह्नित लगाना।",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Tags Category filtration List
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "टैग फ़िल्टर (Tag Filtering)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // All filter
                    FilterChip(
                        selected = activeTagFilter == null,
                        onClick = { activeTagFilter = null },
                        label = { Text("सभी फ़ाइलें", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.testTag("tag_filter_all")
                    )

                    tagsList.forEach { tag ->
                        FilterChip(
                            selected = activeTagFilter == tag.name,
                            onClick = { activeTagFilter = tag.name },
                            label = { Text(tag.name, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(tag.colorHex)))
                                )
                            },
                            modifier = Modifier.testTag("tag_filter_${tag.name}")
                        )
                    }
                }
            }
        }

        // Starred files title
        if (activeTagFilter == null) {
            item {
                Text(
                    text = "स्टार चिह्नित फ़ाइलें (${starredFiles.size})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            if (starredFiles.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.StarOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "कोई स्टार चिह्नित फाइल नहीं है।",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                items(starredFiles) { file ->
                    FileItemRow(file = file, onItemClick = onFileClick, onStarClick = { viewModel.toggleStarred(file) })
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // Files having selected Tag filter
        item {
            Text(
                text = if (activeTagFilter == null) "सभी फ़ाइलें संग्रह (${filteredFilesList.size})" else "टैग '${activeTagFilter}' वाली फ़ाइलें (${filteredFilesList.size})",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        if (filteredFilesList.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("कोई संबद्ध फ़ाइल नहीं पाई गई।", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(filteredFilesList) { file ->
                FileItemRow(file = file, onItemClick = onFileClick, onStarClick = { viewModel.toggleStarred(file) })
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

// 5. INNER CATEGORY BROWSER SCREEN
@Composable
fun CategoryBrowserSection(
    category: String,
    viewModel: NaradViewModel,
    onFileClick: (ScannedFile) -> Unit
) {
    val categoryFiles by when (category) {
        "images" -> viewModel.imagesList
        "videos" -> viewModel.videosList
        "audio" -> viewModel.audioList
        "documents" -> viewModel.docsList
        "apps" -> viewModel.appsList
        else -> viewModel.downloadsList
    }.collectAsStateWithLifecycle()

    val catHindiName = when (category) {
        "images" -> "छवियाँ"
        "videos" -> "वीडियो"
        "audio" -> "ऑडियो"
        "documents" -> "दस्तावेज़"
        "apps" -> "एप्लिकेशन"
        else -> "डाउनलोड फ़ाइलें"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column {
            Text(
                text = catHindiName,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "कुल पाई गईं फ़ाइलें: ${categoryFiles.size}",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (categoryFiles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "इस श्रेणी में कोई फ़ाइल अनुक्रमित नहीं है।",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(categoryFiles) { file ->
                    FileItemRow(
                        file = file,
                        onItemClick = onFileClick,
                        onStarClick = { viewModel.toggleStarred(file) }
                    )
                }
            }
        }
    }
}

// 6. FILE DETAILS BOTTOM SHEET DIALOG
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FileDetailsModal(
    file: ScannedFile,
    viewModel: NaradViewModel,
    onDismiss: () -> Unit
) {
    val suggestedTags by viewModel.suggestedTags.collectAsStateWithLifecycle()
    val isLoadingSuggestions by viewModel.isLoadingSuggestions.collectAsStateWithLifecycle()

    var newTagInput by remember { mutableStateOf("") }

    // Fetch AI Suggestions on display
    LaunchedEffect(file) {
        viewModel.fetchAiTagSuggestions(file)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("file_details_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "फ़ाइल विवरण",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            // File Attributes Display
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AttributeRow(label = "फ़ाइल का नाम (Name):", value = file.name, valueColor = MaterialTheme.colorScheme.onSurface)
                    AttributeRow(label = "फ़ाइल पथ (Path):", value = file.path, valueColor = MaterialTheme.colorScheme.onSurfaceVariant)
                    AttributeRow(label = "आकार (Size):", value = formatFileSize(file.size), valueColor = MaterialTheme.colorScheme.primary)
                    AttributeRow(label = "स्टोरेज प्रकार (Source):", value = file.storageType, valueColor = NaradGold)
                    AttributeRow(label = "संशोधित समय (Modified):", value = formatDate(file.lastModified), valueColor = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (file.aiDescription != null) {
                        AttributeRow(label = "एआई संदर्भ विवरण:", value = file.aiDescription, valueColor = MaterialTheme.colorScheme.secondary)
                    }
                }
            }

            // Tags Edit Block
            Text(
                text = "फ़ाइल टैग्स (Tags)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )

            // Current Tags chips
            val currentTagsList = remember(file.tags) {
                file.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            }

            if (currentTagsList.isEmpty()) {
                Text(
                    text = "इस फ़ाइल में अभी कोई टैग नहीं लगा है।",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    currentTagsList.forEach { tg ->
                        InputChip(
                            selected = true,
                            onClick = {},
                            label = { Text(tg, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Cancel,
                                    contentDescription = "Remove tag",
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { viewModel.removeTagFromFile(file, tg) }
                                )
                            }
                        )
                    }
                }
            }

            // AI suggested tags recomendation widget
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "एआई सुझावित टैग (AI Suggested SmartTags):",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (isLoadingSuggestions) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp))
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        suggestedTags.forEach { suggest ->
                            if (!currentTagsList.contains(suggest)) {
                                SuggestionChip(
                                    onClick = { viewModel.addTagToFile(file, suggest) },
                                    label = { Text(suggest, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = NaradFlameOrange.copy(alpha = 0.12f)
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Quick add custom tag field
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newTagInput,
                    onValueChange = { newTagInput = it },
                    placeholder = { Text("नया टैग लिखें...", fontSize = 12.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("tag_input_field"),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
                Button(
                    onClick = {
                        if (newTagInput.trim().isNotEmpty()) {
                            viewModel.addTagToFile(file, newTagInput.trim())
                            newTagInput = ""
                        }
                    },
                    modifier = Modifier.height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Custom Tag")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Lower Danger Operation: Delete File
            Button(
                onClick = {
                    viewModel.deleteFile(file)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("delete_file_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("फ़ाइल स्थायी रूप से हटाएं", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AttributeRow(label: String, value: String, valueColor: Color) {
    Column {
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = valueColor, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

// Global utilities formatted values helper
fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format(Locale.ROOT, "%.2f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ROOT)
    return sdf.format(Date(timestamp))
}
