package com.ferrine.stockopname.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.ferrine.stockopname.BaseDataRepository
import com.ferrine.stockopname.data.db.AppDatabaseHelper
import com.ferrine.stockopname.data.db.DbContract
import com.ferrine.stockopname.data.model.Barcode
import com.ferrine.stockopname.data.model.Item

class ItemRepository(context: Context) : BaseDataRepository() {
    private val dbHelper = AppDatabaseHelper(context)

    fun getCount(): Long {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM ${DbContract.ItemTable.TABLE_NAME}", null)
        var count = 0L
        if (cursor.moveToFirst()) {
            count = cursor.getLong(0)
        }
        cursor.close()
        return count
    }

    fun getItemByBarcode(barcode: String): Item? {
        val db = dbHelper.readableDatabase
        val query = """
            SELECT i.* FROM ${DbContract.ItemTable.TABLE_NAME} i
            INNER JOIN ${DbContract.BarcodeTable.TABLE_NAME} b ON i.${DbContract.ItemTable.COLUMN_ITEM_ID} = b.${DbContract.BarcodeTable.COLUMN_ITEM_ID}
            WHERE b.${DbContract.BarcodeTable.COLUMN_BARCODE} = ?
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(barcode))
        var item: Item? = null

        if (cursor.moveToFirst()) {
            item = Item(
                itemId = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.ItemTable.COLUMN_ITEM_ID)),
                article = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.ItemTable.COLUMN_ART)),
                material = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.ItemTable.COLUMN_MAT)),
                color = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.ItemTable.COLUMN_COL)),
                size = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.ItemTable.COLUMN_SIZE)),
                name = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.ItemTable.COLUMN_NAME)),
                description = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.ItemTable.COLUMN_DESC)),
                category = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.ItemTable.COLUMN_CATEGORY)),
                price = cursor.getDouble(cursor.getColumnIndexOrThrow(DbContract.ItemTable.COLUMN_PRICE)),
                sellPrice = cursor.getDouble(cursor.getColumnIndexOrThrow(DbContract.ItemTable.COLUMN_SELL_PRICE)),
                discount = cursor.getDouble(cursor.getColumnIndexOrThrow(DbContract.ItemTable.COLUMN_DISC)),
                isSpecialPrice = cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.ItemTable.COLUMN_ISSP)) == 1,
                stockQty = cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.ItemTable.COLUMN_STOCK_QTY)),
                printQty = cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.ItemTable.COLUMN_PRINT_QTY)),
                pricingId = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.ItemTable.COLUMN_PRICING_ID))
            )
        }
        cursor.close()
        return item
    }

    fun searchItems(query: String?, column: String? = null): List<Item> {
        val db = dbHelper.readableDatabase
        val items = mutableListOf<Item>()
        
        val selection = StringBuilder()
        val selectionArgs = mutableListOf<String>()

        if (!query.isNullOrBlank()) {
            val q = "%$query%"
            if (column == null || column == "All") {
                // Default search: itemId, barcode, name, description, art
                selection.append("""
                    (${DbContract.ItemTable.COLUMN_ITEM_ID} LIKE ? OR 
                     ${DbContract.ItemTable.COLUMN_NAME} LIKE ? OR 
                     ${DbContract.ItemTable.COLUMN_DESC} LIKE ? OR 
                     ${DbContract.ItemTable.COLUMN_ART} LIKE ? OR
                     ${DbContract.ItemTable.COLUMN_ITEM_ID} IN (SELECT ${DbContract.BarcodeTable.COLUMN_ITEM_ID} FROM ${DbContract.BarcodeTable.TABLE_NAME} WHERE ${DbContract.BarcodeTable.COLUMN_BARCODE} LIKE ?))
                """.trimIndent())
                repeat(5) { selectionArgs.add(q) }
            } else {
                when (column) {
                    "barcode" -> {
                        selection.append("${DbContract.ItemTable.COLUMN_ITEM_ID} IN (SELECT ${DbContract.BarcodeTable.COLUMN_ITEM_ID} FROM ${DbContract.BarcodeTable.TABLE_NAME} WHERE ${DbContract.BarcodeTable.COLUMN_BARCODE} LIKE ?)")
                        selectionArgs.add(q)
                    }
                    "itemId" -> { selection.append("${DbContract.ItemTable.COLUMN_ITEM_ID} LIKE ?"); selectionArgs.add(q) }
                    "name" -> { selection.append("${DbContract.ItemTable.COLUMN_NAME} LIKE ?"); selectionArgs.add(q) }
                    "description" -> { selection.append("${DbContract.ItemTable.COLUMN_DESC} LIKE ?"); selectionArgs.add(q) }
                    "art" -> { selection.append("${DbContract.ItemTable.COLUMN_ART} LIKE ?"); selectionArgs.add(q) }
                    "material" -> { selection.append("${DbContract.ItemTable.COLUMN_MAT} LIKE ?"); selectionArgs.add(q) }
                    "col" -> { selection.append("${DbContract.ItemTable.COLUMN_COL} LIKE ?"); selectionArgs.add(q) }
                    "category" -> { selection.append("${DbContract.ItemTable.COLUMN_CATEGORY} LIKE ?"); selectionArgs.add(q) }
                }
            }
        }

        val cursor = db.query(
            DbContract.ItemTable.TABLE_NAME,
            null,
            if (selection.isEmpty()) null else selection.toString(),
            if (selectionArgs.isEmpty()) null else selectionArgs.toTypedArray(),
            null,
            null,
            "${DbContract.ItemTable.COLUMN_NAME} ASC"
        )

        if (cursor.moveToFirst()) {
            do {
                items.add(
                    Item(
                        itemId = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.ItemTable.COLUMN_ITEM_ID)),
                        article = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.ItemTable.COLUMN_ART)),
                        material = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.ItemTable.COLUMN_MAT)),
                        color = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.ItemTable.COLUMN_COL)),
                        size = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.ItemTable.COLUMN_SIZE)),
                        name = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.ItemTable.COLUMN_NAME)),
                        description = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.ItemTable.COLUMN_DESC)),
                        category = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.ItemTable.COLUMN_CATEGORY)),
                        price = cursor.getDouble(cursor.getColumnIndexOrThrow(DbContract.ItemTable.COLUMN_PRICE)),
                        sellPrice = cursor.getDouble(cursor.getColumnIndexOrThrow(DbContract.ItemTable.COLUMN_SELL_PRICE)),
                        discount = cursor.getDouble(cursor.getColumnIndexOrThrow(DbContract.ItemTable.COLUMN_DISC)),
                        isSpecialPrice = cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.ItemTable.COLUMN_ISSP)) == 1,
                        stockQty = cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.ItemTable.COLUMN_STOCK_QTY)),
                        printQty = cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.ItemTable.COLUMN_PRINT_QTY)),
                        pricingId = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.ItemTable.COLUMN_PRICING_ID))
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return items
    }

    fun insertOrUpdate(item: Item) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DbContract.ItemTable.COLUMN_ITEM_ID, item.itemId)
            put(DbContract.ItemTable.COLUMN_ART, item.article)
            put(DbContract.ItemTable.COLUMN_MAT, item.material)
            put(DbContract.ItemTable.COLUMN_COL, item.color)
            put(DbContract.ItemTable.COLUMN_SIZE, item.size)
            put(DbContract.ItemTable.COLUMN_NAME, item.name)
            put(DbContract.ItemTable.COLUMN_DESC, item.description)
            put(DbContract.ItemTable.COLUMN_CATEGORY, item.category)
            put(DbContract.ItemTable.COLUMN_PRICE, item.price)
            put(DbContract.ItemTable.COLUMN_SELL_PRICE, item.sellPrice)
            put(DbContract.ItemTable.COLUMN_DISC, item.discount)
            put(DbContract.ItemTable.COLUMN_ISSP, if (item.isSpecialPrice) 1 else 0)
            put(DbContract.ItemTable.COLUMN_STOCK_QTY, item.stockQty)
            put(DbContract.ItemTable.COLUMN_PRINT_QTY, item.printQty)
            put(DbContract.ItemTable.COLUMN_PRICING_ID, item.pricingId)
        }

        db.insertWithOnConflict(
            DbContract.ItemTable.TABLE_NAME,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun insertOrUpdateBatch(items: List<Pair<Item, Barcode>>) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            for (pair in items) {
                val item = pair.first
                val barcode = pair.second
                
                val itemValues = ContentValues().apply {
                    put(DbContract.ItemTable.COLUMN_ITEM_ID, item.itemId)
                    put(DbContract.ItemTable.COLUMN_ART, item.article)
                    put(DbContract.ItemTable.COLUMN_MAT, item.material)
                    put(DbContract.ItemTable.COLUMN_COL, item.color)
                    put(DbContract.ItemTable.COLUMN_SIZE, item.size)
                    put(DbContract.ItemTable.COLUMN_NAME, item.name)
                    put(DbContract.ItemTable.COLUMN_DESC, item.description)
                    put(DbContract.ItemTable.COLUMN_CATEGORY, item.category)
                    put(DbContract.ItemTable.COLUMN_PRICE, item.price)
                    put(DbContract.ItemTable.COLUMN_SELL_PRICE, item.sellPrice)
                    put(DbContract.ItemTable.COLUMN_DISC, item.discount)
                    put(DbContract.ItemTable.COLUMN_ISSP, if (item.isSpecialPrice) 1 else 0)
                    put(DbContract.ItemTable.COLUMN_STOCK_QTY, item.stockQty)
                    put(DbContract.ItemTable.COLUMN_PRINT_QTY, item.printQty)
                    put(DbContract.ItemTable.COLUMN_PRICING_ID, item.pricingId)
                }
                db.insertWithOnConflict(
                    DbContract.ItemTable.TABLE_NAME,
                    null,
                    itemValues,
                    SQLiteDatabase.CONFLICT_REPLACE
                )
                
                val barcodeValues = ContentValues().apply {
                    put(DbContract.BarcodeTable.COLUMN_BARCODE, barcode.barcode)
                    put(DbContract.BarcodeTable.COLUMN_ITEM_ID, barcode.itemId)
                }
                db.insertWithOnConflict(
                    DbContract.BarcodeTable.TABLE_NAME,
                    null,
                    barcodeValues,
                    SQLiteDatabase.CONFLICT_REPLACE
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun deleteAll() {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            db.delete(DbContract.BarcodeTable.TABLE_NAME, null, null)
            db.delete(DbContract.ItemTable.TABLE_NAME, null, null)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}
