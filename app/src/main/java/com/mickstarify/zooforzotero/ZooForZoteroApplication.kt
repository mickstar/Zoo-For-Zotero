package com.mickstarify.zooforzotero

import android.app.Application
import com.mickstarify.zooforzotero.di.component.ApplicationComponent
import com.mickstarify.zooforzotero.di.component.DaggerApplicationComponent
import com.mickstarify.zooforzotero.di.module.ApplicationModule

class ZooForZoteroApplication: Application() {

    lateinit var component: ApplicationComponent

    override fun onCreate() {
        super.onCreate()

//        SoLoader.init(this, false)
//
//        if (BuildConfig.DEBUG && FlipperUtils.shouldEnableFlipper(this)) {
//            val client = AndroidFlipperClient.getInstance(this)
//            client.addPlugin(InspectorFlipperPlugin(this, DescriptorMapping.withDefaults()))
//            client.addPlugin(DatabasesFlipperPlugin(this));
//            client.start()
//        }


        component = DaggerApplicationComponent.builder().applicationModule(
            ApplicationModule(this)
        ).build()
    }

}