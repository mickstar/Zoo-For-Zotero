package com.mickstarify.zooforzotero.SyncSetup.ui.components

import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.webkit.URLUtil
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.mickstarify.zooforzotero.ui.theme.ZoteroTheme

private const val TAG = "ZoteroOAuthWebView"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ZoteroOAuthWebView(
    url: String,
    onOAuthCallback: (String, String) -> Unit,
    onPageFinished: () -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val webView = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
        }
    }

    DisposableEffect(url) {
        onDispose {
            webView.destroy()
        }
    }

    AndroidView(
        factory = { webView },
        modifier = modifier.fillMaxSize(),
        update = { view ->
            view.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    Log.d(TAG, "shouldOverrideUrlLoading: $url")

                    url?.let { urlString ->
                        // Check for OAuth callback URL
                        if (urlString.startsWith("zooforzotero://oauth_callback")) {
                            val uri = Uri.parse(urlString)
                            val oauthToken = uri.getQueryParameter("oauth_token")
                            val oauthVerifier = uri.getQueryParameter("oauth_verifier")

                            Log.d(
                                TAG,
                                "OAuth callback - token: $oauthToken, verifier: $oauthVerifier"
                            )

                            if (oauthToken != null && oauthVerifier != null) {
                                onOAuthCallback(oauthToken, oauthVerifier)
                            } else {
                                onError("Failed to retrieve OAuth parameters from callback")
                            }
                            return true
                        }

                        // Allow network URLs to be loaded normally
                        if (URLUtil.isNetworkUrl(urlString)) {
                            return false
                        }
                    }

                    return super.shouldOverrideUrlLoading(view, url)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    Log.d(TAG, "onPageFinished: $url")
                    onPageFinished()
                }

                override fun onReceivedError(
                    view: WebView?,
                    errorCode: Int,
                    description: String?,
                    failingUrl: String?
                ) {
                    super.onReceivedError(view, errorCode, description, failingUrl)
                    Log.e(TAG, "WebView error $errorCode: $description for URL: $failingUrl")
                    onError("Connection error: $description")
                }

                override fun onPageStarted(
                    view: WebView?,
                    url: String?,
                    favicon: android.graphics.Bitmap?
                ) {
                    super.onPageStarted(view, url, favicon)
                    Log.d(TAG, "onPageStarted: $url")
                }
            }

            // Only load URL if it's different from current URL to avoid reloading
            if (view.url != url) {
                view.loadUrl(url)
            }
        }
    )
}

@Preview
@Composable
private fun ZoteroOAuthWebViewPreview() {
    ZoteroTheme {
        ZoteroOAuthWebView(
            url = "https://www.zotero.org/oauth/authorize",
            onOAuthCallback = { _, _ -> },
            onPageFinished = { },
            onError = { }
        )
    }
}