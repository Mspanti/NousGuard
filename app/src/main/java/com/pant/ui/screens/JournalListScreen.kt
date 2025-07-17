package pant.com.nousguard.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pant.com.nousguard.data.database.entities.JournalEntry
import pant.com.nousguard.ui.viewmodels.JournalViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalListScreen(journalViewModel: JournalViewModel) {
    // Collect the UI state from the ViewModel
    val uiState by journalViewModel.uiState.collectAsState()
    val context = LocalContext.current

    // State for showing the Add/Edit dialog
    var showAddEditDialog by remember { mutableStateOf(false) } // Renamed for clarity
    var entryToEdit by remember { mutableStateOf<JournalEntry?>(null) } // Null for add, entry for edit

    // New states for deletion confirmation
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var entryToDelete by remember { mutableStateOf<JournalEntry?>(null) } // To hold the entry to be deleted


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NousGuard Journal") },
                colors = topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                entryToEdit = null // Ensure it's for adding new entry
                showAddEditDialog = true // Use new state variable
            }) {
                Icon(Icons.Filled.Add, "Add new journal entry")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.wrapContentSize())
                    Text("Loading entries...", modifier = Modifier.padding(16.dp))
                }
                uiState.errorMessage != null -> {
                    Text("Error: ${uiState.errorMessage}", color = MaterialTheme.colorScheme.error)
                }
                uiState.allEntries.isEmpty() -> {
                    Text(
                        "No journal entries yet. Click '+' to add one!",
                        modifier = Modifier.padding(16.dp)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(uiState.allEntries) { entry ->
                            JournalEntryCard(
                                entry = entry,
                                onEditClick = {
                                    entryToEdit = it
                                    showAddEditDialog = true // Use new state variable
                                },
                                onDeleteClick = {
                                    // When delete is clicked, set the entry and show confirmation dialog
                                    entryToDelete = it
                                    showDeleteConfirmationDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Show the Add/Edit dialog when showAddEditDialog is true
    if (showAddEditDialog) { // Use new state variable
        AddEditJournalDialog(
            entryToEdit = entryToEdit,
            onDismiss = { showAddEditDialog = false }, // Use new state variable
            onConfirm = { title, content ->
                if (entryToEdit == null) {
                    journalViewModel.addJournalEntry(title, content)
                    Toast.makeText(context, "Entry added!", Toast.LENGTH_SHORT).show()
                } else {
                    journalViewModel.updateJournalEntry(entryToEdit!!, title, content)
                    Toast.makeText(context, "Entry updated!", Toast.LENGTH_SHORT).show()
                }
                showAddEditDialog = false // Use new state variable
            }
        )
    }

    // Show the Delete Confirmation dialog when showDeleteConfirmationDialog is true
    if (showDeleteConfirmationDialog) {
        DeleteConfirmationDialog(
            onConfirm = {
                entryToDelete?.let { entry ->
                    journalViewModel.deleteJournalEntry(entry)
                    Toast.makeText(context, "Entry deleted!", Toast.LENGTH_SHORT).show()
                }
                showDeleteConfirmationDialog = false // Hide dialog after action
                entryToDelete = null // Clear entryToDelete
            },
            onDismiss = {
                showDeleteConfirmationDialog = false // Hide dialog if dismissed
                entryToDelete = null // Clear entryToDelete
            }
        )
    }
}

@Composable
fun JournalEntryCard(
    entry: JournalEntry,
    onEditClick: (JournalEntry) -> Unit,
    onDeleteClick: (JournalEntry) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .clickable { onEditClick(entry) } // Make card clickable for editing
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = entry.encryptedTitle, // This will show decrypted title
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = entry.encryptedContent, // This will show decrypted content
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3, // Show only a few lines in the list view
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(
                    text = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(entry.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                Row {
                    IconButton(onClick = { onEditClick(entry) }) {
                        Icon(Icons.Filled.Edit, "Edit Entry", tint = MaterialTheme.colorScheme.secondary)
                    }
                    IconButton(onClick = { onDeleteClick(entry) }) {
                        Icon(Icons.Filled.Delete, "Delete Entry", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}


@Composable
fun AddEditJournalDialog(
    entryToEdit: JournalEntry?,
    onDismiss: () -> Unit,
    onConfirm: (title: String, content: String) -> Unit
) {
    var title by remember { mutableStateOf(entryToEdit?.encryptedTitle ?: "") }
    var content by remember { mutableStateOf(entryToEdit?.encryptedContent ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (entryToEdit == null) "Add New Entry" else "Edit Entry") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Content") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 5
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, content) },
                enabled = title.isNotBlank() && content.isNotBlank() // Enable button only if fields are not blank
            ) {
                Text(if (entryToEdit == null) "Add" else "Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm Deletion") },
        text = { Text("Are you sure you want to delete this entry? This action cannot be undone.") },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}