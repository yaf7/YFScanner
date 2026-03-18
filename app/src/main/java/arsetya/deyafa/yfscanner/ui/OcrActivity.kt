package arsetya.deyafa.yfscanner.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import arsetya.deyafa.yfscanner.R
import arsetya.deyafa.yfscanner.databinding.ActivityOcrBinding

class OcrActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOcrBinding
    private var imagePath: String = ""
    private var extractedStr: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityOcrBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.toolbar.setPadding(
                binding.toolbar.paddingLeft, systemBars.top,
                binding.toolbar.paddingRight, binding.toolbar.paddingBottom
            )
            insets
        }

        imagePath = intent.getStringExtra("image_path") ?: ""
        if (imagePath.isEmpty()) {
            finish()
            return
        }

        setupClickListeners()
        loadImageAndExtractText()
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnCopy.setOnClickListener {
            if (extractedStr.isNotEmpty()) {
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Scanned Text", extractedStr)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, getString(R.string.text_copied), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadImageAndExtractText() {
        try {
            val bitmap = BitmapFactory.decodeFile(imagePath)
            if (bitmap != null) {
                binding.ivPreview.setImageBitmap(bitmap)
                
                // Initialize ML Kit Text Recognizer
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                val image = InputImage.fromBitmap(bitmap, 0)

                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        binding.progressBar.visibility = View.GONE
                        extractedStr = visionText.text
                        if (extractedStr.isNotEmpty()) {
                            binding.tvExtractedText.text = extractedStr
                            binding.btnCopy.visibility = View.VISIBLE
                        } else {
                            binding.tvExtractedText.text = getString(R.string.no_text_found)
                        }
                    }
                    .addOnFailureListener { e ->
                        binding.progressBar.visibility = View.GONE
                        binding.tvExtractedText.text = getString(R.string.error_generic)
                        e.printStackTrace()
                    }
            } else {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, getString(R.string.error_generic), Toast.LENGTH_SHORT).show()
                finish()
            }
        } catch (e: Exception) {
            binding.progressBar.visibility = View.GONE
            Toast.makeText(this, getString(R.string.error_generic), Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }
}
