package com.mickstarify.zooforzotero.SyncSetup

class SyncSetupPresenter (_view : SyncSetupContract.View) : SyncSetupContract.Presenter {
    override fun startZoteroAPISetup() {
        view.startZoteroAPIActivity()
    }

    override fun selectSyncSetup(option: SyncOption) {
        when(option){
            SyncOption.ZoteroAPI -> model.setupZoteroAPI()
            else -> view.createUnsupportedAlert()
        }
    }

    private val view = _view
    private val model = SyncSetupModel(this)


    init{
        view.initUI()
    }


}