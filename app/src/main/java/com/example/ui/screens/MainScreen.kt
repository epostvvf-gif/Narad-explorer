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
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
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

    val googleUser by viewModel.googleUser.collectAsStateWithLifecycle()
    val isDriveConnected by viewModel.isDriveConnected.collectAsStateWithLifecycle()
    var showGoogleSignInConsent by remember { mutableStateOf(false) }

    // Display Google Consent Dialogue Box
    if (showGoogleSignInConsent) {
        GoogleSignInConsentDialog(
            userEmail = "epostvvf@gmail.com",
            onDismiss = { showGoogleSignInConsent = false },
            onConfirm = { email: String, name: String ->
                viewModel.signInWithGoogle(email, name)
            }
        )
    }

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
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "AI फ़ाइल मैनेजर",
                                fontSize = 10.sp,
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
                    // Google Profile Quick Indicator
                    if (googleUser != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                .clickable { viewModel.signOutGoogle() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = googleUser!!.displayName.take(1),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            Text(
                                text = "लॉग-आउट",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        Button(
                            onClick = { showGoogleSignInConsent = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier
                                .height(28.dp)
                                .padding(end = 6.dp)
                                .testTag("btn_top_sign_in")
                        ) {
                            Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("साइन-इन", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    IconButton(
                        onClick = { viewModel.triggerScan(true, true, isDriveConnected) },
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
                            viewModel = viewModel,
                            storageSpace = storageSpace,
                            scanState = scanState,
                            onCleanClick = { viewModel.cleanSelectedJunk() },
                            onTriggerScan = { viewModel.triggerScan(true, true, isDriveConnected) }
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
                        // Semantic AI Search fragment/screen running on-demand Gemini client processor
                        SearchFragment(
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
    viewModel: NaradViewModel,
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
        // Storage Permission Banner
        item {
            ScopedStoragePermissionBanner()
        }

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

        // AI Auto-Tagging engine widget Card
        item {
            val isAutoTaggingActive by viewModel.isAutoTaggingActive.collectAsStateWithLifecycle()
            val autoTagStatusText by viewModel.autoTagStatusText.collectAsStateWithLifecycle()
            val autoTagSuccessCount by viewModel.autoTagSuccessCount.collectAsStateWithLifecycle()

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    .testTag("ai_auto_tag_card")
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "नारद एआई ऑटो-टैगिंग इंजन",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Gemini AI Automation Tagging Engine",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "जेमिनी एआई की शक्ति का उपयोग करके अपनी सभी फाइलों का स्वचालित रूप से नाम के आधार पर विश्लेषण करें और 'Work', 'Personal', 'Receipts', या 'Finance' जैसे प्रोफेशनल टैग असाइन करें ताकि आपका कैटलॉग पूरी तरह से आर्गेनाइज्ड रहे।",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 15.sp
                    )
                    
                    if (isAutoTaggingActive) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = autoTagStatusText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "वर्गीकृत: $autoTagSuccessCount फ़ाइलें",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    } else if (autoTagStatusText.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = autoTagStatusText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NaradEmerald
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.runBatchAutoTagging() },
                        enabled = !isAutoTaggingActive,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("run_auto_tagging_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isAutoTaggingActive) "स्वचालित टैगिंग जारी है..." else "पूरी स्टोरेज एआई ऑटो-टैग करें",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
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
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun BrowseSection(
    allFilesCount: Int,
    viewModel: NaradViewModel,
    onCategoryClick: (String) -> Unit,
    onFileClick: (ScannedFile) -> Unit
) {
    val allFiles by viewModel.allFiles.collectAsStateWithLifecycle()
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()
    val googleUser by viewModel.googleUser.collectAsStateWithLifecycle()
    val isDriveConnected by viewModel.isDriveConnected.collectAsStateWithLifecycle()

    var showSignInLocalDialog by remember { mutableStateOf(false) }
    var activeDashboardTagFilter by remember { mutableStateOf<String?>(null) }
    var selectedExtensionGroup by remember { mutableStateOf("All") }

    val docxCount = remember(allFiles) {
        allFiles.count { it.tags.split(",").any { t -> t.trim().equals("docx", ignoreCase = true) } || it.name.endsWith(".docx", ignoreCase = true) }
    }
    val bookCount = remember(allFiles) {
        allFiles.count { it.tags.split(",").any { t -> t.trim().equals("book", ignoreCase = true) } || it.name.contains("book", ignoreCase = true) || it.name.contains("ebook", ignoreCase = true) }
    }
    val personalCount = remember(allFiles) {
        allFiles.count { it.tags.split(",").any { t -> t.trim().equals("personal", ignoreCase = true) } }
    }
    val lettersCount = remember(allFiles) {
        allFiles.count { it.tags.split(",").any { t -> t.trim().equals("letters", ignoreCase = true) } || it.name.contains("letter", ignoreCase = true) || it.name.contains("agreement", ignoreCase = true) || it.name.contains("slip", ignoreCase = true) }
    }

    val displayedFiles = remember(activeDashboardTagFilter, allFiles) {
        val f = activeDashboardTagFilter
        if (f == null) {
            allFiles
        } else {
            allFiles.filter { file ->
                file.tags.split(",").any { t -> t.trim().equals(f, ignoreCase = true) } ||
                (f == "docx" && file.name.endsWith(".docx", ignoreCase = true)) ||
                (f == "book" && (file.name.contains("book", ignoreCase = true) || file.name.contains("ebook", ignoreCase = true))) ||
                (f == "letters" && (file.name.contains("letter", ignoreCase = true) || file.name.contains("agreement", ignoreCase = true) || file.name.contains("slip", ignoreCase = true)))
            }
        }
    }

    val groupedFiles = remember(selectedExtensionGroup, displayedFiles) {
        if (selectedExtensionGroup == "All") {
            displayedFiles
        } else {
            displayedFiles.filter { file ->
                com.example.data.FileHelperUtils.categorizeByExtension(file.name) == selectedExtensionGroup
            }
        }
    }

    if (showSignInLocalDialog) {
        GoogleSignInConsentDialog(
            userEmail = "epostvvf@gmail.com",
            onDismiss = { showSignInLocalDialog = false },
            onConfirm = { email: String, name: String ->
                viewModel.signInWithGoogle(email, name)
            }
        )
    }

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

        // Storage Permission Banner
        item {
            ScopedStoragePermissionBanner()
        }

        // Multi-Storage devices cards (PHONE, SDCARD, DRIVE)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                     text = "स्टोरेज यूनिट्स (Storage Units)",
                     fontSize = 14.sp,
                     fontWeight = FontWeight.Bold,
                     color = MaterialTheme.colorScheme.secondary
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. Phone internal
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("आंतरिक संग्रहण", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("32 GB | सक्रिय", fontSize = 9.sp, color = NaradEmerald, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // 2. SD Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Icon(Icons.Default.SdCard, contentDescription = null, tint = NaradFlameOrange, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("एसडी कार्ड", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("16 GB | सक्रिय", fontSize = 9.sp, color = NaradEmerald, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // 3. Google Drive
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDriveConnected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .clickable {
                                if (isDriveConnected) {
                                    viewModel.signOutGoogle()
                                } else {
                                    showSignInLocalDialog = true
                                }
                            }
                            .border(
                                width = 1.5.dp,
                                color = if (isDriveConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .testTag("google_drive_storage_card")
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudQueue,
                                    contentDescription = null,
                                    tint = if (isDriveConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(20.dp)
                                )
                                if (!isDriveConnected) {
                                    Icon(Icons.Default.Lock, contentDescription = "Locked", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(12.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Google Drive", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                text = if (isDriveConnected) "कनेक्टेड" else "थपथपाएं और लॉगिन करें",
                                fontSize = 9.sp,
                                color = if (isDriveConnected) NaradEmerald else MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
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

        // AI-Suggested categories Dashboard Component
        item {
            AiSmartCategoriesDashboard(
                docxCount = docxCount,
                bookCount = bookCount,
                personalCount = personalCount,
                lettersCount = lettersCount,
                activeFilter = activeDashboardTagFilter,
                onFilterClick = { activeDashboardTagFilter = it }
            )
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
            Column {
                Text(
                    text = if (activeDashboardTagFilter != null) {
                        "श्रेणी फ़िल्टर: '${activeDashboardTagFilter}' (${groupedFiles.size} फ़ाइलें)"
                    } else {
                        "हाल ही में जोड़ी गई फ़ाइलें (Recent Scans)"
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                
                // Grouping Filter Row (Documents, Media, Archives)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    val groups = listOf(
                        Triple("All", "सभी", Icons.Default.AllInclusive),
                        Triple("Documents", "दस्तावेज़", Icons.Default.Description),
                        Triple("Media", "मीडिया", Icons.Default.PlayCircle),
                        Triple("Archives", "अभिलेख (Zip)", Icons.Default.Inventory)
                    )
                    
                    groups.forEach { (groupId, label, icon) ->
                        val isSelected = selectedExtensionGroup == groupId
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedExtensionGroup = groupId },
                            label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("extension_filter_${groupId.lowercase()}")
                        )
                    }
                }
            }
        }

        if (groupedFiles.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (selectedExtensionGroup != "All") {
                            "इस श्रेणी ('$selectedExtensionGroup') में कोई फ़ाइल नहीं मिली।"
                        } else if (activeDashboardTagFilter != null) {
                            "इस श्रेणी से जुड़ी कोई फ़ाइल अनुक्रमित नहीं मिली।"
                        } else {
                            "कोई फ़ाइल अनुक्रमित नहीं है। रिफ्रेश बटन दबाएँ।"
                        },
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(groupedFiles.take(15)) { file ->
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

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun AiSmartCategoriesDashboard(
    docxCount: Int,
    bookCount: Int,
    personalCount: Int,
    lettersCount: Int,
    activeFilter: String?,
    onFilterClick: (String?) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            .testTag("ai_categories_dashboard_card")
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            // Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = "एआई अनुशंसित श्रेणियां",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "AI-Suggested Categories & Live Counter Dashboard",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(14.dp))
            
            // Chips row / flow row
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val chips = listOf(
                    DashboardChipItem("docx", "docx", docxCount, Icons.Default.Description, Color(0xFF1E88E5)),
                    DashboardChipItem("book", "book", bookCount, Icons.Default.Book, Color(0xFF8E24AA)),
                    DashboardChipItem("Personal", "Personal", personalCount, Icons.Default.Person, Color(0xFF43A047)),
                    DashboardChipItem("letters", "letters", lettersCount, Icons.Default.Mail, Color(0xFFD81B60))
                )
                
                chips.forEach { chip ->
                    val isSelected = activeFilter == chip.id
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) chip.color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                            .clickable {
                                if (isSelected) {
                                    onFilterClick(null)
                                } else {
                                    onFilterClick(chip.id)
                                }
                            }
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) chip.color else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .testTag("ai_dashboard_chip_${chip.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = chip.icon,
                                contentDescription = chip.label,
                                tint = chip.color,
                                modifier = Modifier.size(16.dp)
                            )
                            Column(verticalArrangement = Arrangement.Center) {
                                Text(
                                    text = chip.label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${chip.count} फ़ाइलें",
                                    fontSize = 8.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = chip.color,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            if (activeFilter != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onFilterClick(null) }
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "फ़िल्टर सक्रिय है: '${activeFilter}'। सभी हालिया फाइलें देखने के लिए यहाँ क्लिक करें।",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear Filter",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

data class DashboardChipItem(
    val id: String,
    val label: String,
    val count: Int,
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
    val isDescribingFile by viewModel.isDescribingFile.collectAsStateWithLifecycle()

    var newTagInput by remember { mutableStateOf("") }
    var currentDescription by remember(file.path) { mutableStateOf(file.aiDescription) }

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
                    if (currentDescription != null) {
                        AttributeRow(label = "एआई संदर्भ विवरण:", value = currentDescription!!, valueColor = MaterialTheme.colorScheme.secondary)
                    }
                }
            }

            // On-demand Metadata Processing Trigger
            Button(
                onClick = {
                    viewModel.generateAiDescriptionForFileOnDemand(file) { newDesc ->
                        currentDescription = newDesc
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("generate_description_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                shape = RoundedCornerShape(10.dp),
                enabled = !isDescribingFile
            ) {
                if (isDescribingFile) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onSecondary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "AI Analyze", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "एआई विवरण विश्लेषक (Analyze file with Gemini)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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

@Composable
fun GoogleSignInConsentDialog(
    userEmail: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Text("Google एकाउंट साइन-इन", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "नारद एक्सप्लोरर आपके Google Drive स्टोरेज का सुरक्षित अनुक्रमण (Indexing) करने के लिए ऑथेंटिकेशन की सहमति चाहता है।",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = userEmail.take(1).uppercase(Locale.ROOT),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column {
                            Text("Narad User", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text(userEmail, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                
                Text(
                    text = "सुरक्षा विकल्प: आपके क्लाउड दस्तावेज़ और प्रविष्टियां पूर्ण गोपनीयता के साथ स्थानीय स्तर पर ही सुरक्षित रहेंगी।",
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(userEmail, "Narad User")
                    onDismiss()
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("साइन-इन स्वीकारें", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("रद्द करें", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("google_login_dialog")
    )
}

@Composable
fun ScopedStoragePermissionBanner(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(false) }

    fun checkPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            val readGranted = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            readGranted
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = checkPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        hasPermission = checkPermission()
    }

    if (!hasPermission) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(14.dp))
                .testTag("scoped_storage_permission_banner")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "स्टोरेज एक्सेस अनुमति आवश्यक (Storage Access Required)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }

                Text(
                    text = "नारद एक्सप्लोरर के एआई अनुक्रमणिका (Semantic Indexing) को सक्रिय रूप से डिवाइस के स्थानीय फोल्डरों और सभी दस्तावेजों (.pdf, .docx, .apk, .zip) को स्कैन करने के लिए 'MANAGE_EXTERNAL_STORAGE' (All Files Access) अधिकार की आवश्यकता है।",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = {
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            } else {
                                val intent = Intent(Settings.ACTION_SETTINGS)
                                context.startActivity(intent)
                            }
                        } catch (e: Exception) {
                            try {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                                    context.startActivity(intent)
                                }
                            } catch (fallbackEx: Exception) {
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("अनुमति सक्रिय करें (Open Settings)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    } else {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                .testTag("scoped_storage_permission_granted_banner")
        ) {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Storage Accessed",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "पूर्ण स्थानीय स्टोरेज एक्सेस सक्रिय है",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "MANAGE_EXTERNAL_STORAGE अनुमति एआई इंडेक्सिंग के लिए उपलब्ध है।",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
