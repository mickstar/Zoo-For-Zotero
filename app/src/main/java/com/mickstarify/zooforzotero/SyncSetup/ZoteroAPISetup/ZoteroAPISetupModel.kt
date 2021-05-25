package com.mickstarify.zooforzotero.SyncSetup.ZoteroAPISetup

import android.annotation.SuppressLint
import android.util.Log
import com.mickstarify.zooforzotero.BuildConfig
import com.mickstarify.zooforzotero.SyncSetup.AuthenticationStorage
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider

class ZoteroAPISetupModel(val presenter: Contract.Presenter) : Contract.Model {
    private val API_URL = "https://api.zotero.org"
    private val REQUEST_TOKEN_ENDPOINT = "https://www.zotero.org/oauth/request?" +
            "library_access=1&" +
            "notes_access=1&" +
            "write_access=1&" +
            "all_groups=write"

    private val ACCESS_TOKEN_ENDPOINT = "https://www.zotero.org/oauth/access?" +
            "library_access=1&" +
            "notes_access=1&" +
            "write_access=1&" +
            "all_groups=write"

    private val ZOTERO_AUTHORIZE_ENDPOINT = "https://www.zotero.org/oauth/authorize?" +
            "library_access=1&" +
            "notes_access=1&" +
            "write_access=1&" +
            "all_groups=write"

    // You can generate these two keys on the Zotero API website.
    // Put them in a file called apikeys.properties
    /*  PROJECT_ROOT/apikeys.properties
        zotero_api_key="insert_key"
        zotero_api_secret="insert_key"
    *
    * */
    private val client_key = BuildConfig.zotero_api_key
    private val client_secret = BuildConfig.zotero_api_secret

    val OAuthProvider = CommonsHttpOAuthProvider(
        REQUEST_TOKEN_ENDPOINT,
        ACCESS_TOKEN_ENDPOINT,
        ZOTERO_AUTHORIZE_ENDPOINT
    )

    val OAuthConsumer = CommonsHttpOAuthConsumer(
        client_key,
        client_secret
    )

    override fun establishAPIConnection() {
        val d = Observable.fromCallable {
            OAuthProvider.retrieveRequestToken(OAuthConsumer, "zooforzotero://oauth_callback")
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<String> {
                override fun onSubscribe(d: Disposable) {
                }

                override fun onNext(requestURL: String) {
                    Log.d("zotero", "loading URL $requestURL")
                    presenter.loadAuthorizationURL(requestURL)
                }

                override fun onError(e: Throwable) {
                    presenter.showError("Error connecting to Zotero's server ${e.message}")
                }

                override fun onComplete() {
                }

            })
    }


    @SuppressLint("CheckResult")
    override fun handleOAuthCallback(
        oauth_token: String,
        oauth_verifier: String,
        authenticationStorage: AuthenticationStorage
    ) {
        Completable.fromAction {
            OAuthProvider.retrieveAccessToken(OAuthConsumer, oauth_verifier)
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                val params = OAuthProvider.responseParameters
                Log.d("zotero", "userID ${params.getFirst("userID")}")
                val username = params.getFirst("username")
                val userID = params.getFirst("userID")
                val userkey = OAuthConsumer.token

                if (BuildConfig.DEBUG) {
                    Log.d("zotero", "params ${params.toList()}")
                    Log.d("zotero", "userkey ${OAuthConsumer.token}")
                    Log.d("zotero", "user secret ${OAuthConsumer.tokenSecret}")
                }
                authenticationStorage.setCredentials(username, userID, userkey)
                presenter.openLibraryView()
            }

    }
}
