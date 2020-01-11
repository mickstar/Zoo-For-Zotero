package com.mickstarify.zooforzotero.LibraryActivity.Notes

import com.mickstarify.zooforzotero.ZoteroAPI.Model.Note

interface NoteInteractionListener {
    fun deleteNote(note: Note)
    fun editNote(note: Note)
}