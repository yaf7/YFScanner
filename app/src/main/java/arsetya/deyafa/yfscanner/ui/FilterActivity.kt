package arsetya.deyafa.yfscanner.ui

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import arsetya.deyafa.yfscanner.R
import arsetya.deyafa.yfscanner.data.repository.DocumentRepository
import arsetya.deyafa.yfscanner.databinding.ActivityFilterBinding
import arsetya.deyafa.yfscanner.ui.adapter.FilterAdapter
import arsetya.deyafa.yfscanner.util.ImageProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FilterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFilterBinding
    private lateinit var repository: DocumentRepository
    private var filterAdapter: FilterAdapter? = null
    private var originalBitmap: Bitmap? = null
    private var currentFilteredBitmap: Bitmap? = null
    private var currentFilterType: ImageProcessor.FilterType = ImageProcessor.FilterType.ORIGINAL
    private var pageId: Long = -1
    private var documentId: Long = -1
    private var imagePath: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityFilterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.toolbar.setPadding(
                binding.toolbar.paddingLeft, systemBars.top,
                binding.toolbar.paddingRight, binding.toolbar.paddingBottom
            )
            
            // Adjust Apply button bottom margin to be above the nav bar
            val params = binding.btnApply.layoutParams as android.view.ViewGroup.MarginLayoutParams
            params.bottomMargin = systemBars.bottom + (16 * resources.displayMetrics.density).toInt()
            binding.btnApply.layoutParams = params
            
            insets
        }

        pageId = intent.getLongExtra("page_id", -1)
        documentId = intent.getLongExtra("document_id", -1)
        imagePath = intent.getStringExtra("image_path") ?: ""

        if (pageId == -1L || imagePath.isEmpty()) {
            finish()
            return
        }

        repository = DocumentRepository(this)
        loadImage()
        setupClickListeners()
    }

    private fun loadImage() {
        originalBitmap = BitmapFactory.decodeFile(imagePath)
        originalBitmap?.let { bitmap ->
            binding.ivPreview.setImageBitmap(bitmap)
            setupFilterList(bitmap)
        } ?: run {
            Toast.makeText(this, getString(R.string.error_generic), Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupFilterList(bitmap: Bitmap) {
        filterAdapter = FilterAdapter(bitmap) { filterType ->
            currentFilterType = filterType
            
            CoroutineScope(Dispatchers.Default).launch {
                val filteredBitmap = ImageProcessor.applyFilter(bitmap, filterType)
                withContext(Dispatchers.Main) {
                    currentFilteredBitmap = filteredBitmap
                    binding.ivPreview.setImageBitmap(filteredBitmap)
                }
            }
        }

        binding.filterList.apply {
            layoutManager = LinearLayoutManager(
                this@FilterActivity,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = filterAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnCancel.setOnClickListener { finish() }

        binding.btnApply.setOnClickListener {
            applyFilter()
        }
    }

    private fun applyFilter() {
        val bitmapToSave = currentFilteredBitmap ?: originalBitmap ?: return

        CoroutineScope(Dispatchers.Main).launch {
            val page = withContext(Dispatchers.IO) {
                repository.getPagesByDocumentIdSync(documentId).find { it.id == pageId }
            }

            page?.let {
                repository.updatePageFilter(it, bitmapToSave, currentFilterType.name.lowercase())
                Toast.makeText(this@FilterActivity, getString(R.string.filter_applied), Toast.LENGTH_SHORT).show()
                setResult(Activity.RESULT_OK)
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        filterAdapter?.cleanup()
    }
}
