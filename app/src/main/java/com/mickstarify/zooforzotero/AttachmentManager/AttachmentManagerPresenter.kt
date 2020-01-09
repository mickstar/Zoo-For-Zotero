package com.mickstarify.zooforzotero.AttachmentManager

import android.content.Context
import com.mickstarify.zooforzotero.ZoteroStorage.Database.Item
import kotlin.math.round

class AttachmentManagerPresenter(val view: Contract.View, context: Context): Contract.Presenter {
    val model: Contract.Model

    init {
        model = AttachmentManagerModel(this, context)
        view.initUI()
        model.loadLibrary()
    }

    override fun pressedDownloadAttachments() {
        model.downloadAttachments()
    }

    override fun displayAttachmentInformation(nLocal: Int, sizeLocal: Long, nRemote: Int) {
        var sizeLocalString = if (sizeLocal < 1000000L){
            "${(sizeLocal/1000L).toInt()}KB"
        } else {
            // rounds off to 2 decimal places. e.g 43240000 => 43.24MB

            "${round(sizeLocal.toDouble()/10000.0)/100}MB"
        }

        view.displayAttachmentInformation(nLocal, sizeLocalString, nRemote, -1)
    }

    override fun displayLoadingAnimation() {
        view.showLibraryLoadingAnimation()
    }

    override fun finishLoadingAnimation() {
        view.hideLibraryLoadingAnimation()
        view.setDownloadButtonState(true)
    }

    override fun makeToastAlert(message: String) {
        view.makeToastAlert(message)
    }

    override fun createErrorAlert(title: String, message: String, onClick: () -> Unit) {
        view.createErrorAlert(title, message, onClick)
    }

    override fun displayErrorState() {
    }

    override fun updateProgress(filename: String, current: Int, total: Int) {
        view.updateLoadingProgress(filename, current, total)
    }
}