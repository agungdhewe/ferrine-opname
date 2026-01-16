package com.ferrine.stockopname.data.db

object DbContract {

	object UserTable {
		const val TABLE_NAME = "user"
		const val COLUMN_USERNAME = "username"
		const val COLUMN_PASSWORD = "password"
		const val COLUMN_FULLNAME = "fullname"
		const val COLUMN_IS_ADMIN = "is_admin"
		const val COLUMN_ALLOW_OPNAME = "allow_opname"
		const val COLUMN_ALLOW_RECEIVING = "allow_receiving"
		const val COLUMN_ALLOW_TRANSFER = "allow_transfer"
		const val COLUMN_ALLOW_PRINTLABEL = "allow_printlabel"
	}


	object ItemTable {
		const val TABLE_NAME = "item"
		const val COLUMN_ITEM_ID = "item_id" // string
		const val COLUMN_ART = "article" // string
		const val COLUMN_MAT = "meterial" // string
		const val COLUMN_COL = "color" // string
		const val COLUMN_SIZE = "size" // string
		const val COLUMN_NAME = "nama" // string
		const val COLUMN_DESC = "deskripsi" // string
		const val COLUMN_CATEGORY = "category" // string
		const val COLUMN_PRICE = "harga" // decimal
		const val COLUMN_SELL_PRICE = "harga_jual" // decimal
		const val COLUMN_DISC = "discount" // percent
		const val COLUMN_ISSP = "is_specialprice" // boolean
		const val COLUMN_STOCK_QTY = "stock_qty"  // integer
		const val COLUMN_PRINT_QTY = "print_qty"  // integer
		const val COLUMN_PRICING_ID = "pricing_id" // string
	}

	object BarcodeTable {
		const val TABLE_NAME = "barcode"
		const val COLUMN_BARCODE = "barcode" // string
		const val COLUMN_ITEM_ID = "item_id" // string
	}

	object OpnameTable {
		const val TABLE_NAME = "opname"
		const val COLUMN_TIMESTAMP = "timestamp" // integer timestamp up milisecond for identity
		const val COLUMN_WORKING_TYPE = "working_type" // string
		const val COLUMN_OPNAME_ID = "opname_id" // string
		const val COLUMN_DEVICE_ID = "device_id" // string
		const val COLUMN_USER_ID = "user_id" // string
		const val COLUMN_BARCODE = "barcode" // string
		const val COLUMN_BOXCODE = "boxcode" // string
		const val COLUMN_ITEM_ID = "item_id" // string
		const val COLUMN_SCANNED_QTY = "qty"  // integer
	}


}
