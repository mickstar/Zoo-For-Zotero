package com.mickstarify.zooforzotero.LibraryActivity.ViewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mickstarify.zooforzotero.LibraryActivity.ListEntry

class LibraryListViewModel : ViewModel() {
    private val items = MutableLiveData<List<ListEntry>>()

    fun getItems(): LiveData<List<ListEntry>> = items
    fun setItems(items: List<ListEntry>) {
        this.items.value = items
    }
}