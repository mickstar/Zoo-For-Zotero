package com.mickstarify.zooforzotero.SyncSetup

import android.content.Context

interface SyncSetupContract {
    interface View {
        fun initUI()
        fun createUnsupportedAlert()
        fun startZoteroAPIActivity()
        fun showHowToZoteroSyncDialog(onProceed: () -> Unit)
    }

    interface Presenter {
        fun selectSyncSetup(option: SyncOption)
        fun startZoteroAPISetup()
        fun hasSyncSetup(context: Context): Boolean

    }

    interface Model {
        fun setupZoteroAPI()
        fun hasSyncSetup(context: Context): Boolean
    }
}