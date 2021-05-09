package com.mickstarify.zooforzotero.LibraryActivity.ViewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class LibraryLoadingScreenViewModel : ViewModel() {
    private val totalAmountOfEntries = MutableLiveData<Int>()
    private val amountOfDownloadedEntries = MutableLiveData<Int>()
    private val loadingMessage = MutableLiveData<String>()

    fun gettotalAmountOfEntries(): LiveData<Int> = totalAmountOfEntries
    fun setTotalAmountOfEntries(amount: Int) {
        totalAmountOfEntries.value = amount
    }

    fun getAmountOfDownloadedEntries(): LiveData<Int> = amountOfDownloadedEntries
    fun setAmountOfDownloadedEntries(amount: Int) {
        amountOfDownloadedEntries.value = amount
    }

    fun getLoadingMessage(): LiveData<String> = loadingMessage
    fun setLoadingMessage(message: String) {
        loadingMessage.value = message
    }
}