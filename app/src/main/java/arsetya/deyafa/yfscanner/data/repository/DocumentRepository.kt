package arsetya.deyafa.yfscanner.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.LiveData
import arsetya.deyafa.yfscanner.data.db.AppDatabase
import arsetya.deyafa.yfscanner.data.model.ScannedDocument
import arsetya.deyafa.yfscanner.data.model.ScannedPage
import arsetya.deyafa.yfscanner.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class DocumentRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val dao = db.documentDao()

    fun getAllDocuments(): LiveData<List<ScannedDocument>> = dao.getAllDocuments()

    fun searchDocuments(query: String): LiveData<List<ScannedDocument>> = dao.searchDocuments(query)

    suspend fun getDocumentById(id: Long): ScannedDocument? = dao.getDocumentById(id)

    fun getPagesByDocumentId(documentId: Long): LiveData<List<ScannedPage>> =
        dao.getPagesByDocumentId(documentId)

    suspend fun getPagesByDocumentIdSync(documentId: Long): List<ScannedPage> =
        dao.getPagesByDocumentIdSync(documentId)

    suspend fun createDocumentFromUris(title: String, imageUris: List<Uri>): Long {
        return withContext(Dispatchers.IO) {
            // Create document
            val document = ScannedDocument(
                title = title,
                pageCount = imageUris.size
            )
            val documentId = dao.insertDocument(document)

            // Save images and create pages
            val pages = imageUris.mapIndexed { index, uri ->
                val savedPath = FileUtils.saveImageFromUri(context, uri, documentId, index)
                ScannedPage(
                    documentId = documentId,
                    pageIndex = index,
                    originalImagePath = savedPath,
                    processedImagePath = savedPath
                )
            }
            dao.insertPages(pages)

            // Update thumbnail
            if (pages.isNotEmpty()) {
                val updatedDoc = dao.getDocumentById(documentId)
                updatedDoc?.let {
                    it.thumbnailPath = pages[0].originalImagePath
                    dao.updateDocument(it)
                }
            }

            documentId
        }
    }

    suspend fun addPagesToDocument(documentId: Long, imageUris: List<Uri>) {
        withContext(Dispatchers.IO) {
            val existingPages = dao.getPagesByDocumentIdSync(documentId)
            val startIndex = existingPages.size

            val pages = imageUris.mapIndexed { index, uri ->
                val savedPath = FileUtils.saveImageFromUri(context, uri, documentId, startIndex + index)
                ScannedPage(
                    documentId = documentId,
                    pageIndex = startIndex + index,
                    originalImagePath = savedPath,
                    processedImagePath = savedPath
                )
            }
            dao.insertPages(pages)

            val doc = dao.getDocumentById(documentId)
            doc?.let {
                it.pageCount = startIndex + imageUris.size
                it.updatedAt = System.currentTimeMillis()
                dao.updateDocument(it)
            }
        }
    }

    suspend fun updateDocumentTitle(documentId: Long, newTitle: String) {
        val doc = dao.getDocumentById(documentId)
        doc?.let {
            it.title = newTitle
            it.updatedAt = System.currentTimeMillis()
            dao.updateDocument(it)
        }
    }

    suspend fun updatePageFilter(page: ScannedPage, processedBitmap: Bitmap, filterName: String) {
        withContext(Dispatchers.IO) {
            val processedPath = FileUtils.saveBitmap(
                context, processedBitmap,
                page.documentId, page.pageIndex, filterName
            )
            page.processedImagePath = processedPath
            page.filterApplied = filterName
            dao.updatePage(page)

            // Update thumbnail if this is the first page
            if (page.pageIndex == 0) {
                val doc = dao.getDocumentById(page.documentId)
                doc?.let {
                    it.thumbnailPath = page.processedImagePath ?: page.originalImagePath
                    dao.updateDocument(it)
                }
            }
        }
    }

    suspend fun rotatePageImages(page: ScannedPage, degrees: Float) {
        withContext(Dispatchers.IO) {
            val oldOriginalPath = page.originalImagePath
            val oldProcessedPath = page.processedImagePath

            val originalBitmap = android.graphics.BitmapFactory.decodeFile(oldOriginalPath)
            if (originalBitmap != null) {
                val rotatedOriginal = arsetya.deyafa.yfscanner.util.ImageProcessor.rotateBitmap(originalBitmap, degrees)
                val newOriginalPath = FileUtils.saveBitmap(
                    context, rotatedOriginal,
                    page.documentId, page.pageIndex, "orig_${System.currentTimeMillis()}"
                )
                File(oldOriginalPath).delete()
                page.originalImagePath = newOriginalPath
                rotatedOriginal.recycle()
                originalBitmap.recycle()
            }

            if (oldProcessedPath != null && oldProcessedPath != oldOriginalPath) {
                val processedBitmap = android.graphics.BitmapFactory.decodeFile(oldProcessedPath)
                if (processedBitmap != null) {
                    val rotatedProcessed = arsetya.deyafa.yfscanner.util.ImageProcessor.rotateBitmap(processedBitmap, degrees)
                    val newProcessedPath = FileUtils.saveBitmap(
                        context, rotatedProcessed,
                        page.documentId, page.pageIndex, "${page.filterApplied}_${System.currentTimeMillis()}"
                    )
                    File(oldProcessedPath).delete()
                    page.processedImagePath = newProcessedPath
                    rotatedProcessed.recycle()
                    processedBitmap.recycle()
                }
            } else {
                page.processedImagePath = page.originalImagePath
            }

            dao.updatePage(page)

            if (page.pageIndex == 0) {
                val doc = dao.getDocumentById(page.documentId)
                doc?.let {
                    it.thumbnailPath = page.processedImagePath ?: page.originalImagePath
                    dao.updateDocument(it)
                }
            }
        }
    }

    suspend fun deleteDocument(document: ScannedDocument) {
        withContext(Dispatchers.IO) {
            // Delete image files
            FileUtils.deleteDocumentFiles(context, document.id)
            dao.deleteDocument(document)
        }
    }

    suspend fun deletePage(page: ScannedPage) {
        withContext(Dispatchers.IO) {
            // Delete image files
            File(page.originalImagePath).delete()
            page.processedImagePath?.let { File(it).delete() }
            dao.deletePage(page)

            // Update document page count
            val doc = dao.getDocumentById(page.documentId)
            doc?.let {
                it.pageCount = dao.getPageCount(page.documentId)
                it.updatedAt = System.currentTimeMillis()
                dao.updateDocument(it)
            }
        }
    }

    suspend fun deleteAllDocuments() {
        withContext(Dispatchers.IO) {
            FileUtils.deleteAllFiles(context)
            dao.deleteAllDocuments()
        }
    }

    suspend fun getStorageUsage(): Long {
        return withContext(Dispatchers.IO) {
            FileUtils.getStorageUsage(context)
        }
    }
}
