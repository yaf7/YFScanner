package arsetya.deyafa.yfscanner.util

import android.content.Context
import android.graphics.BitmapFactory
import arsetya.deyafa.yfscanner.data.model.ScannedPage
import com.itextpdf.text.Document
import com.itextpdf.text.Image
import com.itextpdf.text.PageSize
import com.itextpdf.text.Rectangle
import com.itextpdf.text.pdf.PdfWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object PdfExporter {

    enum class PageSizeOption {
        A4, LETTER
    }

    suspend fun exportToPdf(
        context: Context,
        pages: List<ScannedPage>,
        outputFileName: String,
        pageSizeOption: PageSizeOption = PageSizeOption.A4
    ): File? {
        return withContext(Dispatchers.IO) {
            try {
                val outputDir = File(context.getExternalFilesDir(null), "exports")
                if (!outputDir.exists()) outputDir.mkdirs()

                val outputFile = File(outputDir, "${outputFileName}.pdf")

                val pageSize = when (pageSizeOption) {
                    PageSizeOption.A4 -> PageSize.A4
                    PageSizeOption.LETTER -> PageSize.LETTER
                }

                val document = Document(pageSize, 0f, 0f, 0f, 0f)
                PdfWriter.getInstance(document, FileOutputStream(outputFile))
                document.open()

                for (page in pages) {
                    val imagePath = page.processedImagePath ?: page.originalImagePath
                    val file = File(imagePath)
                    if (!file.exists()) continue

                    val bitmap = BitmapFactory.decodeFile(imagePath)
                    val stream = ByteArrayOutputStream()
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, stream)
                    val imageBytes = stream.toByteArray()
                    bitmap.recycle()

                    val image = Image.getInstance(imageBytes)

                    // Scale image to fit page
                    val pageWidth = pageSize.width
                    val pageHeight = pageSize.height

                    image.scaleToFit(pageWidth, pageHeight)

                    // Center image on page
                    val x = (pageWidth - image.scaledWidth) / 2
                    val y = (pageHeight - image.scaledHeight) / 2
                    image.setAbsolutePosition(x, y)

                    document.newPage()
                    document.add(image)
                }

                document.close()
                outputFile
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun getExportedPdfs(context: Context): List<File> {
        return withContext(Dispatchers.IO) {
            val outputDir = File(context.getExternalFilesDir(null), "exports")
            if (outputDir.exists()) {
                outputDir.listFiles()?.filter { it.extension == "pdf" }?.toList() ?: emptyList()
            } else {
                emptyList()
            }
        }
    }
}
