package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.data.local.DocumentEntity
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PdfReaderScreen
import com.example.ui.screens.PresentationViewerScreen
import com.example.ui.screens.TextReaderScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.DocumentViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: DocumentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle incoming document intent (e.g. user opens a PDF from Downloads or Files app)
        handleIntent(intent)

        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val uri: Uri? = intent.data
            if (uri != null) {
                viewModel.importUri(uri) { importedDoc ->
                    if (importedDoc != null) {
                        viewModel.selectDocument(importedDoc)
                    }
                }
            }
        }
    }
}

@Composable
fun AppNavigation(viewModel: DocumentViewModel) {
    val currentDoc by viewModel.currentDocument.collectAsState()

    Crossfade(
        targetState = currentDoc,
        label = "screen_transition"
    ) { doc ->
        if (doc == null) {
            HomeScreen(
                viewModel = viewModel,
                onOpenDocument = { selectedDoc ->
                    viewModel.selectDocument(selectedDoc)
                }
            )
        } else {
            when (doc.fileType) {
                "PDF" -> {
                    PdfReaderScreen(
                        document = doc,
                        viewModel = viewModel,
                        onBack = {
                            viewModel.clearSelectedDocument()
                        }
                    )
                }
                "PPTX", "PPT" -> {
                    PresentationViewerScreen(
                        document = doc,
                        viewModel = viewModel,
                        onBack = {
                            viewModel.clearSelectedDocument()
                        }
                    )
                }
                else -> {
                    TextReaderScreen(
                        document = doc,
                        viewModel = viewModel,
                        onBack = {
                            viewModel.clearSelectedDocument()
                        }
                    )
                }
            }
        }
    }
}
