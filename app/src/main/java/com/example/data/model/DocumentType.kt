package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.DocBlue
import com.example.ui.theme.DocBlueContainer
import com.example.ui.theme.PdfRed
import com.example.ui.theme.PdfRedContainer
import com.example.ui.theme.PptOrange
import com.example.ui.theme.PptOrangeContainer
import com.example.ui.theme.TxtGreen
import com.example.ui.theme.TxtGreenContainer

enum class DocumentType(
    val extension: String,
    val displayName: String,
    val badgeColor: Color,
    val containerColor: Color
) {
    PDF("pdf", "PDF Document", PdfRed, PdfRedContainer),
    PPTX("pptx", "PowerPoint Presentation", PptOrange, PptOrangeContainer),
    PPT("ppt", "PowerPoint Slides", PptOrange, PptOrangeContainer),
    TXT("txt", "Text Document", TxtGreen, TxtGreenContainer),
    DOCX("docx", "Word Document", DocBlue, DocBlueContainer);

    companion object {
        fun fromExtensionOrName(fileName: String): DocumentType {
            val lower = fileName.lowercase()
            return when {
                lower.endsWith(".pdf") -> PDF
                lower.endsWith(".pptx") -> PPTX
                lower.endsWith(".ppt") -> PPT
                lower.endsWith(".docx") || lower.endsWith(".doc") -> DOCX
                lower.endsWith(".txt") || lower.endsWith(".md") -> TXT
                else -> PDF
            }
        }
    }
}
