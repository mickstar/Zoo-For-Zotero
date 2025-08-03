package com.mickstarify.zooforzotero.AttachmentManager.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mickstarify.zooforzotero.AttachmentManager.viewmodel.AttachmentManagerViewModel
import com.mickstarify.zooforzotero.ui.theme.ZoteroTheme


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentManagerScreen(
    state: AttachmentManagerViewModel.State,
    dispatch: (AttachmentManagerViewModel.Event) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Attachment Manager") },
                navigationIcon = {
                    IconButton(onClick = { dispatch(AttachmentManagerViewModel.Event.NavigateBack) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            
            // Loading Section
            if (state.isLoading) {
                CircularProgressIndicator()
                Text("Loading attachments...")
            }
            
            // Attachment List
            if (state.attachments.isNotEmpty()) {
                Text(
                    text = "Attachments (${state.attachments.size})",
                    style = MaterialTheme.typography.headlineSmall
                )
                
                state.attachments.forEach { attachment ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        onClick = {
                            dispatch(AttachmentManagerViewModel.Event.SelectAttachment(attachment))
                        }
                    ) {
                        Text(
                            text = attachment,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Refresh Button
            Button(
                onClick = {
                    dispatch(AttachmentManagerViewModel.Event.RefreshAttachments)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            ) {
                Text("Refresh Attachments")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    
    // Error Dialog
    state.error?.let { error ->
        AlertDialog(
            onDismissRequest = { /* Handle error dismiss */ },
            title = { Text("Error") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { /* Handle error dismiss */ }) {
                    Text("OK")
                }
            },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AttachmentManagerScreenPreview() {
    ZoteroTheme {
        AttachmentManagerScreen(
            state = AttachmentManagerViewModel.State(
                attachments = listOf("attachment1.pdf", "attachment2.doc", "attachment3.jpg")
            ),
            dispatch = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AttachmentManagerScreenLoadingPreview() {
    ZoteroTheme {
        AttachmentManagerScreen(
            state = AttachmentManagerViewModel.State(
                isLoading = true
            ),
            dispatch = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AttachmentManagerScreenErrorPreview() {
    ZoteroTheme {
        AttachmentManagerScreen(
            state = AttachmentManagerViewModel.State(
                error = "Failed to load attachments"
            ),
            dispatch = {}
        )
    }
}