package com.mickstarify.zooforzotero.SyncSetup

import android.content.Context

class SyncSetupModel (val presenter: SyncSetupPresenter) : SyncSetupContract.Model {

    //This function will be expanded when more apis are setup.
    override fun hasSyncSetup(context: Context): Boolean {
        val creds = AuthenticationStorage(context)
        return creds.hasCredentials()
    }

    override fun setupZoteroAPI() {
        presenter.startZoteroAPISetup()
    }
}