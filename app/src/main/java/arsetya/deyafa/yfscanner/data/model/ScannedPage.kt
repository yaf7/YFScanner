package arsetya.deyafa.yfscanner.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pages",
    foreignKeys = [
        ForeignKey(
            entity = ScannedDocument::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("documentId")]
)
data class ScannedPage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val documentId: Long,
    var pageIndex: Int,
    var originalImagePath: String,
    var processedImagePath: String? = null,
    var filterApplied: String = "original"
)
