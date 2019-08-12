package com.mickstarify.zooforzotero.SyncSetup

interface SyncSetupContract {
    interface View {
        fun initUI()
        fun createUnsupportedAlert()
        fun startZoteroAPIActivity()
    }

    interface Presenter {
        fun selectSyncSetup(option: SyncOption)
        fun startZoteroAPISetup()

    }

    interface Model {
        fun setupZoteroAPI()
    }
}