package com.mickstarify.zooforzotero.LibraryActivity.WebDAV

import android.app.ProgressDialog
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.analytics.FirebaseAnalytics
import com.mickstarify.zooforzotero.PreferenceManager
import com.mickstarify.zooforzotero.R
import com.mickstarify.zooforzotero.ZoteroAPI.Webdav
import kotlinx.android.synthetic.main.activity_web_dav_setup.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.onComplete
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.toast
import java.util.*

class WebDAVSetup : AppCompatActivity() {
    lateinit var preferenceManager: PreferenceManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_dav_setup)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        preferenceManager = PreferenceManager(this)

        val username_editText = findViewById<EditText>(R.id.editText_username)
        val password_editText = findViewById<EditText>(R.id.editText_password)
        val address_editText = findViewById<EditText>(R.id.editText_address)

        username_editText.setText(preferenceManager.getWebDAVUsername())
        password_editText.setText(preferenceManager.getWebDAVPassword())
        address_editText.setText(preferenceManager.getWebDAVAddress())

        val submitButton = findViewById<Button>(R.id.btn_submit)
        val cancelButton = findViewById<Button>(R.id.btn_cancel)

        if (address_editText.text.toString() != "") {
            toast("WebDAV is already configured.")
        }

        submitButton.onClick {
            val address = formatAddress(address_editText.text.toString())
            address_editText.setText(address) // update the address box with https:// if user forgot.
            val username = username_editText.text.toString()
            val password = password_editText.text.toString()
            if (address == "") {
                destroyWebDAVAuthentication()
            } else {
                makeConnection(address, username, password)
            }
        }

        cancelButton.onClick {
            finish()
        }
    }

    fun formatAddress(address: String): String {
        var mAddress = address.trim()
        if (mAddress == "") {
            return ""
        }
        mAddress = if (!mAddress.toUpperCase(Locale.ROOT).startsWith("HTTP")) {
            "https://" + mAddress
        } else {
            mAddress.trim()
        }
        mAddress = if (mAddress.endsWith("/zotero")) {
            mAddress
        } else {
            if (mAddress.endsWith("/")) { // so we don't get server.com//zotero
                mAddress + "zotero"
            } else {
                mAddress + "/zotero"
            }
        }
        return mAddress
    }

    fun makeConnection(address: String, username: String, password: String) {
        val webDav = Webdav(address, username, password)
        startProgressDialog()
        doAsync {
            var status = false // default to false incase we get an exception
            var hadAuthenticationError = false
            var errorMessage = "unset"
            try {
                status = webDav.testConnection()
            } catch (e: Exception) {
                errorMessage = e.message!!
                val bundle = Bundle()
                Log.e("zotero", "got exception ${e}")
                if (e.message?.contains("401 Unauthorized") == true) {
                    hadAuthenticationError = true
                } else {
                    // i dont want to log auth errors.
                    bundle.putString("exception_message", e.message)
                    FirebaseAnalytics.getInstance(this@WebDAVSetup)
                        .logEvent("webdav_connection_exception", bundle)
                }
            }
            Log.d("zotero", "testing webdav got ${status}")
            onComplete {
                hideProgressDialog()
                if (status) {
                    setWebDAVAuthentication(address, username, password)
                } else {
                    if (hadAuthenticationError) {
                        notifyFailed("Authentication Error. Message: ${errorMessage}")
                    } else {
                        notifyFailed("Error setting up webdav, message: $errorMessage")
                    }
                }
            }
        }
    }

    var progressDialog: ProgressDialog? = null
    fun startProgressDialog() {
        if (progressDialog == null) {
            progressDialog = ProgressDialog(this)
        }
        progressDialog?.setTitle("Connecting to WebDAV Server")
        progressDialog?.setMessage("We are testing your connection to the WebDAV Server.")
        progressDialog?.isIndeterminate = true
        progressDialog?.show()
    }

    fun hideProgressDialog() {
        progressDialog?.hide()
        progressDialog = null
    }

    fun notifyFailed(message: String = "") {
        val alertDialog = AlertDialog.Builder(this)
        alertDialog.setTitle("Error connecting to webDAV server")
        if (message == "") {
            alertDialog.setMessage(
                "There was an error connecting to the webDAV server." +
                        "Please verify the address, username and password."
            )
        } else {
            alertDialog.setMessage(message)
        }
        alertDialog.setPositiveButton("Ok") { _, _ -> {} }
        alertDialog.show()
    }

    fun setWebDAVAuthentication(address: String, username: String, password: String) {
        preferenceManager.setWebDAVAuthentication(address, username, password)
        toast("Successfully added WebDAV.")
        this.finish()
    }

    fun destroyWebDAVAuthentication() {
        preferenceManager.destroyWebDAVAuthentication()
        this.finish()
    }
}
