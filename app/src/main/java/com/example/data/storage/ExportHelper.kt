package com.example.data.storage

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import android.media.MediaScannerConnection
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object ExportHelper {

    private fun getExportsDir(context: Context): File {
        val base = StorageUriHelper.getPublicDocumentsDir()
        val exportsDir = File(base, "VoiceNotes/Exports").apply { mkdirs() }
        return exportsDir
    }

    private fun sanitize(name: String): String {
        return name.replace(Regex("""[\\/:*?"<>|]"""), "_").trim().ifBlank { "Drawing" }
    }

    /**
     * Exports a bitmap as a 100% lossless PNG file and prompts the Android share sheet.
     */
    fun exportLosslessPng(context: Context, title: String, bitmap: Bitmap): File? {
        return try {
            val safeTitle = sanitize(title)
            val file = File(getExportsDir(context), "${safeTitle}_${System.currentTimeMillis() % 10000}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf("image/png"), null)
            shareFile(context, file, "image/png")
            Toast.makeText(context, "PNG saved: ${file.name}", Toast.LENGTH_SHORT).show()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to export PNG", Toast.LENGTH_SHORT).show()
            null
        }
    }

    /**
     * Exports a drawing bitmap as a vector/canvas PDF document and prompts the Android share sheet.
     */
    fun exportPdf(context: Context, title: String, bitmap: Bitmap): File? {
        return try {
            val safeTitle = sanitize(title)
            val file = File(getExportsDir(context), "${safeTitle}_${System.currentTimeMillis() % 10000}.pdf")

            val pdfDocument = PdfDocument()
            // Define page info using standard dimensions or bitmap aspect ratio
            val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            page.canvas.drawBitmap(bitmap, 0f, 0f, null)
            pdfDocument.finishPage(page)

            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf("application/pdf"), null)
            shareFile(context, file, "application/pdf")
            Toast.makeText(context, "PDF saved: ${file.name}", Toast.LENGTH_SHORT).show()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to export PDF", Toast.LENGTH_SHORT).show()
            null
        }
    }

    /**
     * Shares a file via Android FileProvider.
     */
    fun shareFile(context: Context, file: File, mimeType: String) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, null).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
