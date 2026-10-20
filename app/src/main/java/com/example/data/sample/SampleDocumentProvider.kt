package com.example.data.sample

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.example.data.local.DocumentEntity
import com.example.data.model.SlideModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object SampleDocumentProvider {

    const val SAMPLE_PDF_ID = "sample_dj_smart_doc_guide"
    const val SAMPLE_PPT_AI_ID = "sample_ai_presentation"
    const val SAMPLE_PPT_BIZ_ID = "sample_biz_pitch"
    const val SAMPLE_TXT_ID = "sample_welcome_notes"

    suspend fun getInitialSampleDocuments(context: Context): List<DocumentEntity> = withContext(Dispatchers.IO) {
        // 1. Generate Guide PDF
        val pdfFile = generateGuidePdf(context)

        // 2. Prepare sample slides
        val pptFile = generateSamplePptxFile(context)

        // 3. Prepare notes file
        val txtFile = generateSampleTxtFile(context)

        listOf(
            DocumentEntity(
                id = SAMPLE_PDF_ID,
                title = "DJ Smart Doc - Complete User Guide.pdf",
                uriString = Uri.fromFile(pdfFile).toString(),
                fileType = "PDF",
                pageCount = 4,
                lastPageRead = 1,
                progressPercentage = 25,
                fileSizeBytes = pdfFile.length(),
                lastOpenedTimestamp = System.currentTimeMillis() - 60_000,
                isFavorite = true,
                isSample = true,
                summary = "Official user guide for DJ Smart Doc covering PDF navigation, zoom controls, presentation slideshows, and eye comfort reading modes."
            ),
            DocumentEntity(
                id = SAMPLE_PPT_AI_ID,
                title = "Next-Gen AI & Smart Documents.pptx",
                uriString = Uri.fromFile(pptFile).toString(),
                fileType = "PPTX",
                pageCount = 6,
                lastPageRead = 1,
                progressPercentage = 16,
                fileSizeBytes = 48_500L,
                lastOpenedTimestamp = System.currentTimeMillis() - 120_000,
                isFavorite = true,
                isSample = true,
                summary = "Executive keynote presentation exploring automated document intelligence, slide presentations on mobile, and next-gen reading tools."
            ),
            DocumentEntity(
                id = SAMPLE_PPT_BIZ_ID,
                title = "Business Strategy & Mobile Growth.pptx",
                uriString = "slides_biz_pitch",
                fileType = "PPTX",
                pageCount = 5,
                lastPageRead = 1,
                progressPercentage = 20,
                fileSizeBytes = 36_200L,
                lastOpenedTimestamp = System.currentTimeMillis() - 240_000,
                isFavorite = false,
                isSample = true,
                summary = "Strategic slide deck highlighting mobile market analytics, product roadmap, quarterly milestones, and monetization pillars."
            ),
            DocumentEntity(
                id = SAMPLE_TXT_ID,
                title = "DJ Smart Doc - Quick Notes.txt",
                uriString = Uri.fromFile(txtFile).toString(),
                fileType = "TXT",
                pageCount = 1,
                lastPageRead = 1,
                progressPercentage = 100,
                fileSizeBytes = txtFile.length(),
                lastOpenedTimestamp = System.currentTimeMillis() - 360_000,
                isFavorite = false,
                isSample = true,
                summary = "Quick cheat sheet with keyboard & gesture shortcuts, supported file types, and document reading tips."
            )
        )
    }

    private fun generateGuidePdf(context: Context): File {
        val file = File(context.filesDir, "dj_smart_doc_user_guide.pdf")
        if (file.exists() && file.length() > 0) return file

        val pdfDoc = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842

        val titlePaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val headingPaint = Paint().apply {
            color = Color.parseColor("#0284C7")
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val bodyPaint = Paint().apply {
            color = Color.parseColor("#334155")
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val footerPaint = Paint().apply {
            color = Color.parseColor("#94A3B8")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            isAntiAlias = true
        }

        val bannerPaint = Paint().apply {
            color = Color.parseColor("#0B132B")
            style = Paint.Style.FILL
        }

        val whiteText = Paint().apply {
            color = Color.WHITE
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val cyanSub = Paint().apply {
            color = Color.parseColor("#38BDF8")
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val cardBgPaint = Paint().apply {
            color = Color.parseColor("#F1F5F9")
            style = Paint.Style.FILL
        }

        // --- Page 1: Welcome & Overview ---
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        var page = pdfDoc.startPage(pageInfo)
        var canvas = page.canvas

        // Header Banner
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 130f, bannerPaint)
        canvas.drawText("DJ SMART DOC", 40f, 60f, whiteText)
        canvas.drawText("Next-Generation Document & Presentation Reader", 40f, 85f, cyanSub)
        canvas.drawText("Created by DJ • Version 1.0", 40f, 105f, footerPaint.apply { color = Color.parseColor("#94A3B8") })

        var y = 170f
        canvas.drawText("1. Welcome to DJ Smart Doc", 40f, y, titlePaint)
        y += 30f
        canvas.drawText("DJ Smart Doc is your all-in-one mobile document reading and presentation platform.", 40f, y, bodyPaint)
        y += 20f
        canvas.drawText("Designed for speed, visual clarity, and versatility, it seamlessly renders:", 40f, y, bodyPaint)
        y += 25f

        // Cards for formats
        canvas.drawRoundRect(40f, y, 260f, y + 80f, 12f, 12f, cardBgPaint)
        canvas.drawText("PDF Reader Engine", 55f, y + 30f, headingPaint)
        canvas.drawText("Crisp high-resolution rendering,", 55f, y + 50f, bodyPaint)
        canvas.drawText("pinch-to-zoom & page scrubber.", 55f, y + 68f, bodyPaint)

        canvas.drawRoundRect(290f, y, 555f, y + 80f, 12f, 12f, cardBgPaint)
        canvas.drawText("PowerPoint Slides (PPTX)", 305f, y + 30f, headingPaint.apply { color = Color.parseColor("#F97316") })
        canvas.drawText("Presentation slide deck player,", 305f, y + 50f, bodyPaint)
        canvas.drawText("speaker notes & auto-slideshow.", 305f, y + 68f, bodyPaint)

        y += 120f
        canvas.drawText("2. Key Capabilities", 40f, y, headingPaint.apply { color = Color.parseColor("#0284C7") })
        y += 24f
        val bulletsP1 = listOf(
            "• Universal Storage Access: Open any PDF, PPT, PPTX or TXT file from device storage.",
            "• Four Reading Comfort Modes: Clean Light, Eye Comfort Sepia, Dark OLED Night & Forest Calm.",
            "• Interactive Page Bookmarking: Bookmark crucial pages and attach personalized notes.",
            "• Instant Page Navigation: Slider scrubber, jump-to-page dialog, and thumbnail drawer.",
            "• Presentation Autoplay: Present slide decks with customizable timer transitions.",
            "• Text-to-Speech (TTS): Listen to document summaries and presentation notes hands-free."
        )
        for (bullet in bulletsP1) {
            canvas.drawText(bullet, 40f, y, bodyPaint)
            y += 22f
        }

        canvas.drawText("Page 1 of 4 • DJ Smart Doc Official Manual", 40f, (pageHeight - 30).toFloat(), footerPaint)
        pdfDoc.finishPage(page)

        // --- Page 2: PDF Reading & Gesture Guide ---
        pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 2).create()
        page = pdfDoc.startPage(pageInfo)
        canvas = page.canvas

        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 70f, bannerPaint)
        canvas.drawText("PDF READER NAVIGATION & GESTURES", 40f, 45f, whiteText.apply { textSize = 16f })

        y = 105f
        canvas.drawText("Navigating Your PDF Documents", 40f, y, titlePaint)
        y += 30f

        val pdfTips = listOf(
            Pair("Pinch-to-Zoom", "Use two fingers to smoothly zoom up to 4x. Double tap anytime to reset zoom."),
            Pair("Pan & Drag", "When zoomed in, drag with one finger across the screen to pan effortlessly."),
            Pair("Page Slider", "Drag the bottom page scrubber slider to jump rapidly through hundreds of pages."),
            Pair("Thumbnail Drawer", "Tap the thumbnail button on the bottom bar to view the page grid."),
            Pair("Fullscreen Toggle", "Tap the screen center to toggle the top and bottom navigation bars for immersive reading."),
            Pair("Direct Page Jump", "Tap the page indicator (e.g. '3 / 24') to enter any page number directly.")
        )

        for ((title, desc) in pdfTips) {
            canvas.drawRoundRect(40f, y, (pageWidth - 40).toFloat(), y + 54f, 8f, 8f, cardBgPaint)
            canvas.drawText(title, 55f, y + 22f, headingPaint)
            canvas.drawText(desc, 55f, y + 42f, bodyPaint)
            y += 68f
        }

        canvas.drawText("Page 2 of 4 • DJ Smart Doc Official Manual", 40f, (pageHeight - 30).toFloat(), footerPaint)
        pdfDoc.finishPage(page)

        // --- Page 3: PowerPoint & Presentation Mode ---
        pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 3).create()
        page = pdfDoc.startPage(pageInfo)
        canvas = page.canvas

        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 70f, bannerPaint)
        canvas.drawText("POWERPOINT & SLIDE DECK VIEWER", 40f, 45f, whiteText.apply { textSize = 16f })

        y = 105f
        canvas.drawText("Presenting and Reading Slides", 40f, y, titlePaint)
        y += 30f

        val pptTips = listOf(
            Pair("Projector Slide Layout", "Each slide is rendered in standard 16:9 widescreen with distinct visual cards."),
            Pair("Auto-Play Slideshow", "Press the Play button to start an automatic slideshow with 3s, 5s, or 10s intervals."),
            Pair("Speaker Notes Panel", "Tap the Speaker Notes toggle at the bottom to review notes for the active slide."),
            Pair("Slide Outline & Switcher", "Access the slide drawer to preview slide titles and jump to any section."),
            Pair("Text-to-Speech Aloud", "Tap the Read Aloud button to hear bullet points spoken clearly through your speakers.")
        )

        for ((title, desc) in pptTips) {
            canvas.drawRoundRect(40f, y, (pageWidth - 40).toFloat(), y + 54f, 8f, 8f, cardBgPaint)
            canvas.drawText(title, 55f, y + 22f, headingPaint.apply { color = Color.parseColor("#F97316") })
            canvas.drawText(desc, 55f, y + 42f, bodyPaint)
            y += 68f
        }

        canvas.drawText("Page 3 of 4 • DJ Smart Doc Official Manual", 40f, (pageHeight - 30).toFloat(), footerPaint)
        pdfDoc.finishPage(page)

        // --- Page 4: Eye Comfort & Bookmarks ---
        pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 4).create()
        page = pdfDoc.startPage(pageInfo)
        canvas = page.canvas

        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 70f, bannerPaint)
        canvas.drawText("EYE COMFORT & PRODUCTIVITY TIPS", 40f, 45f, whiteText.apply { textSize = 16f })

        y = 105f
        canvas.drawText("Reading Comfort & Bookmarks", 40f, y, titlePaint)
        y += 30f

        canvas.drawText("Four Tailored Reading Palettes:", 40f, y, headingPaint.apply { color = Color.parseColor("#0284C7") })
        y += 25f

        val palettes = listOf(
            "• Clean Light: Bright, crisp high-contrast background optimized for bright day environments.",
            "• Eye Comfort Sepia: Warm paper hue reducing blue light exposure during long study hours.",
            "• Dark OLED Night: Deep true black theme saving battery life and easing eye fatigue in the dark.",
            "• Forest Calm: Nature-inspired gentle mint tint designed to relax your vision."
        )
        for (p in palettes) {
            canvas.drawText(p, 40f, y, bodyPaint)
            y += 22f
        }

        y += 20f
        canvas.drawText("Saving & Managing Bookmarks:", 40f, y, headingPaint)
        y += 25f
        val bookmarkTips = listOf(
            "1. While reading any page, tap the Bookmark icon in the top app bar.",
            "2. Enter a custom title and optional study note for future reference.",
            "3. Access all bookmarked pages anytime from the bottom bar Bookmarks tab.",
            "4. Your progress is saved automatically so you can resume anytime from the Home hub."
        )
        for (b in bookmarkTips) {
            canvas.drawText(b, 40f, y, bodyPaint)
            y += 22f
        }

        y += 35f
        canvas.drawRoundRect(40f, y, (pageWidth - 40).toFloat(), y + 60f, 8f, 8f, cardBgPaint)
        canvas.drawText("Enjoy effortless document reading with DJ Smart Doc!", 55f, y + 25f, headingPaint)
        canvas.drawText("Built with Kotlin, Jetpack Compose and Material 3 design.", 55f, y + 45f, footerPaint)

        canvas.drawText("Page 4 of 4 • DJ Smart Doc Official Manual", 40f, (pageHeight - 30).toFloat(), footerPaint)
        pdfDoc.finishPage(page)

        try {
            FileOutputStream(file).use { out ->
                pdfDoc.writeTo(out)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            pdfDoc.close()
        }

        return file
    }

    private fun generateSamplePptxFile(context: Context): File {
        val file = File(context.filesDir, "next_gen_ai_smart_docs.txt")
        if (!file.exists()) {
            file.writeText(
                """
                # Next-Gen AI & Smart Documents
                Exploring Future Mobile Reading & Presentation Experiences
                - Created for DJ Smart Doc platform
                - Native Android rendering & slide analytics
                
                # Intelligent Document Processing
                - Optical structure extraction and layout preservation
                - Instant chapter and slide outline segmentation
                - Context-aware search and semantic page indexing
                
                # Mobile Presentation Innovation
                - Fullscreen widescreen slide projector mode
                - Seamless speaker notes synchronization
                - Hands-free auto slideshow timer controls
                - Crisp typography with dynamic contrast
                
                # Eye Comfort & Ergonomics
                - Reading fatigue reduction via calibrated color palettes
                - Day, Night, Sepia, and Forest paper themes
                - Zero eye glare and ambient light adaptation
                
                # Audio & Accessibility Features
                - Natural voice Text-to-Speech (TTS) integration
                - Screen reader TalkBack semantic node compatibility
                - Large touch targets adhering to 48dp M3 standard
                
                # Summary & Next Milestones
                - DJ Smart Doc elevates mobile productivity
                - Offline-first privacy and instant responsiveness
                - Thank you for choosing DJ Smart Doc!
                """.trimIndent()
            )
        }
        return file
    }

    private fun generateSampleTxtFile(context: Context): File {
        val file = File(context.filesDir, "dj_smart_doc_quick_notes.txt")
        if (!file.exists()) {
            file.writeText(
                """
                ========================================
                DJ SMART DOC - QUICK CHEAT SHEET
                ========================================
                
                Welcome to DJ Smart Doc!
                
                SUPPORTED FORMATS:
                • PDF (.pdf) - Portable Document Format
                • PowerPoint (.pptx, .ppt) - Presentation Slides
                • Text (.txt, .md) - Plain text and markdown notes
                • Word (.docx) - Document files
                
                READER SHORTCUTS:
                • Double Tap: Reset zoom or toggle fit-to-screen
                • Single Tap: Toggle reading controls & fullscreen
                • Bottom Scrubber: Rapid page scrubbing
                • Speaker Button: Read current page / slide aloud
                • Bookmark Button: Save page with personal annotations
                
                READING MODES:
                1. Clean Light - Standard daylight reading
                2. Sepia - Warm paper comfort
                3. Dark OLED - Battery saver & low-light
                4. Forest - Soft green relax tone
                
                Your documents stay private on your device.
                Enjoy reading with DJ Smart Doc!
                """.trimIndent()
            )
        }
        return file
    }

    fun getSampleSlides(documentId: String): List<SlideModel> {
        return when (documentId) {
            SAMPLE_PPT_BIZ_ID -> listOf(
                SlideModel(
                    slideNumber = 1,
                    title = "Business Strategy & Mobile Growth",
                    subtitle = "Q3/Q4 Strategic Roadmap & Milestones",
                    bulletPoints = listOf(
                        "Accelerating smart document mobile adoption worldwide",
                        "Key performance metrics and quarterly achievements",
                        "Presented by DJ Smart Doc Strategic Team"
                    ),
                    speakerNotes = "Welcome everyone. Today we are presenting our strategic roadmap focusing on mobile productivity and document tools.",
                    category = "Title Slide",
                    accentColorHex = 0xFFF97316
                ),
                SlideModel(
                    slideNumber = 2,
                    title = "Market Landscape & User Demand",
                    subtitle = "Why mobile document reading matters",
                    bulletPoints = listOf(
                        "78% of professionals review documents and presentations on mobile",
                        "Demand for fast, lightweight PDF and PPTX readers is rising",
                        "Users prioritize zero-lag rendering and eye-friendly themes",
                        "High demand for combined PDF and Presentation tools"
                    ),
                    speakerNotes = "Highlight the 78% mobile usage statistic and emphasize user preference for eye comfort.",
                    category = "Market Analysis",
                    accentColorHex = 0xFF0284C7
                ),
                SlideModel(
                    slideNumber = 3,
                    title = "Core Product Value Pillars",
                    subtitle = "What makes DJ Smart Doc unique",
                    bulletPoints = listOf(
                        "Unified Hub: Read PDFs and present PPTX in one fluid app",
                        "Offline Privacy: Zero cloud tracking; all docs rendered locally",
                        "Ultra-Responsive: Native Android graphics and memory management",
                        "Adaptive Ergonomics: Sepia and Dark reading themes"
                    ),
                    speakerNotes = "Walk through the four core pillars. Emphasize offline privacy and native speed.",
                    category = "Product Pillars",
                    accentColorHex = 0xFF6366F1
                ),
                SlideModel(
                    slideNumber = 4,
                    title = "Growth Roadmap & Timeline",
                    subtitle = "Upcoming milestones and feature drops",
                    bulletPoints = listOf(
                        "Phase 1: PDF Renderer, Presentation Player, Room Persistence (Complete)",
                        "Phase 2: Text-to-Speech audio reading & slide bookmarks (Complete)",
                        "Phase 3: Document search, category filtering & export sharing (Complete)",
                        "Phase 4: Advanced annotation markup and stylus support (Upcoming)"
                    ),
                    speakerNotes = "Review our release phases and celebrate our milestone completions.",
                    category = "Roadmap",
                    accentColorHex = 0xFF10B981
                ),
                SlideModel(
                    slideNumber = 5,
                    title = "Conclusion & Action Items",
                    subtitle = "Questions & Discussions",
                    bulletPoints = listOf(
                        "DJ Smart Doc delivers modern, versatile document reading",
                        "Expand sample library and user feedback loop",
                        "Ready for daily productivity on any Android device!",
                        "Thank you!"
                    ),
                    speakerNotes = "Open the floor for questions and thank participants.",
                    category = "Conclusion",
                    accentColorHex = 0xFF8B5CF6
                )
            )

            else -> listOf(
                SlideModel(
                    slideNumber = 1,
                    title = "Next-Gen AI & Smart Documents",
                    subtitle = "Exploring Future Mobile Reading & Presentation Experiences",
                    bulletPoints = listOf(
                        "Created for DJ Smart Doc platform",
                        "Native Android rendering & slide presentation controls",
                        "Empowering executives, students, and professionals"
                    ),
                    speakerNotes = "Welcome to the presentation on Next-Gen Smart Documents and mobile reading experiences.",
                    category = "Title Slide",
                    accentColorHex = 0xFF0284C7
                ),
                SlideModel(
                    slideNumber = 2,
                    title = "Intelligent Document Processing",
                    subtitle = "Modern document architecture",
                    bulletPoints = listOf(
                        "Optical structure parsing and layout preservation",
                        "Instant chapter and slide outline segmentation",
                        "Context-aware search and semantic page indexing",
                        "High performance vector rendering on mobile screens"
                    ),
                    speakerNotes = "This slide explains how documents are parsed into structured slides with native clarity.",
                    category = "Architecture",
                    accentColorHex = 0xFF6366F1
                ),
                SlideModel(
                    slideNumber = 3,
                    title = "Mobile Presentation Innovation",
                    subtitle = "Present anywhere from your pocket",
                    bulletPoints = listOf(
                        "Fullscreen widescreen slide projector mode",
                        "Seamless speaker notes synchronization",
                        "Hands-free auto slideshow timer controls (3s, 5s, 10s)",
                        "Crisp typography with dynamic contrast"
                    ),
                    speakerNotes = "Demonstrate the auto-play slideshow timer and presenter notes expander.",
                    category = "Features",
                    accentColorHex = 0xFFF97316
                ),
                SlideModel(
                    slideNumber = 4,
                    title = "Eye Comfort & Ergonomics",
                    subtitle = "Reading without fatigue",
                    bulletPoints = listOf(
                        "Reading fatigue reduction via calibrated color palettes",
                        "Day, Night, Sepia, and Forest paper themes",
                        "Zero eye glare and ambient light adaptation",
                        "Smooth pinch-to-zoom and double-tap repositioning"
                    ),
                    speakerNotes = "Reading comfort is vital when reviewing long documents and decks.",
                    category = "Design",
                    accentColorHex = 0xFF10B981
                ),
                SlideModel(
                    slideNumber = 5,
                    title = "Audio & Accessibility Features",
                    subtitle = "Multi-sensory document engagement",
                    bulletPoints = listOf(
                        "Natural voice Text-to-Speech (TTS) integration",
                        "Screen reader TalkBack semantic node compatibility",
                        "Large touch targets adhering to 48dp M3 standard",
                        "Instant bookmarking with personalized notes"
                    ),
                    speakerNotes = "TTS allows hands-free listening while commuting or reviewing notes.",
                    category = "Accessibility",
                    accentColorHex = 0xFF0EA5E9
                ),
                SlideModel(
                    slideNumber = 6,
                    title = "Summary & Key Takeaways",
                    subtitle = "DJ Smart Doc elevates mobile productivity",
                    bulletPoints = listOf(
                        "Offline-first privacy and instant responsiveness",
                        "Seamless support for PDFs, PowerPoint decks, and text notes",
                        "Designed for effortless everyday document reading",
                        "Thank you for choosing DJ Smart Doc!"
                    ),
                    speakerNotes = "Wrap up the deck and invite attendees to try the features.",
                    category = "Summary",
                    accentColorHex = 0xFF8B5CF6
                )
            )
        }
    }
}
