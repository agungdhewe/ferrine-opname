package com.ferrine.stockopname.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class OpnameRow(
    val timestamp: Long = 0L,
    val activity: String = "",
    val projectId: String = "",
    val deviceId: String = "",
    val userId: String = "",
    val barcode: String = "",
    val boxcode: String = "",
    val itemId: String = "",
    val scannedQty: Int = 1,
) : Parcelable
