package arsetya.deyafa.yfscanner.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import arsetya.deyafa.yfscanner.R
import arsetya.deyafa.yfscanner.data.model.ScannedDocument
import arsetya.deyafa.yfscanner.databinding.ItemDocumentBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class DocumentAdapter(
    private val onItemClick: (ScannedDocument) -> Unit,
    private val onItemLongClick: (ScannedDocument) -> Unit
) : ListAdapter<ScannedDocument, DocumentAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDocumentBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemDocumentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(document: ScannedDocument) {
            binding.tvTitle.text = document.title

            val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            binding.tvDate.text = dateFormat.format(Date(document.updatedAt))

            val pageText = if (document.pageCount == 1) {
                binding.root.context.getString(R.string.one_page)
            } else {
                binding.root.context.getString(R.string.pages_count, document.pageCount)
            }
            binding.tvPageCount.text = pageText

            // Load thumbnail
            document.thumbnailPath?.let { path ->
                val file = File(path)
                if (file.exists()) {
                    Glide.with(binding.root.context)
                        .load(file)
                        .signature(com.bumptech.glide.signature.ObjectKey(file.lastModified()))
                        .transform(CenterCrop())
                        .placeholder(R.drawable.bg_thumbnail_placeholder)
                        .into(binding.ivThumbnail)
                }
            }

            // Click listeners
            binding.root.setOnClickListener { onItemClick(document) }
            binding.root.setOnLongClickListener {
                onItemLongClick(document)
                true
            }

            // Entry animation
            binding.root.alpha = 0f
            binding.root.translationY = 50f
            binding.root.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300)
                .setStartDelay((bindingAdapterPosition * 50).toLong())
                .start()
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ScannedDocument>() {
        override fun areItemsTheSame(oldItem: ScannedDocument, newItem: ScannedDocument) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ScannedDocument, newItem: ScannedDocument) =
            oldItem == newItem
    }
}
