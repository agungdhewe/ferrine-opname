package com.birumuda.stockopname.utils

import com.birumuda.stockopname.data.model.Item
import com.birumuda.stockopname.data.model.Label

object BarcodeLabel {

	fun createBarcodeLabel(barcode: String, item: Item) : Label {
		return Label(
			barcode = barcode,
			description = item.name,
			category = item.category,
			price = java.math.BigDecimal(item.price),
			discount = "50%",
			oldPrice = java.math.BigDecimal(200000),
			pricingCode = "P01"
		)
	}


    fun generateTspl(label: Label): String {
        return StringBuilder().apply {
            appendLine("SIZE 35 mm, 30 mm")
            appendLine("GAP 2 mm, 0")
            appendLine("DIRECTION 0")
            appendLine("CLS")
            appendLine("TEXT 10,10,\"ROMAN.TTF\",0,1,1,\"${label.description}\"")
            appendLine("BARCODE 10,50,\"128\",50,1,0,2,2,\"${label.barcode}\"")
            appendLine("PRINT 1")
        }.toString()
    }


}
