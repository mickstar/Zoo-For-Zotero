package com.mickstarify.zooforzotero.AttachmentManager

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import com.mickstarify.zooforzotero.AttachmentManager.ui.AttachmentManagerScreen
import com.mickstarify.zooforzotero.AttachmentManager.viewmodel.AttachmentManagerViewModel
import com.mickstarify.zooforzotero.common.provides
import com.mickstarify.zooforzotero.ui.theme.ZoteroTheme
import dagger.hilt.android.AndroidEntryPoint

private const val TAG = "AttachmentManager"

@AndroidEntryPoint
class AttachmentManager : ComponentActivity() {
    
    private val viewModel: AttachmentManagerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        Log.d(TAG, "AttachmentManager created")

        setContent {
            ZoteroTheme {

                val (state, effect, dispatch) = viewModel.provides()

//                LaunchedEffect(Unit) {
//                    dispatch(AttachmentManagerViewModel.Event.LoadAttachments)
//                }

                LaunchedEffect(Unit) {
                    effect.collect { effect ->
                        when (effect) {
                            is AttachmentManagerViewModel.Effect.NavigateBack -> {
                                Log.d(TAG, "Navigate back effect received")
                                finish()
                            }
                            is AttachmentManagerViewModel.Effect.ShowError -> {
                                Log.d(TAG, "Show error effect: ${effect.message}")
                            }
                            is AttachmentManagerViewModel.Effect.OpenAttachment -> {
                                Log.d(TAG, "Open attachment effect: ${effect.attachmentPath}")
                            }
                        }
                    }
                }

                AttachmentManagerScreen(
                    state = state,
                    dispatch = { event ->
                        Log.d(TAG, "Event dispatched: $event")
                        dispatch(event)
                    }
                )
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        Log.d(TAG, "Back pressed")
        viewModel.dispatchEvent(AttachmentManagerViewModel.Event.NavigateBack)
    }
}