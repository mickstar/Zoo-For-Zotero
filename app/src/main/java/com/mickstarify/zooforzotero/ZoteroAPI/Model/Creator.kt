package com.mickstarify.zooforzotero.ZoteroAPI.Model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Creator (
    val creatorType : String,
    val firstName : String,
    val lastName : String
) : Parcelable