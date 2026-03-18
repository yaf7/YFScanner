package arsetya.deyafa.yfscanner.ui

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import arsetya.deyafa.yfscanner.R
import arsetya.deyafa.yfscanner.data.model.ScannedPage
import arsetya.deyafa.yfscanner.data.repository.DocumentRepository
import arsetya.deyafa.yfscanner.databinding.ActivityDocumentViewerBinding
import arsetya.deyafa.yfscanner.ui.adapter.PageAdapter
import arsetya.deyafa.yfscanner.util.ImageProcessor
import arsetya.deyafa.yfscanner.util.PdfExporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class DocumentViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDocumentViewerBinding
    private lateinit var repository: DocumentRepository
    private lateinit var pageAdapter: PageAdapter
    private var documentId: Long = -1
    private var currentPages: List<ScannedPage> = emptyList()

    private val filterLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Refresh pages after filter applied
            // Handled automatically by LiveData observer
        }
    }

    private val addPageLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            scanResult?.pages?.let { pages ->
                if (pages.isNotEmpty()) {
                    val uris = pages.map { it.imageUri }
                    CoroutineScope(Dispatchers.Main).launch {
                        repository.addPagesToDocument(documentId, uris)
                        Toast.makeText(this@DocumentViewerActivity, "Pages added", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDocumentViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.viewerRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.toolbar.setPadding(
                binding.toolbar.paddingLeft, systemBars.top,
                binding.toolbar.paddingRight, binding.toolbar.paddingBottom
            )
            binding.bottomBar.setPadding(
                binding.bottomBar.paddingLeft, binding.bottomBar.paddingTop,
                binding.bottomBar.paddingRight, systemBars.bottom + binding.bottomBar.paddingBottom
            )
            insets
        }

        documentId = intent.getLongExtra("document_id", -1)
        if (documentId == -1L) {
            finish()
            return
        }

        repository = DocumentRepository(this)
        setupViewPager()
        setupClickListeners()
        loadDocumentTitle()
        observePages()
    }

    private fun setupViewPager() {
        pageAdapter = PageAdapter()
        binding.viewPager.adapter = pageAdapter

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updatePageIndicator(position)
            }
        })
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnEdit.setOnClickListener { showRenameDialog() }

        binding.btnAddPage.setOnClickListener { addPages() }

        binding.btnFilter.setOnClickListener {
            val currentPosition = binding.viewPager.currentItem
            if (currentPosition in currentPages.indices) {
                val page = currentPages[currentPosition]
                val intent = Intent(this, FilterActivity::class.java)
                intent.putExtra("page_id", page.id)
                intent.putExtra("document_id", documentId)
                intent.putExtra("image_path", page.processedImagePath ?: page.originalImagePath)
                filterLauncher.launch(intent)
            }
        }

        binding.btnOcr.setOnClickListener {
            val currentPosition = binding.viewPager.currentItem
            if (currentPosition in currentPages.indices) {
                val page = currentPages[currentPosition]
                val intent = Intent(this, OcrActivity::class.java)
                intent.putExtra("image_path", page.processedImagePath ?: page.originalImagePath)
                startActivity(intent)
            }
        }

        binding.btnRotate.setOnClickListener {
            rotatePage()
        }

        binding.btnExportPdf.setOnClickListener { exportPdf() }

        binding.btnShare.setOnClickListener { sharePdf() }

        binding.btnDelete.setOnClickListener { showDeletePageDialog() }

        binding.btnMore.setOnClickListener {
            showMoreOptions()
        }
    }

    private fun loadDocumentTitle() {
        CoroutineScope(Dispatchers.Main).launch {
            val doc = withContext(Dispatchers.IO) { repository.getDocumentById(documentId) }
            doc?.let {
                binding.tvDocTitle.text = it.title
            }
        }
    }

    private fun observePages() {
        repository.getPagesByDocumentId(documentId).observe(this) { pages ->
            // Save current position before submitting list
            val currentPos = binding.viewPager.currentItem
            val wasEmpty = currentPages.isEmpty()
            
            currentPages = pages
            pageAdapter.submitList(pages)

            // Restore position if possible
            if (!wasEmpty && currentPages.isNotEmpty()) {
                binding.viewPager.setCurrentItem(currentPos.coerceIn(0, currentPages.size - 1), false)
            }
            updatePageIndicator(binding.viewPager.currentItem)

            if (pages.isEmpty()) {
                finish()
            }
        }
    }

    private fun updatePageIndicator(position: Int) {
        if (currentPages.isNotEmpty()) {
            binding.tvPageIndicator.text = getString(
                R.string.page_indicator,
                (position + 1).coerceIn(1, currentPages.size),
                currentPages.size
            )
            binding.tvPageIndicator.visibility = View.VISIBLE
        } else {
            binding.tvPageIndicator.visibility = View.GONE
        }
    }

    private fun showRenameDialog() {
        val editText = EditText(this).apply {
            setText(binding.tvDocTitle.text)
            setTextColor(getColor(R.color.white))
            setPadding(48, 32, 48, 32)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.rename))
            .setView(editText)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val newTitle = editText.text.toString().trim()
                if (newTitle.isNotEmpty()) {
                    CoroutineScope(Dispatchers.Main).launch {
                        repository.updateDocumentTitle(documentId, newTitle)
                        binding.tvDocTitle.text = newTitle
                    }
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun addPages() {
        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(10)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_BASE)
            .build()

        val scanner = GmsDocumentScanning.getClient(options)
        scanner.getStartScanIntent(this)
            .addOnSuccessListener { intentSender ->
                addPageLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
            .addOnFailureListener {
                Toast.makeText(this, getString(R.string.scanner_unavailable), Toast.LENGTH_SHORT).show()
            }
    }

    private fun rotatePage() {
        val currentPosition = binding.viewPager.currentItem
        if (currentPosition in currentPages.indices) {
            val page = currentPages[currentPosition]
            val imagePath = page.processedImagePath ?: page.originalImagePath

            CoroutineScope(Dispatchers.Main).launch {
                repository.rotatePageImages(page, 90f)
            }
        }
    }

    private fun exportPdf() {
        if (currentPages.isEmpty()) return

        Toast.makeText(this, getString(R.string.exporting_pdf), Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.Main).launch {
            val doc = withContext(Dispatchers.IO) { repository.getDocumentById(documentId) }
            val fileName = doc?.title?.replace(Regex("[^a-zA-Z0-9_\\- ]"), "") ?: "scan_${documentId}"

            val pdfFile = PdfExporter.exportToPdf(
                this@DocumentViewerActivity,
                currentPages,
                fileName
            )

            if (pdfFile != null) {
                Toast.makeText(this@DocumentViewerActivity, getString(R.string.pdf_exported), Toast.LENGTH_LONG).show()

                // Open the PDF
                try {
                    val uri = FileProvider.getUriForFile(
                        this@DocumentViewerActivity,
                        "${packageName}.fileprovider",
                        pdfFile
                    )
                    val openIntent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/pdf")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(openIntent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                Toast.makeText(this@DocumentViewerActivity, getString(R.string.pdf_export_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sharePdf() {
        if (currentPages.isEmpty()) return

        CoroutineScope(Dispatchers.Main).launch {
            val doc = withContext(Dispatchers.IO) { repository.getDocumentById(documentId) }
            val fileName = doc?.title?.replace(Regex("[^a-zA-Z0-9_\\- ]"), "") ?: "scan_${documentId}"

            val pdfFile = PdfExporter.exportToPdf(
                this@DocumentViewerActivity,
                currentPages,
                fileName
            )

            if (pdfFile != null) {
                val uri = FileProvider.getUriForFile(
                    this@DocumentViewerActivity,
                    "${packageName}.fileprovider",
                    pdfFile
                )
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, doc?.title ?: "Scanned Document")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(shareIntent, getString(R.string.share)))
            }
        }
    }

    private fun showDeletePageDialog() {
        val currentPosition = binding.viewPager.currentItem
        if (currentPosition !in currentPages.indices) return

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.delete))
            .setMessage("Delete this page?")
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                CoroutineScope(Dispatchers.Main).launch {
                    repository.deletePage(currentPages[currentPosition])
                }
            }
            .setNegativeButton(getString(R.string.no), null)
            .show()
    }

    private fun showMoreOptions() {
        val options = arrayOf(getString(R.string.rename), getString(R.string.delete))
        MaterialAlertDialogBuilder(this)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showRenameDialog()
                    1 -> showDeleteDocumentDialog()
                }
            }
            .show()
    }

    private fun showDeleteDocumentDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.delete))
            .setMessage(getString(R.string.confirm_delete))
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                CoroutineScope(Dispatchers.Main).launch {
                    val doc = withContext(Dispatchers.IO) { repository.getDocumentById(documentId) }
                    doc?.let { repository.deleteDocument(it) }
                    finish()
                }
            }
            .setNegativeButton(getString(R.string.no), null)
            .show()
    }
}
