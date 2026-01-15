package com.ferrine.stockopname.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Item(
    val itemId: String = "",
    val article: String = "",
    val material: String = "",
    val color: String = "",
    val size: String = "",
    val name: String = "",
    val description: String = "",
    val category: String = "",
    val price: Double = 0.0,
    val sellPrice: Double = 0.0,
    val discount: Double = 0.0,
    val isSpecialPrice: Boolean = false,
    val stockQty: Int = 0,
    var printQty: Int = 0,
    val pricingId: String = ""
) : Parcelable
