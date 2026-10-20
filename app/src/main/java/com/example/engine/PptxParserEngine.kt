package com.example.engine

import android.content.Context
import android.net.Uri
import com.example.data.model.SlideModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.io.StringReader
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object PptxParserEngine {

    suspend fun parsePptx(context: Context, uri: Uri): List<SlideModel> = withContext(Dispatchers.IO) {
        val slidesMap = sortedMapOf<Int, String>()
        val notesMap = mutableMapOf<Int, String>()

        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                ZipInputStream(stream).use { zip ->
                    var entry: ZipEntry? = zip.nextEntry
                    val buffer = ByteArray(4096)
                    while (entry != null) {
                        val name = entry.name
                        if (name.startsWith("ppt/slides/slide") && name.endsWith(".xml")) {
                            // Extract slide number from filename, e.g., slide1.xml -> 1
                            val numStr = name.substringAfter("ppt/slides/slide").substringBefore(".xml")
                            val slideNum = numStr.toIntOrNull() ?: (slidesMap.size + 1)
                            val xmlContent = zip.bufferedReader(Charsets.UTF_8).readText()
                            slidesMap[slideNum] = xmlContent
                        } else if (name.startsWith("ppt/notesSlides/notesSlide") && name.endsWith(".xml")) {
                            val numStr = name.substringAfter("ppt/notesSlides/notesSlide").substringBefore(".xml")
                            val slideNum = numStr.toIntOrNull() ?: 1
                            val xmlContent = zip.bufferedReader(Charsets.UTF_8).readText()
                            notesMap[slideNum] = extractTextFromXml(xmlContent)
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (slidesMap.isNotEmpty()) {
            val result = mutableListOf<SlideModel>()
            var idx = 1
            for ((slideNum, xmlContent) in slidesMap) {
                val parsed = parseSlideXml(xmlContent, idx, notesMap[slideNum] ?: "")
                result.add(parsed)
                idx++
            }
            return@withContext result
        }

        // Fallback: Read as raw text/stream if not a standard zip
        val fallbackText = runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull() ?: ""

        return@withContext parseTextToSlides(fallbackText)
    }

    private fun parseSlideXml(xmlContent: String, slideNumber: Int, speakerNotes: String): SlideModel {
        val paragraphs = mutableListOf<String>()
        var title = ""
        var subtitle = ""

        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xmlContent))

            var eventType = parser.eventType
            val currentParagraph = StringBuilder()

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (parser.name == "p") {
                            currentParagraph.clear()
                        }
                    }
                    XmlPullParser.TEXT -> {
                        val text = parser.text?.trim()
                        if (!text.isNullOrEmpty()) {
                            currentParagraph.append(text).append(" ")
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "p") {
                            val pText = currentParagraph.toString().trim()
                            if (pText.isNotEmpty()) {
                                paragraphs.add(pText)
                            }
                            currentParagraph.clear()
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            // regex fallback for <a:t>
            val matcher = java.util.regex.Pattern.compile("<a:t>([^<]+)</a:t>").matcher(xmlContent)
            while (matcher.find()) {
                val t = matcher.group(1)?.trim()
                if (!t.isNullOrEmpty()) {
                    paragraphs.add(t)
                }
            }
        }

        if (paragraphs.isNotEmpty()) {
            title = paragraphs[0]
            val bullets = if (paragraphs.size > 1) {
                if (paragraphs.size > 2 && paragraphs[1].length < 60 && !paragraphs[1].endsWith(".")) {
                    subtitle = paragraphs[1]
                    paragraphs.drop(2)
                } else {
                    paragraphs.drop(1)
                }
            } else {
                emptyList()
            }

            val accentColors = listOf(
                0xFF0284C7, 0xFF6366F1, 0xFFF97316, 0xFF10B981, 0xFF8B5CF6, 0xFF0EA5E9
            )
            val accent = accentColors[(slideNumber - 1) % accentColors.size]

            return SlideModel(
                slideNumber = slideNumber,
                title = title,
                subtitle = subtitle,
                bulletPoints = bullets,
                rawText = paragraphs.joinToString("\n\n"),
                speakerNotes = speakerNotes,
                category = if (slideNumber == 1) "Title Slide" else "Content Slide",
                accentColorHex = accent
            )
        }

        return SlideModel(
            slideNumber = slideNumber,
            title = "Slide $slideNumber",
            bulletPoints = listOf("Slide content rendered from presentation deck."),
            speakerNotes = speakerNotes
        )
    }

    private fun extractTextFromXml(xmlContent: String): String {
        val sb = StringBuilder()
        val matcher = java.util.regex.Pattern.compile("<a:t>([^<]+)</a:t>").matcher(xmlContent)
        while (matcher.find()) {
            matcher.group(1)?.let { sb.append(it).append(" ") }
        }
        return sb.toString().trim()
    }

    fun parseTextToSlides(text: String): List<SlideModel> {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) {
            return listOf(
                SlideModel(
                    slideNumber = 1,
                    title = "Empty Document",
                    bulletPoints = listOf("No readable slide content detected.")
                )
            )
        }

        // Group into chunks of 4-6 lines per slide
        val slides = mutableListOf<SlideModel>()
        val chunkSize = 5
        val chunks = lines.chunked(chunkSize)

        chunks.forEachIndexed { index, chunk ->
            val slideTitle = chunk.firstOrNull() ?: "Slide ${index + 1}"
            val bullets = chunk.drop(1)
            slides.add(
                SlideModel(
                    slideNumber = index + 1,
                    title = slideTitle,
                    bulletPoints = bullets,
                    rawText = chunk.joinToString("\n"),
                    category = if (index == 0) "Overview" else "Details"
                )
            )
        }

        return slides
    }
}
