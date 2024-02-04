package com.mickstarify.zooforzotero.SyncSetup.ZoteroAPISetup

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.webkit.URLUtil
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.mickstarify.zooforzotero.LibraryActivity.LibraryActivity
import com.mickstarify.zooforzotero.R
import com.mickstarify.zooforzotero.SyncSetup.AuthenticationStorage


class ZoteroAPISetup : AppCompatActivity(), Contract.View {
    override fun openLibraryView() {
        val intent = Intent(this, LibraryActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun startLoadingAnimation() {
        val progressLayout = findViewById<ConstraintLayout>(R.id.constraintLayout_progressdialog)
        progressLayout.visibility = View.VISIBLE
    }

    override fun stopLoadingAnimation() {
        val progressLayout = findViewById<ConstraintLayout>(R.id.constraintLayout_progressdialog)
        progressLayout.visibility = View.GONE
    }

    override fun makeErrorAlert(title: String, message: String) {
        val alert = AlertDialog.Builder(this)
        alert.setIcon(R.drawable.ic_error_black_24dp)
        alert.setTitle(title)
        alert.setMessage(message)
        alert.setPositiveButton("Ok") { _, _ -> }
        alert.show()
    }

    override fun loadURL(url: String) {
        val webView = findViewById<WebView>(R.id.webview_zotero_API_setup)

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                Log.d(packageName, "got url $url")
                if (URLUtil.isNetworkUrl(url)) {
                    Log.d(packageName, "is network url $url")
                    return false
                }
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
                return true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                stopLoadingAnimation()
            }
        }
        webView.loadUrl(url)
    }

    private lateinit var presenter: Contract.Presenter


    override fun applyOverrideConfiguration(overrideConfiguration: Configuration) {
        /* This method is meant to fix a bug in webview which causes a crash on old devices
        * Source: https://stackoverflow.com/questions/41025200/android-view-inflateexception-error-inflating-class-android-webkit-webview*/
        if (Build.VERSION.SDK_INT >= 21 && Build.VERSION.SDK_INT < 25) {
            overrideConfiguration.uiMode =
                overrideConfiguration.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()
        }
        super.applyOverrideConfiguration(overrideConfiguration)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_zotero_api_setup)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        presenter = ZoteroAPISetupPresenter(this, AuthenticationStorage(this))

        val textView_zoteroUrl = findViewById<TextView>(R.id.textView_hint_zotero_keys)
        textView_zoteroUrl.setOnClickListener {
            val url = "https://www.zotero.org/settings/keys"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            startActivity(intent)
        }

        // bad code i know, but we have two of these links and i dont want to make a new layout for it.
        val textView_zoteroUrl2 = findViewById<TextView>(R.id.textView_hint_zotero_keys2)
        textView_zoteroUrl2.setOnClickListener {
            val url = "https://www.zotero.org/settings/keys"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            startActivity(intent)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleZoteroOAuthIntent(intent)
    }

    private fun handleZoteroOAuthIntent(intent: Intent?) {
        if (intent != null) {
            val uri = intent.data
            Log.d(this.packageName, "got intent $uri")
            Log.d(packageName, "token ${uri?.getQueryParameter("oauth_token")}")
            Log.d(packageName, "verifier ${uri?.getQueryParameter("oauth_verifier")}")
            presenter.handleOAuthCallback(uri)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            android.R.id.home -> {
                finish()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun showErrorScreen() {
        val errorScreen = findViewById<ConstraintLayout>(R.id.constraintLayout_failed_zotero_api_setup)
        errorScreen.visibility = View.VISIBLE

        val loadingScreen = findViewById<ConstraintLayout>(R.id.constraintLayout_progressdialog)
        loadingScreen.visibility = View.GONE

        val buttonReturn = findViewById<TextView>(R.id.button_go_back)
        buttonReturn.setOnClickListener {
            finish()
        }
    }
}
