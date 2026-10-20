package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PdfRendererEngine(
    private val context: Context,
    private val uri: Uri
) {
    private var pfd: ParcelFileDescriptor? = null
    private var renderer: PdfRenderer? = null
    private var tempFile: File? = null
    private val renderLock = Any()

    suspend fun initialize(): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            synchronized(renderLock) {
                // If it's a file uri or content uri, prepare descriptor
                if (uri.scheme == "file") {
                    val file = File(uri.path ?: throw IllegalArgumentException("Invalid file URI"))
                    pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                } else {
                    // Copy content URI to app cache file to ensure persistent, reliable seekable access
                    val cacheDir = context.cacheDir
                    val temp = File(cacheDir, "pdf_preview_${System.currentTimeMillis()}.pdf")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(temp).use { output ->
                            input.copyTo(output)
                        }
                    } ?: throw IllegalStateException("Cannot open input stream for $uri")
                    tempFile = temp
                    pfd = ParcelFileDescriptor.open(temp, ParcelFileDescriptor.MODE_READ_ONLY)
                }

                val descriptor = pfd ?: throw IllegalStateException("ParcelFileDescriptor is null")
                val r = PdfRenderer(descriptor)
                renderer = r
                r.pageCount
            }
        }
    }

    suspend fun renderPage(pageIndex: Int, targetWidth: Int = 1080): Bitmap? = withContext(Dispatchers.IO) {
        synchronized(renderLock) {
            val r = renderer ?: return@withContext null
            if (pageIndex < 0 || pageIndex >= r.pageCount) return@withContext null

            var page: PdfRenderer.Page? = null
            try {
                page = r.openPage(pageIndex)
                val originalWidth = page.width
                val originalHeight = page.height

                val scale = if (originalWidth > 0) {
                    targetWidth.toFloat() / originalWidth.toFloat()
                } else 1f

                val destWidth = (originalWidth * scale).toInt().coerceAtLeast(1)
                val destHeight = (originalHeight * scale).toInt().coerceAtLeast(1)

                val bitmap = Bitmap.createBitmap(destWidth, destHeight, Bitmap.Config.ARGB_8888)
                // Initialize canvas to pure white background
                val canvas = android.graphics.Canvas(bitmap)
                canvas.drawColor(Color.WHITE)

                val matrix = Matrix().apply {
                    postScale(scale, scale)
                }

                page.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap
            } catch (e: Exception) {
                e.printStackTrace()
                null
            } finally {
                try {
                    page?.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun getPageCount(): Int {
        synchronized(renderLock) {
            return renderer?.pageCount ?: 0
        }
    }

    fun close() {
        synchronized(renderLock) {
            try {
                renderer?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            renderer = null

            try {
                pfd?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            pfd = null

            tempFile?.delete()
            tempFile = null
        }
    }
}
