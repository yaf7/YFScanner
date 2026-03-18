package arsetya.deyafa.yfscanner.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object FileUtils {

    private const val DOCUMENTS_DIR = "scanned_documents"

    fun getDocumentDir(context: Context, documentId: Long): File {
        val dir = File(context.filesDir, "$DOCUMENTS_DIR/$documentId")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun saveImageFromUri(context: Context, uri: Uri, documentId: Long, pageIndex: Int): String {
        val dir = getDocumentDir(context, documentId)
        val file = File(dir, "page_${pageIndex}.jpg")

        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream)
                FileOutputStream(file).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                }
                bitmap.recycle()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return file.absolutePath
    }

    fun saveBitmap(
        context: Context,
        bitmap: Bitmap,
        documentId: Long,
        pageIndex: Int,
        suffix: String = ""
    ): String {
        val dir = getDocumentDir(context, documentId)
        val fileName = if (suffix.isNotEmpty()) "page_${pageIndex}_$suffix.jpg" else "page_${pageIndex}.jpg"
        val file = File(dir, fileName)

        try {
            FileOutputStream(file).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return file.absolutePath
    }

    fun deleteDocumentFiles(context: Context, documentId: Long) {
        val dir = getDocumentDir(context, documentId)
        if (dir.exists()) {
            dir.deleteRecursively()
        }
    }

    fun deleteAllFiles(context: Context) {
        val dir = File(context.filesDir, DOCUMENTS_DIR)
        if (dir.exists()) {
            dir.deleteRecursively()
        }
    }

    fun getStorageUsage(context: Context): Long {
        val dir = File(context.filesDir, DOCUMENTS_DIR)
        return if (dir.exists()) {
            dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        } else {
            0L
        }
    }

    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
            else -> String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024))
        }
    }
}
