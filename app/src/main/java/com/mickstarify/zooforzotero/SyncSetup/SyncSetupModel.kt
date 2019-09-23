package com.mickstarify.zooforzotero.SyncSetup


import android.content.Context
import android.content.Intent
import com.mickstarify.zooforzotero.BuildConfig
import com.mickstarify.zooforzotero.LibraryActivity.LibraryActivity
import com.mickstarify.zooforzotero.ZoteroAPI.BASE_URL
import com.mickstarify.zooforzotero.ZoteroAPI.Model.KeyInfo
import com.mickstarify.zooforzotero.ZoteroAPI.ZoteroAPIService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SyncSetupModel(val presenter: SyncSetupPresenter, val context: Context) :
    SyncSetupContract.Model {
    override fun testAPIKey(apiKey: String) {
        if (apiKey.trim() == "") {
            return
        }
        val httpClient = OkHttpClient().newBuilder().apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().apply {
                    this.level = HttpLoggingInterceptor.Level.BODY
                })
            }
        }.build()

        val service = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build().create(ZoteroAPIService::class.java)

        val call: Call<KeyInfo> = service.getKeyInfo(apiKey)
        call.enqueue(object : Callback<KeyInfo> {
            override fun onFailure(call: Call<KeyInfo>, t: Throwable) {
                presenter.createNetworkError("There was a network error Connecting to the Zotero API.")
            }

            override fun onResponse(call: Call<KeyInfo>, response: Response<KeyInfo>) {
                if (response.code() == 200) {
                    val credStorage = AuthenticationStorage(context)
                    val keyInfo = response.body()
                    if (keyInfo == null) {
                        presenter.createNetworkError("Error, got back unrecognizable data from Zotero API.")
                        return
                    }
                    credStorage.setCredentials(
                        keyInfo.username,
                        keyInfo.userID.toString(),
                        keyInfo.key
                    )
                    openLibrary()
                }
                if (response.code() == 404) {
                    presenter.createNetworkError("Error your key was not found. Server Response was:\n" + response.raw())
                } else {
                    presenter.createNetworkError("Unknown network error, got back server code ${response.code()}")
                }
            }
        })
    }

    //This function will be expanded when more apis are setup.
    override fun hasSyncSetup(): Boolean {
        val creds = AuthenticationStorage(context)
        return creds.hasCredentials()
    }

    override fun setupZoteroAPI() {
        presenter.startZoteroAPISetup()
    }

    fun openLibrary() {
        val intent = Intent(context, LibraryActivity::class.java)
        context.startActivity(intent)
        (context as SyncSetupView).finish()
    }

}