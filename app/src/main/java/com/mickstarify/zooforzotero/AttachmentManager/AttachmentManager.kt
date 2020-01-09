package com.mickstarify.zooforzotero.AttachmentManager

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import com.mickstarify.zooforzotero.R

import kotlinx.android.synthetic.main.activity_attachment_manager.*
import org.jetbrains.anko.sdk27.coroutines.onClick

class AttachmentManager : AppCompatActivity(), Contract.View {
    lateinit var presenter: Contract.Presenter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attachment_manager)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        presenter = AttachmentManagerPresenter(this, this)
    }

    override fun initUI() {
        findViewById<LinearLayout>(R.id.ll_local).visibility = View.INVISIBLE
        findViewById<LinearLayout>(R.id.ll_remote).visibility = View.INVISIBLE

        findViewById<Button>(R.id.button_download).onClick {
            presenter.pressedDownloadAttachments()
        }
        setDownloadButtonState(false)
    }

    override fun makeToastAlert(message: String) {
        val toast = Toast.makeText(this, message, Toast.LENGTH_LONG)
        toast.show()
    }

    override fun createErrorAlert(title: String, message: String, onClick: () -> Unit) {
        val alert = AlertDialog.Builder(this)
        alert.setIcon(R.drawable.ic_error_black_24dp)
        alert.setTitle(title)
        alert.setMessage(message)
        alert.setPositiveButton("Ok") { _, _ -> onClick() }
        try {
            alert.show()
        } catch (exception: WindowManager.BadTokenException) {
            Log.e("zotero", "error cannot show error dialog. ${message}")
        }
    }

    var progressBar: ProgressBar? = null
    override fun showLibraryLoadingAnimation() {
        if (progressBar == null) {
            progressBar = findViewById(R.id.progressBar)
        }

        progressBar?.isIndeterminate = true
        progressBar?.visibility = View.VISIBLE
        progressBar?.isActivated = true

        val textView = findViewById<TextView>(R.id.txt_loading_library_text)
        textView.text = resources.getString(R.string.loading_your_library)
        textView.visibility = View.VISIBLE
    }

    override fun hideLibraryLoadingAnimation() {
        progressBar?.let {
            it.visibility = View.INVISIBLE
            it.isActivated = false
        }
        val textView = findViewById<TextView>(R.id.txt_loading_library_text)
        textView.visibility = View.INVISIBLE
    }

    override fun setDownloadButtonState(enabled: Boolean) {
        val button = findViewById<Button>(R.id.button_download)
        button.isEnabled = enabled
    }

    override fun updateLoadingProgress(message: String, current: Int, total: Int) {
        if (progressBar == null) {
            progressBar = findViewById(R.id.progressBar)
        }
        progressBar?.let {
            it.visibility = View.VISIBLE
            it.isIndeterminate = false
            it.progress = current
            it.max = total
            it.isActivated = true
        }

        val textView = findViewById<TextView>(R.id.txt_loading_library_text)
        textView.visibility = View.VISIBLE
        textView.text = message
    }

    override fun displayAttachmentInformation(
        nLocal: Int,
        sizeLocal: String,
        nRemote: Int,
        sizeRemote: Int
    ) {
        findViewById<TextView>(R.id.txt_local_number_attachments).text = "$nLocal Attachments"
        findViewById<TextView>(R.id.txt_local_size).text = "${sizeLocal} used"

        findViewById<TextView>(R.id.txt_remote_number_attachments).text = "$nRemote Attachments"
        findViewById<TextView>(R.id.txt_remote_size).text = "Unknown Size"

        findViewById<LinearLayout>(R.id.ll_local).visibility = View.VISIBLE
        findViewById<LinearLayout>(R.id.ll_remote).visibility = View.VISIBLE

        //todo add free disk space.
    }


}
