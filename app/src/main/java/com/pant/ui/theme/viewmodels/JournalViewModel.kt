package pant.com.nousguard.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pant.com.nousguard.data.JournalRepository
import pant.com.nousguard.data.database.entities.JournalEntry

// State class to represent the UI state for journal entries
data class JournalUiState(
    val allEntries: List<JournalEntry> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class JournalViewModel(private val repository: JournalRepository) : ViewModel() {

    // Expose UI state as StateFlow
    val uiState: StateFlow<JournalUiState> =
        repository.getAllAndDecryptJournalEntries()
            .map { entries ->
                Log.d("JournalViewModel", "Entries received: ${entries.size}")
                JournalUiState(allEntries = entries, isLoading = false, errorMessage = null)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000), // Keep active for 5 seconds after last subscriber
                initialValue = JournalUiState(isLoading = true) // Initial loading state
            )

    // Function to add a new journal entry
    fun addJournalEntry(title: String, content: String) {
        viewModelScope.launch {
            try {
                repository.insertJournalEntry(title, content)
                Log.d("JournalViewModel", "Attempted to add entry: $title")
            } catch (e: Exception) {
                Log.e("JournalViewModel", "Failed to add journal entry: ${e.message}", e)
                // Update UI state with error message, or show a Toast
                // For simplicity, we just log here. You might want to update uiState.errorMessage
            }
        }
    }

    // Function to update an existing journal entry
    fun updateJournalEntry(entry: JournalEntry, newTitle: String, newContent: String) {
        viewModelScope.launch {
            try {
                repository.updateJournalEntry(entry, newTitle, newContent)
                Log.d("JournalViewModel", "Attempted to update entry: ${entry.id}")
            } catch (e: Exception) {
                Log.e("JournalViewModel", "Failed to update journal entry: ${e.message}", e)
            }
        }
    }

    // Function to delete a journal entry
    fun deleteJournalEntry(entry: JournalEntry) {
        viewModelScope.launch {
            try {
                repository.deleteJournalEntry(entry)
                Log.d("JournalViewModel", "Attempted to delete entry: ${entry.id}")
            } catch (e: Exception) {
                Log.e("JournalViewModel", "Failed to delete journal entry: ${e.message}", e)
            }
        }
    }

    // Factory to create ViewModel instance with required dependencies
    companion object {
        fun provideFactory(repository: JournalRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(JournalViewModel::class.java)) {
                        return JournalViewModel(repository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }
    }
}