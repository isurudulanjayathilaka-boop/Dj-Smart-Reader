package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.BookmarkEntity
import com.example.data.local.DocumentEntity
import com.example.data.model.ReadingMode
import com.example.engine.PdfRendererEngine
import com.example.ui.components.AddBookmarkDialog
import com.example.ui.components.JumpToPageDialog
import com.example.ui.components.ReadingModeSheet
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.Navy700
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.PdfRed
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.viewmodel.DocumentViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfReaderScreen(
    document: DocumentEntity,
    viewModel: DocumentViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val readingMode by viewModel.readingMode.collectAsState()
    val bookmarks by viewModel.currentDocumentBookmarks.collectAsState()

    var currentPage by remember { mutableIntStateOf(document.lastPageRead.coerceIn(1, document.pageCount.coerceAtLeast(1))) }
    var totalPages by remember { mutableIntStateOf(document.pageCount.coerceAtLeast(1)) }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoadingPage by remember { mutableStateOf(true) }

    // Gesture states
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // UI overlays
    var showControls by remember { mutableStateOf(true) }
    var showReadingModeSheet by remember { mutableStateOf(false) }
    var showBookmarksSheet by remember { mutableStateOf(false) }
    var showAddBookmarkDialog by remember { mutableStateOf(false) }
    var showJumpDialog by remember { mutableStateOf(false) }
    var showThumbnailsSheet by remember { mutableStateOf(false) }

    // TTS
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var isTtsReady by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        var speech: TextToSpeech? = null
        speech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                speech?.language = Locale.US
                isTtsReady = true
            }
        }
        tts = speech
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    val renderer = remember(document.id) {
        PdfRendererEngine(context, Uri.parse(document.uriString))
    }

    DisposableEffect(renderer) {
        onDispose {
            renderer.close()
        }
    }

    // Initialize and load page
    LaunchedEffect(document.id) {
        val result = renderer.initialize()
        if (result.isSuccess) {
            val count = result.getOrDefault(document.pageCount).coerceAtLeast(1)
            totalPages = count
            currentPage = currentPage.coerceIn(1, count)
        }
    }

    // Render current page when page changes
    LaunchedEffect(currentPage, renderer) {
        isLoadingPage = true
        // reset zoom
        scale = 1f
        offset = Offset.Zero
        viewModel.updateProgress(currentPage)

        withContext(Dispatchers.IO) {
            val bmp = renderer.renderPage(currentPage - 1, targetWidth = 1440)
            currentBitmap = bmp
            isLoadingPage = false
        }
    }

    val isCurrentPageBookmarked = bookmarks.any { it.pageNumber == currentPage }

    // Color filter for night / sepia reading modes
    val colorFilter = remember(readingMode) {
        when (readingMode) {
            ReadingMode.NIGHT -> {
                // Invert colors for night reading
                val matrix = ColorMatrix(
                    floatArrayOf(
                        -1f, 0f, 0f, 0f, 255f,
                        0f, -1f, 0f, 0f, 255f,
                        0f, 0f, -1f, 0f, 255f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                ColorFilter.colorMatrix(matrix)
            }
            ReadingMode.SEPIA -> {
                // Warm sepia tone filter
                val matrix = ColorMatrix(
                    floatArrayOf(
                        0.95f, 0.05f, 0.0f, 0f, 15f,
                        0.05f, 0.90f, 0.0f, 0f, 10f,
                        0.0f, 0.05f, 0.80f, 0f, 0f,
                        0.0f, 0.0f, 0.0f, 1f, 0f
                    )
                )
                ColorFilter.colorMatrix(matrix)
            }
            else -> null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(readingMode.backgroundColor)
            .testTag("pdf_reader_screen")
    ) {
        // PDF Canvas with gestures
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            showControls = !showControls
                        },
                        onDoubleTap = {
                            if (scale > 1.2f) {
                                scale = 1f
                                offset = Offset.Zero
                            } else {
                                scale = 2.2f
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 4.5f)
                        if (scale > 1f) {
                            val maxOffsetX = (1440f * (scale - 1f)) / 2f
                            val maxOffsetY = (2000f * (scale - 1f)) / 2f
                            val newX = (offset.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX)
                            val newY = (offset.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                            offset = Offset(newX, newY)
                        } else {
                            offset = Offset.Zero
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (isLoadingPage) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = CyanPrimary,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Rendering Page $currentPage...",
                        color = readingMode.textColor,
                        fontSize = 13.sp
                    )
                }
            } else if (currentBitmap != null) {
                Image(
                    bitmap = currentBitmap!!.asImageBitmap(),
                    contentDescription = "PDF Page $currentPage",
                    colorFilter = colorFilter,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(if (showControls) 16.dp else 4.dp)
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                )
            } else {
                Text(
                    text = "Unable to render page $currentPage",
                    color = readingMode.textColor
                )
            }
        }

        // Top Navigation Bar
        AnimatedVisibility(
            visible = showControls,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                color = Navy900.copy(alpha = 0.94f),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("pdf_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }

                        Column(modifier = Modifier.padding(start = 4.dp)) {
                            Text(
                                text = document.title,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Navy800,
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .clickable { showJumpDialog = true }
                            ) {
                                Text(
                                    text = "Page $currentPage / $totalPages ▾",
                                    color = CyanAccent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Bookmark icon
                        IconButton(
                            onClick = {
                                if (isCurrentPageBookmarked) {
                                    scope.launch {
                                        viewModel.allBookmarks.value.find { it.pageNumber == currentPage }?.let {
                                            viewModel.removeBookmark(it.id)
                                        }
                                    }
                                } else {
                                    showAddBookmarkDialog = true
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isCurrentPageBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                contentDescription = "Bookmark",
                                tint = if (isCurrentPageBookmarked) Color(0xFFF59E0B) else Color.White
                            )
                        }

                        // Reading Mode Palette
                        IconButton(onClick = { showReadingModeSheet = true }) {
                            Icon(
                                imageVector = Icons.Default.ColorLens,
                                contentDescription = "Reading Themes",
                                tint = Color.White
                            )
                        }

                        // Speech / TTS
                        IconButton(
                            onClick = {
                                if (isTtsReady) {
                                    val text = "${document.title}. Reading page $currentPage of $totalPages."
                                    tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "pdf_tts")
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = "Read page info aloud",
                                tint = CyanAccent
                            )
                        }
                    }
                }
            }
        }

        // Bottom Navigation Bar
        AnimatedVisibility(
            visible = showControls,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = Navy900.copy(alpha = 0.94f),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    // Page scrubber slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "1",
                            color = Slate400,
                            fontSize = 11.sp
                        )

                        Slider(
                            value = currentPage.toFloat(),
                            onValueChange = { currentPage = it.toInt() },
                            valueRange = 1f..totalPages.toFloat().coerceAtLeast(1f),
                            steps = if (totalPages > 2) totalPages - 2 else 0,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp)
                                .testTag("pdf_page_slider"),
                            colors = SliderDefaults.colors(
                                thumbColor = CyanAccent,
                                activeTrackColor = CyanPrimary,
                                inactiveTrackColor = Navy700
                            )
                        )

                        Text(
                            text = "$totalPages",
                            color = Slate400,
                            fontSize = 11.sp
                        )
                    }

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Thumbnail Drawer Button
                        IconButton(
                            onClick = { showThumbnailsSheet = true },
                            modifier = Modifier.testTag("pdf_thumbnails_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridOn,
                                contentDescription = "Thumbnails",
                                tint = Color.White
                            )
                        }

                        // Bookmarks Button
                        IconButton(onClick = { showBookmarksSheet = true }) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = "Bookmarks List",
                                tint = if (bookmarks.isNotEmpty()) Color(0xFFF59E0B) else Color.White
                            )
                        }

                        // Page Prev
                        IconButton(
                            onClick = {
                                if (currentPage > 1) currentPage--
                            },
                            enabled = currentPage > 1
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Previous Page",
                                tint = if (currentPage > 1) Color.White else Slate600
                            )
                        }

                        // Page Indicator
                        Text(
                            text = "$currentPage / $totalPages",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )

                        // Page Next
                        IconButton(
                            onClick = {
                                if (currentPage < totalPages) currentPage++
                            },
                            enabled = currentPage < totalPages
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next Page",
                                tint = if (currentPage < totalPages) Color.White else Slate600
                            )
                        }

                        // Reset zoom
                        IconButton(
                            onClick = {
                                scale = 1f
                                offset = Offset.Zero
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ZoomOut,
                                contentDescription = "Reset Zoom",
                                tint = if (scale > 1.05f) CyanAccent else Slate600
                            )
                        }
                    }
                }
            }
        }
    }

    // Sheets & Dialogs
    if (showReadingModeSheet) {
        ReadingModeSheet(
            currentMode = readingMode,
            onSelectMode = { viewModel.setReadingMode(it) },
            onDismiss = { showReadingModeSheet = false }
        )
    }

    if (showBookmarksSheet) {
        BookmarksSheet(
            bookmarks = bookmarks,
            isSlide = false,
            onJumpToPage = { targetPage ->
                currentPage = targetPage
            },
            onDeleteBookmark = { id ->
                viewModel.removeBookmark(id)
            },
            onDismiss = { showBookmarksSheet = false }
        )
    }

    if (showAddBookmarkDialog) {
        AddBookmarkDialog(
            pageNumber = currentPage,
            isSlide = false,
            onSave = { title, note ->
                viewModel.addBookmark(currentPage, title, note)
                showAddBookmarkDialog = false
            },
            onDismiss = { showAddBookmarkDialog = false }
        )
    }

    if (showJumpDialog) {
        JumpToPageDialog(
            currentPage = currentPage,
            totalPages = totalPages,
            isSlide = false,
            onJump = { target ->
                currentPage = target
                showJumpDialog = false
            },
            onDismiss = { showJumpDialog = false }
        )
    }

    // Thumbnail Grid Sheet
    if (showThumbnailsSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showThumbnailsSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Page Overview",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Tap any page to jump immediately",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate500,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 70.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.height(300.dp)
                ) {
                    items((1..totalPages).toList()) { pageNum ->
                        val isSelected = pageNum == currentPage
                        val isBookmarked = bookmarks.any { it.pageNumber == pageNum }

                        Box(
                            modifier = Modifier
                                .size(70.dp, 90.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) CyanPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) CyanPrimary else Slate300,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    currentPage = pageNum
                                    showThumbnailsSheet = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                if (isBookmarked) {
                                    Icon(
                                        imageVector = Icons.Default.Bookmark,
                                        contentDescription = null,
                                        tint = Color(0xFFF59E0B),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = "$pageNum",
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) CyanPrimary else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
