package arsetya.deyafa.yfscanner.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import arsetya.deyafa.yfscanner.data.model.ScannedDocument
import arsetya.deyafa.yfscanner.data.repository.DocumentRepository
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DocumentRepository(application)

    private val _searchQuery = MutableLiveData<String>("")

    val documents: LiveData<List<ScannedDocument>> = _searchQuery.switchMap { query ->
        if (query.isNullOrEmpty()) {
            repository.getAllDocuments()
        } else {
            repository.searchDocuments(query)
        }
    }

    fun searchDocuments(query: String) {
        _searchQuery.value = query
    }

    fun deleteDocument(document: ScannedDocument) {
        viewModelScope.launch {
            repository.deleteDocument(document)
        }
    }
}
