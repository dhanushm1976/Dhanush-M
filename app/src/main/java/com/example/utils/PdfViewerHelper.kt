package com.example.utils

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object PdfViewerHelper {

    suspend fun getPageCount(file: File): Int = withContext(Dispatchers.IO) {
        try {
            if (!file.exists()) return@withContext 1
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    renderer.pageCount
                }
            }
        } catch (e: Exception) {
            1
        }
    }

    suspend fun renderPageBitmap(file: File, pageIndex: Int, densityDpi: Int = 240): Bitmap? = withContext(Dispatchers.IO) {
        try {
            if (!file.exists()) return@withContext null
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val safeIndex = pageIndex.coerceIn(0, (renderer.pageCount - 1).coerceAtLeast(0))
                    renderer.openPage(safeIndex).use { page ->
                        // Standardize resolution
                        val scale = 2.0f
                        val width = (page.width * scale).toInt().coerceAtLeast(300)
                        val height = (page.height * scale).toInt().coerceAtLeast(400)
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmap
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
