package com.mickstarify.zooforzotero.AttachmentManager

import com.mickstarify.zooforzotero.ZoteroStorage.Database.Item

interface Contract {
    interface View {
        fun initUI()
        fun showLibraryLoadingAnimation()
        fun hideLibraryLoadingAnimation()
        fun setDownloadButtonState(enabled: Boolean)
        fun updateLoadingProgress(message: String, current: Int, total: Int)
        fun createErrorAlert(title: String, message: String, onClick: () -> Unit)
        fun makeToastAlert(message: String)
        fun displayAttachmentInformation(nLocal: Int, sizeLocal: String, nRemote: Int, sizeRemote: Int)
    }

    interface Presenter {
        fun pressedDownloadAttachments()
        fun displayAttachmentInformation(nLocal: Int, sizeLocal: Long, nRemote: Int)
        fun displayLoadingAnimation()
        fun finishLoadingAnimation()
        fun makeToastAlert(message: String)
        fun displayErrorState()
        fun updateProgress(filename: String, current: Int, total: Int)
        fun createErrorAlert(title: String, message: String, onClick: () -> Unit)
    }

    interface Model {
        fun downloadAttachments()
        fun loadLibrary()
    }
}