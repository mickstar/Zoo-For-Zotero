package com.mickstarify.zooforzotero.ZoteroStorage.Database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        val TABLE_NAME = "Collections"
        database.execSQL("CREATE TABLE IF NOT EXISTS `${TABLE_NAME}` (`key` TEXT NOT NULL, `version` INTEGER NOT NULL, `name` TEXT NOT NULL, `parentCollection` TEXT NOT NULL, `group` INTEGER NOT NULL, PRIMARY KEY(`key`))")
    }
}