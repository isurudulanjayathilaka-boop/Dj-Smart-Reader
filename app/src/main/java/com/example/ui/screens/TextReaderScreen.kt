package com.example.ui.screens

import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.DocumentEntity
import com.example.ui.components.ReadingModeSheet
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.Navy900
import com.example.ui.theme.Slate400
import com.example.ui.viewmodel.DocumentViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun TextReaderScreen(
    document: DocumentEntity,
    viewModel: DocumentViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val readingMode by viewModel.readingMode.collectAsState()

    var textContent by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var fontSizeSp by remember { mutableFloatStateOf(16f) }
    var showReadingModeSheet by remember { mutableStateOf(false) }

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

    LaunchedEffect(document.id) {
        isLoading = true
        withContext(Dispatchers.IO) {
            val content = runCatching {
                context.contentResolver.openInputStream(Uri.parse(document.uriString))?.bufferedReader()?.use {
                    it.readText()
                }
            }.getOrNull() ?: "Unable to read document contents."
            textContent = content
            isLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(readingMode.backgroundColor)
            .testTag("text_reader_screen")
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
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }

                        Text(
                            text = document.title,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Increase font size
                        IconButton(onClick = { if (fontSizeSp < 28f) fontSizeSp += 2f }) {
                            Text("A+", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        // Decrease font size
                        IconButton(onClick = { if (fontSizeSp > 12f) fontSizeSp -= 2f }) {
                            Text("A-", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        // Themes
                        IconButton(onClick = { showReadingModeSheet = true }) {
                            Icon(Icons.Default.ColorLens, contentDescription = "Themes", tint = Color.White)
                        }

                        // Read Aloud
                        IconButton(
                            onClick = {
                                if (isTtsReady && textContent.isNotBlank()) {
                                    val preview = textContent.take(500)
                                    tts?.speak(preview, TextToSpeech.QUEUE_FLUSH, null, "txt_tts")
                                }
                            }
                        ) {
                            Icon(Icons.Default.RecordVoiceOver, contentDescription = "Read Aloud", tint = CyanAccent)
                        }
                    }
                }
            }

            // Body
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = CyanPrimary)
                }
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = textContent,
                        color = readingMode.textColor,
                        fontSize = fontSizeSp.sp,
                        lineHeight = (fontSizeSp * 1.5f).sp,
                        fontFamily = FontFamily.Default
                    )
                }
            }
        }
    }

    if (showReadingModeSheet) {
        ReadingModeSheet(
            currentMode = readingMode,
            onSelectMode = { viewModel.setReadingMode(it) },
            onDismiss = { showReadingModeSheet = false }
        )
    }
}
