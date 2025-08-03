package com.mickstarify.zooforzotero.AttachmentManager.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mickstarify.zooforzotero.ui.theme.ZoteroTheme

@Composable
fun DownloadButton(
    isDownloading: Boolean,
    isEnabled: Boolean,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonColors = if (isDownloading) {
        ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError
        )
    } else {
        ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    }
    
    val buttonText = if (isDownloading) "Cancel Download" else "Download All Attachments"
    val buttonIcon = if (isDownloading) Icons.Filled.Cancel else Icons.Filled.CloudDownload
    
    Button(
        onClick = if (isDownloading) onCancel else onDownload,
        enabled = isEnabled,
        modifier = modifier.height(56.dp),
        colors = buttonColors,
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = if (isDownloading) 8.dp else 6.dp
        )
    ) {
        AnimatedContent(
            targetState = isDownloading,
            transitionSpec = {
                slideInHorizontally { width -> width } + fadeIn() togetherWith
                slideOutHorizontally { width -> -width } + fadeOut()
            },
            label = "button_content_animation"
        ) { downloading ->
            ButtonContent(
                icon = buttonIcon,
                text = buttonText,
                isDownloading = downloading
            )
        }
    }
}

@Composable
private fun ButtonContent(
    icon: ImageVector,
    text: String,
    isDownloading: Boolean
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Animated icon rotation for downloading state
        val rotation by if (isDownloading) {
            val infiniteTransition = rememberInfiniteTransition(label = "button_icon_rotation")
            infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "icon_rotation"
            )
        } else {
            remember { mutableFloatStateOf(0f) }
        }
        
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer(rotationZ = rotation)
        )
        
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DownloadButtonPreview() {
    ZoteroTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DownloadButton(
                isDownloading = false,
                isEnabled = true,
                onDownload = {},
                onCancel = {},
                modifier = Modifier.fillMaxWidth()
            )
            
            DownloadButton(
                isDownloading = true,
                isEnabled = true,
                onDownload = {},
                onCancel = {},
                modifier = Modifier.fillMaxWidth()
            )
            
            DownloadButton(
                isDownloading = false,
                isEnabled = false,
                onDownload = {},
                onCancel = {},
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}