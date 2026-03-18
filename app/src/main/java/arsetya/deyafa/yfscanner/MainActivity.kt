package arsetya.deyafa.yfscanner

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import arsetya.deyafa.yfscanner.data.repository.DocumentRepository
import arsetya.deyafa.yfscanner.databinding.ActivityMainBinding
import arsetya.deyafa.yfscanner.ui.DocumentViewerActivity
import arsetya.deyafa.yfscanner.ui.SettingsActivity
import arsetya.deyafa.yfscanner.ui.adapter.DocumentAdapter
import arsetya.deyafa.yfscanner.ui.viewmodel.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var documentAdapter: DocumentAdapter
    private lateinit var repository: DocumentRepository
    private var scanner: GmsDocumentScanner? = null

    private val scannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            scanResult?.pages?.let { pages ->
                if (pages.isNotEmpty()) {
                    val uris = pages.map { it.imageUri }
                    val defaultTitle = "Scan ${SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date())}"

                    val editText = android.widget.EditText(this).apply {
                        setText(defaultTitle)
                        setTextColor(getColor(R.color.white))
                        setPadding(48, 32, 48, 32)
                        setSingleLine(true)
                    }

                    MaterialAlertDialogBuilder(this)
                        .setTitle("Simpan Dokumen")
                        .setView(editText)
                        .setPositiveButton(getString(R.string.save)) { _, _ ->
                            val newTitle = editText.text.toString().trim()
                            saveScannedDocument(if (newTitle.isNotEmpty()) newTitle else defaultTitle, uris)
                        }
                        .setCancelable(false)
                        .show()
                }
            }
        }
    }

    private fun saveScannedDocument(title: String, uris: List<android.net.Uri>) {
        CoroutineScope(Dispatchers.Main).launch {
            val docId = repository.createDocumentFromUris(title, uris)
            Toast.makeText(this@MainActivity, getString(R.string.scan_complete), Toast.LENGTH_SHORT).show()

            // Open the document
            val intent = Intent(this@MainActivity, DocumentViewerActivity::class.java)
            intent.putExtra("document_id", docId)
            startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Handle system bar insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val statusBarSpacer = binding.headerLayout.findViewById<View>(R.id.statusBarSpacer)
            val params = statusBarSpacer.layoutParams
            params.height = systemBars.top
            statusBarSpacer.layoutParams = params
            binding.fabNewScan.apply {
                val fabParams = layoutParams as ViewGroup.MarginLayoutParams
                fabParams.bottomMargin = systemBars.bottom + 20
                layoutParams = fabParams
            }
            insets
        }

        repository = DocumentRepository(this)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        setupRecyclerView()
        setupObservers()
        setupClickListeners()
        setupSearch()
        setupScanner()
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val query = s?.toString() ?: ""
                viewModel.searchDocuments(query)
                binding.btnClearSearch.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
            }
        })

        binding.btnClearSearch.setOnClickListener {
            binding.etSearch.setText("")
            binding.etSearch.clearFocus()

            // Hide keyboard
            val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
        }
    }

    private fun setupRecyclerView() {
        documentAdapter = DocumentAdapter(
            onItemClick = { document ->
                val intent = Intent(this, DocumentViewerActivity::class.java)
                intent.putExtra("document_id", document.id)
                startActivity(intent)
            },
            onItemLongClick = { document ->
                showOptionsDialog(document)
            }
        )

        binding.rvDocuments.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 2)
            adapter = documentAdapter
        }
    }

    private fun setupObservers() {
        viewModel.documents.observe(this) { documents ->
            documentAdapter.submitList(documents)

            binding.emptyState.visibility = if (documents.isEmpty()) View.VISIBLE else View.GONE
            binding.rvDocuments.visibility = if (documents.isEmpty()) View.GONE else View.VISIBLE

            val countText = if (documents.isEmpty()) {
                getString(R.string.my_documents)
            } else {
                "${getString(R.string.my_documents)} (${documents.size})"
            }
            binding.tvDocumentCount.text = countText
        }
    }

    private fun setupClickListeners() {
        binding.fabNewScan.setOnClickListener {
            startScan()
        }

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun startScan() {
        scanner?.getStartScanIntent(this)
            ?.addOnSuccessListener { intentSender ->
                scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
            ?.addOnFailureListener { e ->
                Toast.makeText(this, getString(R.string.scanner_unavailable), Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
    }

    private fun setupScanner() {
        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(20)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_BASE)
            .build()

        scanner = GmsDocumentScanning.getClient(options)
    }

    private fun showOptionsDialog(document: arsetya.deyafa.yfscanner.data.model.ScannedDocument) {
        val options = arrayOf(getString(R.string.rename), getString(R.string.delete))
        MaterialAlertDialogBuilder(this)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showRenameDialog(document)
                    1 -> showDeleteDialog(document)
                }
            }
            .show()
    }

    private fun showRenameDialog(document: arsetya.deyafa.yfscanner.data.model.ScannedDocument) {
        val editText = android.widget.EditText(this).apply {
            setText(document.title)
            setTextColor(getColor(R.color.white))
            setPadding(48, 32, 48, 32)
            setSingleLine(true)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.rename))
            .setView(editText)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val newTitle = editText.text.toString().trim()
                if (newTitle.isNotEmpty() && newTitle != document.title) {
                    CoroutineScope(Dispatchers.Main).launch {
                        repository.updateDocumentTitle(document.id, newTitle)
                        // This triggers a DB update, observer will refresh the list.
                    }
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showDeleteDialog(document: arsetya.deyafa.yfscanner.data.model.ScannedDocument) {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.delete))
            .setMessage(getString(R.string.confirm_delete))
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                viewModel.deleteDocument(document)
                Snackbar.make(binding.root, getString(R.string.delete_document), Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton(getString(R.string.no), null)
            .show()
    }
}