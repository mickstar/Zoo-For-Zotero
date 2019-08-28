package com.mickstarify.zooforzotero.SyncSetup

import android.content.Context

class SyncSetupPresenter(private val view: SyncSetupContract.View) : SyncSetupContract.Presenter {
    override fun hasSyncSetup(context: Context): Boolean {
        return model.hasSyncSetup(context)
    }

    override fun startZoteroAPISetup() {
        view.startZoteroAPIActivity()
    }

    override fun selectSyncSetup(option: SyncOption) {
        when(option){
            SyncOption.ZoteroAPI -> {
                view.showHowToZoteroSyncDialog({
                    model.setupZoteroAPI()
                })
            }
            else -> view.createUnsupportedAlert()
        }
    }

    private val model = SyncSetupModel(this)


    init{
        view.initUI()
    }


}