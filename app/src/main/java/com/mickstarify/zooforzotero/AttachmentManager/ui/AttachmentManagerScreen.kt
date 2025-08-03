package com.mickstarify.zooforzotero.AttachmentManager.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mickstarify.zooforzotero.AttachmentManager.ui.components.AttachmentStatsCard
import com.mickstarify.zooforzotero.AttachmentManager.ui.components.LoadingSection
import com.mickstarify.zooforzotero.AttachmentManager.viewmodel.AttachmentManagerViewModel
import com.mickstarify.zooforzotero.AttachmentManager.viewmodel.AttachmentStats
import com.mickstarify.zooforzotero.AttachmentManager.viewmodel.DownloadProgress
import com.mickstarify.zooforzotero.ui.theme.ZoteroTheme


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentManagerScreen(
    state: AttachmentManagerViewModel.State,
    dispatch: (AttachmentManagerViewModel.Event) -> Unit
) {
    LaunchedEffect(Unit) {
        dispatch(AttachmentManagerViewModel.Event.LoadLibrary)
    }
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
            
            when (state) {
                is AttachmentManagerViewModel.State.InitialState -> {
                    // Initial state - show nothing or placeholder
                }
                
                is AttachmentManagerViewModel.State.LoadingState -> {
                    LoadingSection(
                        message = "Loading your library...",
                        progress = DownloadProgress(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                is AttachmentManagerViewModel.State.LoadedState -> {
                    // Show download progress if downloading
                    if (state.isDownloading) {
                        LoadingSection(
                            message = "Downloading attachments...",
                            progress = state.downloadProgress,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        // Attachment Stats Card
                        state.attachmentStats?.let { stats ->
                            AttachmentStatsCard(
                                stats = stats,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // Download/Cancel Button
                    if (state.isDownloading) {
                        OutlinedButton(
                            onClick = {
                                dispatch(AttachmentManagerViewModel.Event.CancelDownload)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Cancel,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cancel Download")
                        }
                    } else {
                        Button(
                            onClick = {
                                dispatch(AttachmentManagerViewModel.Event.DownloadButtonPressed)
                            },
                            enabled = state.downloadButtonEnabled,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(state.downloadButtonText)
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
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AttachmentManagerScreenPreview() {
    ZoteroTheme {
        AttachmentManagerScreen(
            state = AttachmentManagerViewModel.State.LoadedState(
                attachmentStats = AttachmentStats(
                    localCount = 12,
                    totalCount = 25,
                    localSize = "45.6MB"
                ),
                downloadButtonEnabled = true
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
            state = AttachmentManagerViewModel.State.LoadingState,
            dispatch = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AttachmentManagerScreenCalculatingPreview() {
    ZoteroTheme {
        AttachmentManagerScreen(
            state = AttachmentManagerViewModel.State.LoadedState(
                attachmentStats = AttachmentStats(
                    localCount = 8,
                    totalCount = 25,
                    localSize = "32.1MB"
                ),
                downloadButtonText = "Loading...",
                downloadButtonEnabled = false
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
            state = AttachmentManagerViewModel.State.LoadedState(
                error = "No credentials available. Please re-authenticate."
            ),
            dispatch = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AttachmentManagerScreenInitialPreview() {
    ZoteroTheme {
        AttachmentManagerScreen(
            state = AttachmentManagerViewModel.State.InitialState,
            dispatch = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AttachmentManagerScreenDownloadingPreview() {
    ZoteroTheme {
        AttachmentManagerScreen(
            state = AttachmentManagerViewModel.State.LoadedState(
                attachmentStats = AttachmentStats(
                    localCount = 8,
                    totalCount = 25,
                    localSize = "32.1MB"
                ),
                isDownloading = true,
                downloadButtonText = "Downloading...",
                downloadButtonEnabled = false,
                downloadProgress = DownloadProgress(
                    current = 15,
                    total = 25,
                    fileName = "research_paper.pdf",
                    isIndeterminate = false
                )
            ),
            dispatch = {}
        )
    }
}