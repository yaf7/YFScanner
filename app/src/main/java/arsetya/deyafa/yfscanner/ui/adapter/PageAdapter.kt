package arsetya.deyafa.yfscanner.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import arsetya.deyafa.yfscanner.data.model.ScannedPage
import arsetya.deyafa.yfscanner.databinding.ItemPagePreviewBinding
import java.io.File

class PageAdapter : RecyclerView.Adapter<PageAdapter.ViewHolder>() {

    private val pages = mutableListOf<ScannedPage>()

    fun submitList(newPages: List<ScannedPage>) {
        pages.clear()
        pages.addAll(newPages)
        notifyDataSetChanged()
    }

    fun getPage(position: Int): ScannedPage? {
        return if (position in pages.indices) pages[position] else null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPagePreviewBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(pages[position])
    }

    override fun getItemCount() = pages.size

    inner class ViewHolder(
        private val binding: ItemPagePreviewBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(page: ScannedPage) {
            val imagePath = page.processedImagePath ?: page.originalImagePath
            val file = java.io.File(imagePath)
            if (file.exists()) {
                Glide.with(binding.root.context)
                    .load(file)
                    .signature(com.bumptech.glide.signature.ObjectKey(file.lastModified()))
                    .into(binding.ivPageImage)
            }
        }
    }
}
