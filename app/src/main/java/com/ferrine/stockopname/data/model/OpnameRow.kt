package com.ferrine.stockopname.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class OpnameRow(
    val timestamp: Long = 0L,
    val activity: String = "",
    val opnameId: String = "",
    val deviceId: String = "",
    val userId: String = "",
    val barcode: String = "",
    val boxcode: String = "",
    val itemId: String = "",
    val scannedQty: Int = 0,
    val article: String = "",
    val material: String = "",
    val color: String = "",
    val name: String = "",
    val description: String = "",
    val category: String = "",
    val price: Double = 0.0,
    val sellPrice: Double = 0.0,
    val discount: Double = 0.0,
    val isSpecialPrice: Boolean = false,
    val pricingId: String = ""
) : Parcelable
