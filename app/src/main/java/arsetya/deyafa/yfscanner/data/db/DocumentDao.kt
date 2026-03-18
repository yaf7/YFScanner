package arsetya.deyafa.yfscanner.data.db

import androidx.lifecycle.LiveData
import androidx.room.*
import arsetya.deyafa.yfscanner.data.model.ScannedDocument
import arsetya.deyafa.yfscanner.data.model.ScannedPage

@Dao
interface DocumentDao {

    // Document operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: ScannedDocument): Long

    @Update
    suspend fun updateDocument(document: ScannedDocument)

    @Delete
    suspend fun deleteDocument(document: ScannedDocument)

    @Query("SELECT * FROM documents ORDER BY updatedAt DESC")
    fun getAllDocuments(): LiveData<List<ScannedDocument>>

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getDocumentById(id: Long): ScannedDocument?

    @Query("SELECT * FROM documents WHERE title LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun searchDocuments(query: String): LiveData<List<ScannedDocument>>

    @Query("DELETE FROM documents")
    suspend fun deleteAllDocuments()

    @Query("SELECT COUNT(*) FROM documents")
    suspend fun getDocumentCount(): Int

    // Page operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPage(page: ScannedPage): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPages(pages: List<ScannedPage>)

    @Update
    suspend fun updatePage(page: ScannedPage)

    @Delete
    suspend fun deletePage(page: ScannedPage)

    @Query("SELECT * FROM pages WHERE documentId = :documentId ORDER BY pageIndex ASC")
    fun getPagesByDocumentId(documentId: Long): LiveData<List<ScannedPage>>

    @Query("SELECT * FROM pages WHERE documentId = :documentId ORDER BY pageIndex ASC")
    suspend fun getPagesByDocumentIdSync(documentId: Long): List<ScannedPage>

    @Query("SELECT * FROM pages WHERE id = :pageId")
    suspend fun getPageById(pageId: Long): ScannedPage?

    @Query("DELETE FROM pages WHERE documentId = :documentId")
    suspend fun deletePagesByDocumentId(documentId: Long)

    @Query("SELECT COUNT(*) FROM pages WHERE documentId = :documentId")
    suspend fun getPageCount(documentId: Long): Int
}
