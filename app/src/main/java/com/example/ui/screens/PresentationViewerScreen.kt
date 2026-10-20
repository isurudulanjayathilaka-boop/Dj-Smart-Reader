package com.example.ui.screens

import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.DocumentEntity
import com.example.data.model.SlideModel
import com.example.data.sample.SampleDocumentProvider
import com.example.engine.PptxParserEngine
import com.example.ui.components.AddBookmarkDialog
import com.example.ui.components.JumpToPageDialog
import com.example.ui.components.ReadingModeSheet
import com.example.ui.components.SlideCanvas
import com.example.ui.components.SpeakerNotesCard
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.PptOrange
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.viewmodel.DocumentViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresentationViewerScreen(
    document: DocumentEntity,
    viewModel: DocumentViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val readingMode by viewModel.readingMode.collectAsState()
    val bookmarks by viewModel.currentDocumentBookmarks.collectAsState()

    var slides by remember { mutableStateOf<List<SlideModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Slideshow autoplay
    var isAutoplay by remember { mutableStateOf(false) }
    var autoplayIntervalSeconds by remember { mutableIntStateOf(5) }
    var showSpeedMenu by remember { mutableStateOf(false) }

    // Dialogs & Sheets
    var showReadingModeSheet by remember { mutableStateOf(false) }
    var showBookmarksSheet by remember { mutableStateOf(false) }
    var showAddBookmarkDialog by remember { mutableStateOf(false) }
    var showJumpDialog by remember { mutableStateOf(false) }
    var showSlideOutlineSheet by remember { mutableStateOf(false) }

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

    // Load slides
    LaunchedEffect(document.id) {
        isLoading = true
        val loadedSlides = if (document.isSample) {
            SampleDocumentProvider.getSampleSlides(document.id)
        } else {
            val uri = Uri.parse(document.uriString)
            PptxParserEngine.parsePptx(context, uri)
        }
        slides = loadedSlides.ifEmpty {
            SampleDocumentProvider.getSampleSlides(SampleDocumentProvider.SAMPLE_PPT_AI_ID)
        }
        isLoading = false
    }

    val totalSlides = slides.size.coerceAtLeast(1)
    val initialPage = (document.lastPageRead - 1).coerceIn(0, totalSlides - 1)
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { totalSlides }
    )

    // Save progress on slide change
    LaunchedEffect(pagerState.currentPage) {
        viewModel.updateProgress(pagerState.currentPage + 1)
    }

    // Autoplay timer loop
    LaunchedEffect(isAutoplay, pagerState.currentPage, totalSlides) {
        if (isAutoplay && totalSlides > 1) {
            delay(autoplayIntervalSeconds * 1000L)
            val nextPage = (pagerState.currentPage + 1) % totalSlides
            pagerState.animateScrollToPage(nextPage)
        }
    }

    val currentSlideIndex = pagerState.currentPage
    val currentSlide = slides.getOrNull(currentSlideIndex) ?: SlideModel(
        slideNumber = 1,
        title = document.title,
        bulletPoints = listOf("Loading presentation slides...")
    )

    val isCurrentSlideBookmarked = bookmarks.any { it.pageNumber == currentSlideIndex + 1 }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(readingMode.backgroundColor)
            .testTag("presentation_viewer_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Navy900,
                shadowElevation = 6.dp
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
                            modifier = Modifier.testTag("presentation_back_button")
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
                                    text = "Slide ${currentSlideIndex + 1} of $totalSlides ▾",
                                    color = PptOrange,
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
                                if (isCurrentSlideBookmarked) {
                                    scope.launch {
                                        viewModel.allBookmarks.value.find { it.pageNumber == currentSlideIndex + 1 }?.let {
                                            viewModel.removeBookmark(it.id)
                                        }
                                    }
                                } else {
                                    showAddBookmarkDialog = true
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isCurrentSlideBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                contentDescription = "Bookmark Slide",
                                tint = if (isCurrentSlideBookmarked) Color(0xFFF59E0B) else Color.White
                            )
                        }

                        // Reading theme
                        IconButton(onClick = { showReadingModeSheet = true }) {
                            Icon(
                                imageVector = Icons.Default.ColorLens,
                                contentDescription = "Themes",
                                tint = Color.White
                            )
                        }

                        // Read Aloud / TTS
                        IconButton(
                            onClick = {
                                if (isTtsReady) {
                                    val speechText = buildString {
                                        append(currentSlide.title).append(". ")
                                        if (currentSlide.subtitle.isNotBlank()) {
                                            append(currentSlide.subtitle).append(". ")
                                        }
                                        currentSlide.bulletPoints.forEach {
                                            append(it).append(". ")
                                        }
                                    }
                                    tts?.speak(speechText, TextToSpeech.QUEUE_FLUSH, null, "ppt_tts")
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = "Read Slide Aloud",
                                tint = CyanAccent
                            )
                        }
                    }
                }
            }

            // Slideshow Progress Bar when Autoplay is active
            if (isAutoplay) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = PptOrange,
                    trackColor = Navy800
                )
            }

            // Slide content body
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PptOrange)
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Slide Pager
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) { pageIdx ->
                        val slide = slides.getOrNull(pageIdx) ?: return@HorizontalPager
                        SlideCanvas(
                            slide = slide,
                            totalSlides = totalSlides,
                            readingMode = readingMode,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Speaker notes section
                    if (currentSlide.speakerNotes.isNotBlank()) {
                        SpeakerNotesCard(
                            notes = currentSlide.speakerNotes,
                            onSpeakClick = {
                                if (isTtsReady) {
                                    tts?.speak(currentSlide.speakerNotes, TextToSpeech.QUEUE_FLUSH, null, "notes_tts")
                                }
                            }
                        )
                    }
                }
            }

            // Bottom Presentation Control Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Navy900,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Slide Outline Button
                    IconButton(
                        onClick = { showSlideOutlineSheet = true },
                        modifier = Modifier.testTag("slide_outline_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatListNumbered,
                            contentDescription = "Slide Outline",
                            tint = Color.White
                        )
                    }

                    // Bookmarks Button
                    IconButton(onClick = { showBookmarksSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = "Bookmarks",
                            tint = if (bookmarks.isNotEmpty()) Color(0xFFF59E0B) else Color.White
                        )
                    }

                    // Slide Navigation Arrows
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    if (pagerState.currentPage > 0) {
                                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                    }
                                }
                            },
                            enabled = pagerState.currentPage > 0
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Previous Slide",
                                tint = if (pagerState.currentPage > 0) Color.White else Slate600
                            )
                        }

                        Text(
                            text = "${currentSlideIndex + 1} / $totalSlides",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        IconButton(
                            onClick = {
                                scope.launch {
                                    if (pagerState.currentPage < totalSlides - 1) {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    }
                                }
                            },
                            enabled = pagerState.currentPage < totalSlides - 1
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next Slide",
                                tint = if (pagerState.currentPage < totalSlides - 1) Color.White else Slate600
                            )
                        }
                    }

                    // Slideshow Autoplay Button
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { isAutoplay = !isAutoplay },
                            modifier = Modifier
                                .background(if (isAutoplay) PptOrange else Navy800, CircleShape)
                                .size(40.dp)
                                .testTag("presentation_autoplay_button")
                        ) {
                            Icon(
                                imageVector = if (isAutoplay) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isAutoplay) "Pause Slideshow" else "Start Slideshow",
                                tint = if (isAutoplay) Navy900 else Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Speed / Interval selector
                        Box {
                            IconButton(onClick = { showSpeedMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = "Slideshow Interval",
                                    tint = Slate400
                                )
                            }

                            DropdownMenu(
                                expanded = showSpeedMenu,
                                onDismissRequest = { showSpeedMenu = false }
                            ) {
                                listOf(3, 5, 8, 10).forEach { seconds ->
                                    DropdownMenuItem(
                                        text = { Text("$seconds seconds / slide") },
                                        onClick = {
                                            autoplayIntervalSeconds = seconds
                                            showSpeedMenu = false
                                        }
                                    )
                                }
                            }
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
            isSlide = true,
            onJumpToPage = { targetSlide ->
                scope.launch {
                    pagerState.animateScrollToPage(targetSlide - 1)
                }
            },
            onDeleteBookmark = { id ->
                viewModel.removeBookmark(id)
            },
            onDismiss = { showBookmarksSheet = false }
        )
    }

    if (showAddBookmarkDialog) {
        AddBookmarkDialog(
            pageNumber = currentSlideIndex + 1,
            isSlide = true,
            onSave = { title, note ->
                viewModel.addBookmark(currentSlideIndex + 1, title, note)
                showAddBookmarkDialog = false
            },
            onDismiss = { showAddBookmarkDialog = false }
        )
    }

    if (showJumpDialog) {
        JumpToPageDialog(
            currentPage = currentSlideIndex + 1,
            totalPages = totalSlides,
            isSlide = true,
            onJump = { target ->
                scope.launch {
                    pagerState.animateScrollToPage(target - 1)
                }
                showJumpDialog = false
            },
            onDismiss = { showJumpDialog = false }
        )
    }

    // Slide Outline Sheet
    if (showSlideOutlineSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showSlideOutlineSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Slide Deck Outline",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Select any slide to jump immediately",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate500,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(slides) { slide ->
                        val isSelected = slide.slideNumber == currentSlideIndex + 1
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    scope.launch {
                                        pagerState.animateScrollToPage(slide.slideNumber - 1)
                                        showSlideOutlineSheet = false
                                    }
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) PptOrange.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSelected) PptOrange else Slate600
                                ) {
                                    Text(
                                        text = "${slide.slideNumber}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = slide.title,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = if (isSelected) PptOrange else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    if (slide.subtitle.isNotBlank()) {
                                        Text(
                                            text = slide.subtitle,
                                            fontSize = 12.sp,
                                            color = Slate500,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
