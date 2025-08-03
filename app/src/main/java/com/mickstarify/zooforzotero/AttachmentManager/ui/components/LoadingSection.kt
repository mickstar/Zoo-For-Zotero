package com.mickstarify.zooforzotero.AttachmentManager.ui.components

import androidx.compose.animation.core.*
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mickstarify.zooforzotero.AttachmentManager.viewmodel.DownloadProgress
import com.mickstarify.zooforzotero.ui.theme.ZoteroTheme

@Composable
fun LoadingSection(
    message: String,
    progress: DownloadProgress,
    modifier: Modifier = Modifier
) {
    // Animated alpha for smooth appearance
    val infiniteTransition = rememberInfiniteTransition(label = "loading_alpha")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "loading_alpha_animation"
    )
    
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Loading message
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(if (progress.isIndeterminate) alpha else 1f)
            )
            
            // Progress indicator
            if (progress.isIndeterminate) {
                CircularProgressIndicator(
                    modifier = Modifier.size(40.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 4.dp
                )
            } else {
                // Animated progress values
                val targetProgress = if (progress.total > 0) {
                    progress.current.toFloat() / progress.total.toFloat()
                } else 0f
                
                val animatedProgress by animateFloatAsState(
                    targetValue = targetProgress,
                    animationSpec = tween(
                        durationMillis = 300,
                        easing = EaseOutCubic
                    ),
                    label = "progress_animation"
                )
                
                val targetPercentage = if (progress.total > 0) {
                    (progress.current * 100 / progress.total)
                } else 0
                
                val animatedPercentage by animateIntAsState(
                    targetValue = targetPercentage,
                    animationSpec = tween(
                        durationMillis = 300,
                        easing = EaseOutCubic
                    ),
                    label = "percentage_animation"
                )
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    )
                    
                    // Progress text
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${progress.current} of ${progress.total}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        
                        Text(
                            text = "$animatedPercentage%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    // Current file name (if available) with animation
                    Crossfade(
                        targetState = progress.fileName,
                        animationSpec = tween(
                            durationMillis = 250,
                            easing = EaseInOut
                        ),
                        label = "filename_crossfade",
                        modifier = Modifier.animateContentSize()
                    ) { fileName ->
                        if (fileName.isNotEmpty()) {
                            Text(
                                text = "Downloading: $fileName",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoadingSectionIndeterminatePreview() {
    ZoteroTheme {
        LoadingSection(
            message = "Loading your library...",
            progress = DownloadProgress(
                isIndeterminate = true
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LoadingSectionProgressPreview() {
    ZoteroTheme {
        LoadingSection(
            message = "Downloading attachments...",
            progress = DownloadProgress(
                current = 15,
                total = 48,
                fileName = "research_paper.pdf",
                isIndeterminate = false
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LoadingSectionProgressNoFilePreview() {
    ZoteroTheme {
        LoadingSection(
            message = "Downloading attachments...",
            progress = DownloadProgress(
                current = 30,
                total = 100,
                fileName = "",
                isIndeterminate = false
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}