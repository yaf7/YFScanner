package arsetya.deyafa.yfscanner.ui.adapter

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import arsetya.deyafa.yfscanner.R
import arsetya.deyafa.yfscanner.databinding.ItemFilterOptionBinding
import arsetya.deyafa.yfscanner.util.ImageProcessor

class FilterAdapter(
    private val originalBitmap: Bitmap,
    private val onFilterSelected: (ImageProcessor.FilterType) -> Unit
) : RecyclerView.Adapter<FilterAdapter.ViewHolder>() {

    private val filters = ImageProcessor.FilterType.entries.toList()
    private var selectedPosition = 0
    private val previewCache = mutableMapOf<ImageProcessor.FilterType, Bitmap>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFilterOptionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(filters[position], position == selectedPosition)
    }

    override fun getItemCount() = filters.size

    inner class ViewHolder(
        private val binding: ItemFilterOptionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(filterType: ImageProcessor.FilterType, isSelected: Boolean) {
            binding.tvFilterName.text = ImageProcessor.getFilterName(filterType)

            // Generate or use cached preview
            val preview = previewCache.getOrPut(filterType) {
                val smallBitmap = Bitmap.createScaledBitmap(originalBitmap, 100, 100, true)
                ImageProcessor.applyFilter(smallBitmap, filterType)
            }
            binding.ivFilterPreview.setImageBitmap(preview)

            // Selection state
            if (isSelected) {
                binding.cardFilter.strokeColor = binding.root.context.getColor(R.color.primary)
                binding.tvFilterName.setTextColor(binding.root.context.getColor(R.color.primary_light))
                binding.ivCheck.visibility = android.view.View.VISIBLE
            } else {
                binding.cardFilter.strokeColor = binding.root.context.getColor(R.color.transparent)
                binding.tvFilterName.setTextColor(binding.root.context.getColor(R.color.on_surface_medium))
                binding.ivCheck.visibility = android.view.View.GONE
            }

            binding.root.setOnClickListener {
                if (selectedPosition != bindingAdapterPosition) {
                    val oldPosition = selectedPosition
                    selectedPosition = bindingAdapterPosition
                    notifyItemChanged(oldPosition)
                    notifyItemChanged(selectedPosition)

                    onFilterSelected(filterType)
                }
            }
        }
    }

    fun cleanup() {
        previewCache.values.forEach { if (!it.isRecycled) it.recycle() }
        previewCache.clear()
    }
}
