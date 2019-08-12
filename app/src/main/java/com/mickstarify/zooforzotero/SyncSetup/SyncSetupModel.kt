package com.mickstarify.zooforzotero.SyncSetup

class SyncSetupModel (val presenter: SyncSetupPresenter) : SyncSetupContract.Model {
    override fun setupZoteroAPI() {
        presenter.startZoteroAPISetup()
    }
}