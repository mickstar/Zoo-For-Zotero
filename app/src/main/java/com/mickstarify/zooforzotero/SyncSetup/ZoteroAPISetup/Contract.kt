package com.mickstarify.zooforzotero.SyncSetup.ZoteroAPISetup

import android.net.Uri
import com.mickstarify.zooforzotero.SyncSetup.AuthenticationStorage

interface Contract {
    interface View {
        fun loadURL(url: String)
        fun makeErrorAlert(title: String, message: String)
        fun startLoadingAnimation()
        fun stopLoadingAnimation()
        fun openLibraryView()

        fun showErrorScreen()
    }

    interface Presenter {
        fun loadAuthorizationURL(authorizationURL: String)
        fun handleOAuthCallback(uri: Uri?)
        fun openLibraryView()
        fun showError(message: String)
    }

    interface Model {
        fun establishAPIConnection()
        fun handleOAuthCallback(
            oauth_token: String,
            oauth_verifier: String,
            authenticationStorage: AuthenticationStorage
        )

    }
}