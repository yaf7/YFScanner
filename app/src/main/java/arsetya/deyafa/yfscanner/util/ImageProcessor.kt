package arsetya.deyafa.yfscanner.util

import android.graphics.*

object ImageProcessor {

    enum class FilterType {
        ORIGINAL,
        GRAYSCALE, // Standard Grayscale
        GRAYSCALE_DOC, // High contrast Grayscale for documents
        MAGIC_COLOR, // Enhances colors slightly
        COLOR_DOC, // High contrast colors for documents
        BLACK_WHITE, // Standard B&W
        BW_DOC, // Sharp high contrast B&W for text readability
        LIGHTEN,
        DARKEN,
        SHARPEN
    }

    fun applyFilter(bitmap: Bitmap, filterType: FilterType): Bitmap {
        return when (filterType) {
            FilterType.ORIGINAL -> bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
            FilterType.GRAYSCALE -> applyGrayscale(bitmap, contrast = 1f)
            FilterType.GRAYSCALE_DOC -> applyGrayscale(bitmap, contrast = 1.5f)
            FilterType.MAGIC_COLOR -> applyColorEnhance(bitmap, contrast = 1.2f, saturation = 1.3f)
            FilterType.COLOR_DOC -> applyColorEnhance(bitmap, contrast = 1.8f, saturation = 1.5f)
            FilterType.BLACK_WHITE -> applyBlackWhite(bitmap, threshold = 128f)
            FilterType.BW_DOC -> applyBlackWhite(bitmap, threshold = 160f) // Harsher threshold for cleaner text
            FilterType.LIGHTEN -> applyLighten(bitmap)
            FilterType.DARKEN -> applyDarken(bitmap)
            FilterType.SHARPEN -> applySharpen(bitmap)
        }
    }

    private fun applyGrayscale(bitmap: Bitmap, contrast: Float): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()
        
        val colorMatrix = ColorMatrix().apply {
            setSaturation(0f)
        }

        if (contrast != 1f) {
            val scale = contrast
            val translate = (-.5f * scale + .5f) * 255f
            val contrastMatrix = ColorMatrix(floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            ))
            contrastMatrix.postConcat(colorMatrix)
            paint.colorFilter = ColorMatrixColorFilter(contrastMatrix)
        } else {
            paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        }
        
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return result
    }

    private fun applyColorEnhance(bitmap: Bitmap, contrast: Float, saturation: Float): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()

        // Contrast adjustment
        val translate = (-.5f * contrast + .5f) * 255f
        val contrastMatrix = ColorMatrix(floatArrayOf(
            contrast, 0f, 0f, 0f, translate,
            0f, contrast, 0f, 0f, translate,
            0f, 0f, contrast, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        ))

        // Saturation adjustment
        val saturationMatrix = ColorMatrix().apply {
            setSaturation(saturation)
        }

        contrastMatrix.postConcat(saturationMatrix)
        paint.colorFilter = ColorMatrixColorFilter(contrastMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return result
    }

    private fun applyBlackWhite(bitmap: Bitmap, threshold: Float): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()

        // High contrast black and white threshold conversion
        // Uses luminosity and forces pixels to absolute black or white
        val m = 255f
        val t = -threshold * 255f
        
        val thresholdMatrix = ColorMatrix(floatArrayOf(
            m, m, m, 0f, t,
            m, m, m, 0f, t,
            m, m, m, 0f, t,
            0f, 0f, 0f, 1f, 0f
        ))

        paint.colorFilter = ColorMatrixColorFilter(thresholdMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return result
    }

    private fun applyLighten(bitmap: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()

        val colorMatrix = ColorMatrix(floatArrayOf(
            1.2f, 0f, 0f, 0f, 30f,
            0f, 1.2f, 0f, 0f, 30f,
            0f, 0f, 1.2f, 0f, 30f,
            0f, 0f, 0f, 1f, 0f
        ))

        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return result
    }

    private fun applyDarken(bitmap: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()

        val colorMatrix = ColorMatrix(floatArrayOf(
            0.8f, 0f, 0f, 0f, -20f,
            0f, 0.8f, 0f, 0f, -20f,
            0f, 0f, 0.8f, 0f, -20f,
            0f, 0f, 0f, 1f, 0f
        ))

        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return result
    }

    private fun applySharpen(bitmap: Bitmap): Bitmap {
        // Enhanced contrast for sharpening effect without convolutions
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()

        val colorMatrix = ColorMatrix(floatArrayOf(
            1.3f, 0f, 0f, 0f, -25f,
            0f, 1.3f, 0f, 0f, -25f,
            0f, 0f, 1.3f, 0f, -25f,
            0f, 0f, 0f, 1f, 0f
        ))

        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return result
    }

    fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply {
            postRotate(degrees)
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun getFilterName(filterType: FilterType): String {
        return when (filterType) {
            FilterType.ORIGINAL -> "Original"
            FilterType.GRAYSCALE -> "Grayscale"
            FilterType.GRAYSCALE_DOC -> "B&W Doc"
            FilterType.MAGIC_COLOR -> "Magic Color"
            FilterType.COLOR_DOC -> "Color Doc"
            FilterType.BLACK_WHITE -> "Hard B&W"
            FilterType.BW_DOC -> "Clean Text"
            FilterType.LIGHTEN -> "Lighten"
            FilterType.DARKEN -> "Darken"
            FilterType.SHARPEN -> "Sharpen"
        }
    }
}
