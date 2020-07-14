package com.mickstarify.zooforzotero

import android.app.Application
import com.mickstarify.zooforzotero.di.component.ApplicationComponent
import com.mickstarify.zooforzotero.di.component.DaggerApplicationComponent
import com.mickstarify.zooforzotero.di.module.ApplicationModule

class ZooForZoteroApplication : Application() {

    lateinit var component: ApplicationComponent

    override fun onCreate() {
        super.onCreate()

        component = DaggerApplicationComponent.builder().applicationModule(
            ApplicationModule(this)
        ).build()
    }

}