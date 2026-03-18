package arsetya.deyafa.yfscanner.ui

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import arsetya.deyafa.yfscanner.R
import arsetya.deyafa.yfscanner.data.repository.DocumentRepository
import arsetya.deyafa.yfscanner.databinding.ActivitySettingsBinding
import arsetya.deyafa.yfscanner.util.FileUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: SharedPreferences
    private lateinit var repository: DocumentRepository

    companion object {
        const val PREFS_NAME = "yfscanner_prefs"
        const val KEY_QUALITY = "image_quality"
        const val KEY_PAGE_SIZE = "pdf_page_size"
        const val KEY_AUTO_ENHANCE = "auto_enhance"
        const val KEY_DARK_MODE = "dark_mode"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.root.setPadding(0, systemBars.top, 0, systemBars.bottom)
            insets
        }

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        repository = DocumentRepository(this)

        setupUI()
        setupClickListeners()
        loadSettings()
        loadStorageUsage()
    }

    private fun setupUI() {
        // Version text removed
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { finish() }

        // Quality radio group
        binding.rgQuality.setOnCheckedChangeListener { _, checkedId ->
            val quality = when (checkedId) {
                R.id.rbLow -> "low"
                R.id.rbMedium -> "medium"
                R.id.rbHigh -> "high"
                else -> "medium"
            }
            prefs.edit { putString(KEY_QUALITY, quality) }
        }

        // Page size radio group
        binding.rgPageSize.setOnCheckedChangeListener { _, checkedId ->
            val pageSize = when (checkedId) {
                R.id.rbA4 -> "a4"
                R.id.rbLetter -> "letter"
                else -> "a4"
            }
            prefs.edit { putString(KEY_PAGE_SIZE, pageSize) }
        }

        // Auto enhance switch
        binding.switchAutoEnhance.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit { putBoolean(KEY_AUTO_ENHANCE, isChecked) }
        }

        // Dark mode switch
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit { putBoolean(KEY_DARK_MODE, isChecked) }
            if (isChecked) {
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                    androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                )
            } else {
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                    androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
                )
            }
        }

        // Clear all button
        binding.btnClearAll.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.clear_all))
                .setMessage(getString(R.string.clear_all_confirm))
                .setPositiveButton(getString(R.string.yes)) { _, _ ->
                    CoroutineScope(Dispatchers.Main).launch {
                        repository.deleteAllDocuments()
                        loadStorageUsage()
                        Toast.makeText(this@SettingsActivity, "All documents cleared", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton(getString(R.string.no), null)
                .show()
        }
    }

    private fun loadSettings() {
        // Quality
        when (prefs.getString(KEY_QUALITY, "medium")) {
            "low" -> binding.rbLow.isChecked = true
            "medium" -> binding.rbMedium.isChecked = true
            "high" -> binding.rbHigh.isChecked = true
        }

        // Page size
        when (prefs.getString(KEY_PAGE_SIZE, "a4")) {
            "a4" -> binding.rbA4.isChecked = true
            "letter" -> binding.rbLetter.isChecked = true
        }

        // Auto enhance
        binding.switchAutoEnhance.isChecked = prefs.getBoolean(KEY_AUTO_ENHANCE, false)

        // Dark mode
        binding.switchDarkMode.isChecked = prefs.getBoolean(KEY_DARK_MODE, false)
    }

    private fun loadStorageUsage() {
        CoroutineScope(Dispatchers.Main).launch {
            val bytes = withContext(Dispatchers.IO) { repository.getStorageUsage() }
            binding.tvStorageUsage.text = FileUtils.formatFileSize(bytes)
        }
    }
}
