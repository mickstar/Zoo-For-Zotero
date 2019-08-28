package com.mickstarify.zooforzotero

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mickstarify.zooforzotero.LibraryActivity.LibraryActivity
import com.mickstarify.zooforzotero.SyncSetup.AuthenticationStorage
import com.mickstarify.zooforzotero.SyncSetup.SyncSetupView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val auth = AuthenticationStorage(this)
        val intent = if (auth.hasCredentials()) {
            Intent(this, LibraryActivity::class.java)
        } else {
            Intent(this, SyncSetupView::class.java)
        }
        startActivity(intent)
        overridePendingTransition(0, 0)
        finish()
    }
}
