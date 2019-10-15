package com.mickstarify.zooforzotero.ZoteroAPI.Model

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AttachmentTest {
    @Test
    fun testNewTemplate() {
        val jsonObject = Attachment.getNewAttachmentTemplate("title","filename", "parentKey")
        assertEquals(jsonObject.toString(), json)
    }

    val json = """[{"itemType":"attachment","parentItem":"parentKey","linkMode":"imported_url","title":"title","accessDate":"","url":"","note":"","tags":[],"relations":{},"contentType":"application/pdf","charset":"","filename":"filename","md5":null,"mtime":null}]"""
}