package com.mickstarify.zooforzotero.ZoteroStorage.Database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        val TABLE_NAME = "Collections"
        database.execSQL("CREATE TABLE IF NOT EXISTS `${TABLE_NAME}` (`key` TEXT NOT NULL, `version` INTEGER NOT NULL, `name` TEXT NOT NULL, `parentCollection` TEXT NOT NULL, `group` INTEGER NOT NULL, PRIMARY KEY(`key`))")

        database.execSQL("CREATE TABLE IF NOT EXISTS `RecentlyOpenedAttachment` (`itemKey` TEXT NOT NULL, PRIMARY KEY(`itemKey`))")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE IF NOT EXISTS `ItemInfo` (`itemKey` TEXT NOT NULL, `group` INTEGER NOT NULL, `version` INTEGER NOT NULL, PRIMARY KEY(`itemKey`, `group`))")
        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_ItemInfo_itemKey` ON `ItemInfo` (`itemKey`)")
        database.execSQL("CREATE TABLE IF NOT EXISTS `ItemData` (`parent` TEXT NOT NULL, `name` TEXT NOT NULL, `value` TEXT NOT NULL, `valueType` TEXT NOT NULL, PRIMARY KEY(`parent`, `name`), FOREIGN KEY(`parent`) REFERENCES `ItemInfo`(`itemKey`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        database.execSQL("CREATE TABLE IF NOT EXISTS `ItemCreator` (`parent` TEXT NOT NULL, `firstName` TEXT NOT NULL, `lastName` TEXT NOT NULL, `creatorType` TEXT NOT NULL, PRIMARY KEY(`parent`, `firstName`, `lastName`), FOREIGN KEY(`parent`) REFERENCES `ItemInfo`(`itemKey`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        database.execSQL("CREATE TABLE IF NOT EXISTS `ItemTags` (`parent` TEXT NOT NULL, `tag` TEXT NOT NULL, PRIMARY KEY(`parent`, `tag`), FOREIGN KEY(`parent`) REFERENCES `ItemInfo`(`itemKey`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        database.execSQL("CREATE TABLE IF NOT EXISTS `ItemCollection` (`collectionKey` TEXT NOT NULL, `itemKey` TEXT NOT NULL, PRIMARY KEY(`collectionKey`, `itemKey`), FOREIGN KEY(`itemKey`) REFERENCES `ItemInfo`(`itemKey`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        database.execSQL("CREATE TABLE IF NOT EXISTS `AttachmentInfo` (`itemKey` TEXT NOT NULL, `group` INTEGER NOT NULL, `md5Key` TEXT NOT NULL, `mtime` INTEGER NOT NULL, `downloadedFrom` TEXT NOT NULL, PRIMARY KEY(`itemKey`, `group`), FOREIGN KEY(`itemKey`) REFERENCES `ItemInfo`(`itemKey`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        database.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        database.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '0edf267c734aa113781c16954559eff8')")

        // delete all old rows from the database (neccessary because i am upgrading the table to add a version record)
        database.execSQL("DELETE FROM `RecentlyOpenedAttachment`")
        database.execSQL("ALTER TABLE `RecentlyOpenedAttachment` ADD `version` INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_3_4 = object : Migration(3,4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE `ItemInfo` ADD `deleted` INTEGER NOT NULL DEFAULT 0")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_ItemCollection_itemKey_collectionKey` ON `ItemCollection` (`itemKey`, `collectionKey`)")
    }

}