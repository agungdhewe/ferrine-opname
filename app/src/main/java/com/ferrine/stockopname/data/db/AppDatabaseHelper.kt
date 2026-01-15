package com.ferrine.stockopname.data.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.ferrine.stockopname.data.repository.UserRepository

class AppDatabaseHelper(context: Context) :
	SQLiteOpenHelper(context, "stockopname.db", null, 1) {

	override fun onCreate(db: SQLiteDatabase) {
		createUserTable(db)
		createItemTable(db)
		createBarcodeTable(db)
		createOpnameTable(db)
		insertDummyUsers(db)
	}

	override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
	}

	fun resetDatabase() {
		val db = writableDatabase
		db.execSQL("DROP TABLE IF EXISTS ${DbContract.UserTable.TABLE_NAME}")
		db.execSQL("DROP TABLE IF EXISTS ${DbContract.ItemTable.TABLE_NAME}")
		db.execSQL("DROP TABLE IF EXISTS ${DbContract.BarcodeTable.TABLE_NAME}")
		db.execSQL("DROP TABLE IF EXISTS ${DbContract.OpnameTable.TABLE_NAME}")
		onCreate(db)
	}

	private fun createUserTable(db: SQLiteDatabase) {
		db.execSQL("""
			CREATE TABLE ${DbContract.UserTable.TABLE_NAME} (
				${DbContract.UserTable.COLUMN_USERNAME} TEXT PRIMARY KEY,
				${DbContract.UserTable.COLUMN_PASSWORD} TEXT,
				${DbContract.UserTable.COLUMN_FULLNAME} TEXT,
				${DbContract.UserTable.COLUMN_IS_ADMIN} INTEGER,
				${DbContract.UserTable.COLUMN_ALLOW_OPNAME} INTEGER,
				${DbContract.UserTable.COLUMN_ALLOW_RECEIVING} INTEGER,
				${DbContract.UserTable.COLUMN_ALLOW_TRANSFER} INTEGER,
				${DbContract.UserTable.COLUMN_ALLOW_PRINTLABEL} INTEGER
			)
        """)
	}

	private fun insertDummyUsers(db: SQLiteDatabase) {
		// agung isAdmin = false dan allowOpname
		insertUser(db, "user", "User", "user",
			isAdmin = false, allowOpname = true, allowReceiving = false, allowTransfer = false, allowPrintlabel = false)
		
		// admin isAdmin = true, dan allow all
		insertUser(db, "admin", "Admin", "admin", 
			isAdmin = true, allowOpname = true, allowReceiving = true, allowTransfer = true, allowPrintlabel = true)
	}

	private fun insertUser(
		db: SQLiteDatabase, 
		username: String, 
		fullname: String, 
		pass: String,
		isAdmin: Boolean = false,
		allowOpname: Boolean = false,
		allowReceiving: Boolean = false,
		allowTransfer: Boolean = false,
		allowPrintlabel: Boolean = false
	) {
		val values = ContentValues().apply {
			put(DbContract.UserTable.COLUMN_USERNAME, username)
			put(DbContract.UserTable.COLUMN_FULLNAME, fullname)
			put(DbContract.UserTable.COLUMN_PASSWORD, UserRepository.hashPassword(pass))
			put(DbContract.UserTable.COLUMN_IS_ADMIN, if (isAdmin) 1 else 0)
			put(DbContract.UserTable.COLUMN_ALLOW_OPNAME, if (allowOpname) 1 else 0)
			put(DbContract.UserTable.COLUMN_ALLOW_RECEIVING, if (allowReceiving) 1 else 0)
			put(DbContract.UserTable.COLUMN_ALLOW_TRANSFER, if (allowTransfer) 1 else 0)
			put(DbContract.UserTable.COLUMN_ALLOW_PRINTLABEL, if (allowPrintlabel) 1 else 0)
		}
		db.insert(DbContract.UserTable.TABLE_NAME, null, values)
	}

	private fun createItemTable(db: SQLiteDatabase) {
		db.execSQL("""
			CREATE TABLE ${DbContract.ItemTable.TABLE_NAME} (
				${DbContract.ItemTable.COLUMN_ITEM_ID} INTEGER PRIMARY KEY,
				${DbContract.ItemTable.COLUMN_ART} TEXT,
				${DbContract.ItemTable.COLUMN_MAT} TEXT,
				${DbContract.ItemTable.COLUMN_COL} TEXT,
				${DbContract.ItemTable.COLUMN_SIZE} TEXT,
				${DbContract.ItemTable.COLUMN_NAME} TEXT,
				${DbContract.ItemTable.COLUMN_DESC} TEXT,
				${DbContract.ItemTable.COLUMN_CATEGORY} TEXT,
				${DbContract.ItemTable.COLUMN_PRICE} REAL,
				${DbContract.ItemTable.COLUMN_SELL_PRICE} REAL,
				${DbContract.ItemTable.COLUMN_DISC} REAL,
				${DbContract.ItemTable.COLUMN_ISSP} INTEGER DEFAULT 0,
				${DbContract.ItemTable.COLUMN_STOCK_QTY} INTEGER,
				${DbContract.ItemTable.COLUMN_PRINT_QTY} INTEGER,
				${DbContract.ItemTable.COLUMN_PRICING_ID} TEXT
			)
		""")
	}

	private fun createBarcodeTable(db: SQLiteDatabase) {
		db.execSQL("""
			CREATE TABLE ${DbContract.BarcodeTable.TABLE_NAME} (
				${DbContract.BarcodeTable.COLUMN_BARCODE} TEXT PRIMARY KEY,
				${DbContract.BarcodeTable.COLUMN_ITEM_ID} TEXT,
				CONSTRAINT barcode_pair UNIQUE (${DbContract.BarcodeTable.COLUMN_BARCODE}, ${DbContract.BarcodeTable.COLUMN_ITEM_ID})
			)
		""")
	}

	private fun createOpnameTable(db: SQLiteDatabase) {
		db.execSQL("""
		CREATE TABLE ${DbContract.OpnameTable.TABLE_NAME} (
			${DbContract.OpnameTable.COLUMN_TIMESTAMP} INTEGER PRIMARY KEY,
			${DbContract.OpnameTable.COLUMN_ACTIVITY} TEXT,
			${DbContract.OpnameTable.COLUMN_OPNAME_ID} TEXT,
			${DbContract.OpnameTable.COLUMN_DEVICE_ID} TEXT,
			${DbContract.OpnameTable.COLUMN_USER_ID} TEXT,
			${DbContract.OpnameTable.COLUMN_BARCODE} TEXT,
			${DbContract.OpnameTable.COLUMN_BOXCODE} TEXT,
			${DbContract.OpnameTable.COLUMN_ITEM_ID} TEXT,
			${DbContract.OpnameTable.COLUMN_SCANNED_QTY} INTEGER,
			${DbContract.OpnameTable.COLUMN_ART} TEXT,
			${DbContract.OpnameTable.COLUMN_MAT} TEXT,
			${DbContract.OpnameTable.COLUMN_COL} TEXT,
			${DbContract.OpnameTable.COLUMN_SIZE} TEXT,
			${DbContract.OpnameTable.COLUMN_NAME} TEXT,
			${DbContract.OpnameTable.COLUMN_DESC} TEXT,
			${DbContract.OpnameTable.COLUMN_CATEGORY} TEXT,
			${DbContract.OpnameTable.COLUMN_PRICE} REAL,
			${DbContract.OpnameTable.COLUMN_SELL_PRICE} REAL,
			${DbContract.OpnameTable.COLUMN_DISC} REAL,
			${DbContract.OpnameTable.COLUMN_ISSP} INTEGER,
			${DbContract.OpnameTable.COLUMN_PRICING_ID} TEXT
		)
	""")
	}
}
