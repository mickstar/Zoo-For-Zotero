package com.mickstarify.zooforzotero.SyncSetup

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.mickstarify.zooforzotero.LibraryActivity.LibraryActivity
import com.mickstarify.zooforzotero.SyncSetup.ZoteroAPISetup.ZoteroAPISetup
import com.mickstarify.zooforzotero.SyncSetup.ui.SyncSetupScreen
import com.mickstarify.zooforzotero.ui.theme.ZoteroTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SyncSetupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            ZoteroTheme {
                SyncSetupScreen(
                    onNavigateToZoteroApiSetup = {
                        navigateToZoteroApiSetup()
                    },
                    onNavigateToLibrary = {
                        navigateToLibrary()
                    }
                )
            }
        }
    }

    private fun navigateToZoteroApiSetup() {
        val intent = Intent(this, ZoteroAPISetup::class.java)
        startActivity(intent)
    }

    private fun navigateToLibrary() {
        val intent = Intent(this, LibraryActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onResume() {
        super.onResume()
        // Check if sync is already set up and finish if it is
        val auth = AuthenticationStorage(this)
        if (auth.hasCredentials()) {
            navigateToLibrary()
        }
    }
}