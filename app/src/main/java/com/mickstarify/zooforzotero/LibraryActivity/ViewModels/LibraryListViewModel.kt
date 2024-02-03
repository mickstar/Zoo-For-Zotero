package com.mickstarify.zooforzotero.LibraryActivity.ViewModels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mickstarify.zooforzotero.LibraryActivity.ListEntry
import com.mickstarify.zooforzotero.ZoteroStorage.Database.Collection
import com.mickstarify.zooforzotero.ZoteroStorage.Database.Item

class LibraryListViewModel : ViewModel() {
    private val items = MutableLiveData<List<ListEntry>>()

    fun getItems(): LiveData<List<ListEntry>> = items
    fun setItems(items: List<ListEntry>) {
        Log.d("zotero", "Setting items ${items.size}")
        this.items.value = items
    }

    private val itemClicked = MutableLiveData<Item>()
    fun getOnItemClicked(): LiveData<Item> = itemClicked
    fun onItemClicked(item: Item) {
        this.itemClicked.value = item
    }

    private val attachmentClicked = MutableLiveData<Item>()
    fun getOnAttachmentClicked(): LiveData<Item> = attachmentClicked
    fun onAttachmentClicked(attachment: Item) {
        this.attachmentClicked.value = attachment
    }

    private val collectionClicked = MutableLiveData<Collection>()
    fun getOnCollectionClicked(): LiveData<Collection> = collectionClicked
    fun onCollectionClicked(collection: Collection) {
        this.collectionClicked.value = collection
    }

    private val scannedBarcode = MutableLiveData<String>()
    fun scannedBarcodeNumber(barcodeNo: String) {
        scannedBarcode.value = barcodeNo
    }

    fun getScannedBarcode(): LiveData<String> = scannedBarcode

    private val isShowingLoadingAnimation = MutableLiveData<Boolean>(false)
    fun setIsShowingLoadingAnimation(value: Boolean) {
        if (isShowingLoadingAnimation.value != value) {
            isShowingLoadingAnimation.value = value
        }
    }

    fun getIsShowingLoadingAnimation(): LiveData<Boolean> = isShowingLoadingAnimation

    private val onLibraryRefreshRequested = MutableLiveData<Int>()
    fun onLibraryRefreshRequested() {
        // changes the value so any listener will get pinged.
        onLibraryRefreshRequested.value = (onLibraryRefreshRequested.value ?: 0) + 1
    }

    fun getOnLibraryRefreshRequested(): LiveData<Int> = onLibraryRefreshRequested

    private val libraryFilterText = MutableLiveData<String>("")
    fun getLibraryFilterText(): LiveData<String> = libraryFilterText
    fun setLibraryFilterText(query: String) {
        if (this.libraryFilterText.value != query) {
            this.libraryFilterText.value = query
        }
    }
}