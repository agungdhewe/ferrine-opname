package com.ferrine.stockopname.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SummaryItem(
    val itemId: String,
    val article: String,
    val material: String,
    val color: String,
    val size: String,
    val name: String,
    val category: String,
    val totalQty: Int
) : Parcelable
