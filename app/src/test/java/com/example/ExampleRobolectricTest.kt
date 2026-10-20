package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.DocumentType
import com.example.engine.PptxParserEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("DJ Smart Doc", appName)
    }

    @Test
    fun `document type parsing identifies PDF and PPTX`() {
        val pdfType = DocumentType.fromExtensionOrName("MyReport.pdf")
        assertEquals(DocumentType.PDF, pdfType)

        val pptxType = DocumentType.fromExtensionOrName("Presentation_Deck.pptx")
        assertEquals(DocumentType.PPTX, pptxType)

        val pptType = DocumentType.fromExtensionOrName("OldSlide.ppt")
        assertEquals(DocumentType.PPT, pptType)

        val txtType = DocumentType.fromExtensionOrName("Notes.txt")
        assertEquals(DocumentType.TXT, txtType)
    }

    @Test
    fun `parseTextToSlides generates slides correctly`() {
        val sampleText = """
            Welcome to DJ Smart Doc
            Feature One: PDF Rendering
            Feature Two: PowerPoint Slides
            Feature Three: Reading Modes
        """.trimIndent()

        val slides = PptxParserEngine.parseTextToSlides(sampleText)
        assertTrue(slides.isNotEmpty())
        assertEquals("Welcome to DJ Smart Doc", slides[0].title)
    }
}
