package com.ferrine.stockopname.data.repository

import android.content.ContentValues
import android.content.Context
import com.ferrine.stockopname.BaseDataRepository
import com.ferrine.stockopname.data.db.AppDatabaseHelper
import com.ferrine.stockopname.data.db.DbContract
import com.ferrine.stockopname.data.model.OpnameRow
import com.ferrine.stockopname.data.model.SummaryItem
import com.ferrine.stockopname.data.model.WorkingTypes

class OpnameRowRepository(context: Context) : BaseDataRepository() {
    private val dbHelper = AppDatabaseHelper(context)

    fun insert(row: OpnameRow): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DbContract.OpnameTable.COLUMN_TIMESTAMP, if (row.timestamp == 0L) System.currentTimeMillis() else row.timestamp)
            put(DbContract.OpnameTable.COLUMN_WORKING_TYPE, row.activity)
            put(DbContract.OpnameTable.COLUMN_PROJECT_ID, row.projectId)
            put(DbContract.OpnameTable.COLUMN_DEVICE_ID, row.deviceId)
            put(DbContract.OpnameTable.COLUMN_USER_ID, row.userId)
            put(DbContract.OpnameTable.COLUMN_BARCODE, row.barcode)
            put(DbContract.OpnameTable.COLUMN_BOXCODE, row.boxcode)
            put(DbContract.OpnameTable.COLUMN_ITEM_ID, row.itemId)
            put(DbContract.OpnameTable.COLUMN_SCANNED_QTY, row.scannedQty)
        }
        return db.insert(DbContract.OpnameTable.TABLE_NAME, null, values)
    }

    fun updateScannedQty(timestamp: Long, newQty: Int): Int {
        val db = dbHelper.writableDatabase

        val values = ContentValues().apply {
            put(DbContract.OpnameTable.COLUMN_SCANNED_QTY, newQty)
        }
        return db.update(
            DbContract.OpnameTable.TABLE_NAME,
            values,
            "${DbContract.OpnameTable.COLUMN_TIMESTAMP} = ?",
            arrayOf(timestamp.toString())
        )
    }

    fun cancelInsertedRow(timestamp: Long): Int {
        val db = dbHelper.writableDatabase
        return db.delete(
            DbContract.OpnameTable.TABLE_NAME,
            "${DbContract.OpnameTable.COLUMN_TIMESTAMP} = ?",
            arrayOf(timestamp.toString())
        )
    }

    fun getCount(workingType: WorkingTypes): Long {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM ${DbContract.OpnameTable.TABLE_NAME} WHERE ${DbContract.OpnameTable.COLUMN_WORKING_TYPE} = ?",
            arrayOf(workingType.name)
        )
        var count = 0L
        if (cursor.moveToFirst()) {
            count = cursor.getLong(0)
        }
        cursor.close()
        return count
    }

    fun getScannedQty(workingType: WorkingTypes, itemId: String): Double {
        val db = dbHelper.readableDatabase
        val query = "SELECT SUM(${DbContract.OpnameTable.COLUMN_SCANNED_QTY}) FROM ${DbContract.OpnameTable.TABLE_NAME} " +
                "WHERE ${DbContract.OpnameTable.COLUMN_WORKING_TYPE} = ? " +
                "AND ${DbContract.OpnameTable.COLUMN_ITEM_ID} = ?"

        val cursor = db.rawQuery(query, arrayOf(workingType.name, itemId))
        var totalQty = 0.0

        if (cursor.moveToFirst()) {
            totalQty = cursor.getDouble(0)
        }
        cursor.close()
        return totalQty
    }

    fun getSummaryItem(workingType: WorkingTypes): List<SummaryItem> {
        val db = dbHelper.readableDatabase
        val summaryList = mutableListOf<SummaryItem>()

        val query = """
            SELECT 
                o.${DbContract.OpnameTable.COLUMN_ITEM_ID}, 
                i.${DbContract.ItemTable.COLUMN_MAT}, 
                i.${DbContract.ItemTable.COLUMN_COL}, 
                i.${DbContract.ItemTable.COLUMN_SIZE}, 
                i.${DbContract.ItemTable.COLUMN_NAME}, 
                i.${DbContract.ItemTable.COLUMN_CATEGORY}, 
                SUM(o.${DbContract.OpnameTable.COLUMN_SCANNED_QTY}) as totalQty,
                i.${DbContract.ItemTable.COLUMN_ART}
            FROM ${DbContract.OpnameTable.TABLE_NAME} o
            JOIN ${DbContract.ItemTable.TABLE_NAME} i ON o.${DbContract.OpnameTable.COLUMN_ITEM_ID} = i.${DbContract.ItemTable.COLUMN_ITEM_ID}
            WHERE o.${DbContract.OpnameTable.COLUMN_WORKING_TYPE} = ?
            GROUP BY 
                o.${DbContract.OpnameTable.COLUMN_ITEM_ID}, 
                i.${DbContract.ItemTable.COLUMN_MAT}, 
                i.${DbContract.ItemTable.COLUMN_COL}, 
                i.${DbContract.ItemTable.COLUMN_SIZE}, 
                i.${DbContract.ItemTable.COLUMN_NAME}, 
                i.${DbContract.ItemTable.COLUMN_CATEGORY},
                i.${DbContract.ItemTable.COLUMN_ART}
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(workingType.name))

        if (cursor.moveToFirst()) {
            do {
                summaryList.add(
                    SummaryItem(
                        itemId = cursor.getString(0),
                        material = cursor.getString(1),
                        color = cursor.getString(2),
                        size = cursor.getString(3),
                        name = cursor.getString(4),
                        category = cursor.getString(5),
                        totalQty = cursor.getInt(6),
                        article = cursor.getString(7)
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return summaryList
    }

    fun getSummaryItemByProperty(workingType: WorkingTypes, property: String): List<SummaryItem> {
        val db = dbHelper.readableDatabase
        val summaryList = mutableListOf<SummaryItem>()
        val searchPattern = "%$property%"

        val query = """
            SELECT 
                o.${DbContract.OpnameTable.COLUMN_ITEM_ID}, 
                i.${DbContract.ItemTable.COLUMN_MAT}, 
                i.${DbContract.ItemTable.COLUMN_COL}, 
                i.${DbContract.ItemTable.COLUMN_SIZE}, 
                i.${DbContract.ItemTable.COLUMN_NAME}, 
                i.${DbContract.ItemTable.COLUMN_CATEGORY}, 
                SUM(o.${DbContract.OpnameTable.COLUMN_SCANNED_QTY}) as totalQty,
                i.${DbContract.ItemTable.COLUMN_ART}
            FROM ${DbContract.OpnameTable.TABLE_NAME} o
            JOIN ${DbContract.ItemTable.TABLE_NAME} i ON o.${DbContract.OpnameTable.COLUMN_ITEM_ID} = i.${DbContract.ItemTable.COLUMN_ITEM_ID}
            WHERE o.${DbContract.OpnameTable.COLUMN_WORKING_TYPE} = ?
            AND (
                o.${DbContract.OpnameTable.COLUMN_ITEM_ID} LIKE ? OR 
                i.${DbContract.ItemTable.COLUMN_NAME} LIKE ? OR 
                i.${DbContract.ItemTable.COLUMN_CATEGORY} LIKE ? OR
                i.${DbContract.ItemTable.COLUMN_ART} LIKE ? OR
                i.${DbContract.ItemTable.COLUMN_MAT} LIKE ? OR
                i.${DbContract.ItemTable.COLUMN_COL} LIKE ?
            )
            GROUP BY 
                o.${DbContract.OpnameTable.COLUMN_ITEM_ID}, 
                i.${DbContract.ItemTable.COLUMN_MAT}, 
                i.${DbContract.ItemTable.COLUMN_COL}, 
                i.${DbContract.ItemTable.COLUMN_SIZE}, 
                i.${DbContract.ItemTable.COLUMN_NAME}, 
                i.${DbContract.ItemTable.COLUMN_CATEGORY},
                i.${DbContract.ItemTable.COLUMN_ART}
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(workingType.name, searchPattern, searchPattern, searchPattern, searchPattern, searchPattern, searchPattern))

        if (cursor.moveToFirst()) {
            do {
                summaryList.add(
                    SummaryItem(
                        itemId = cursor.getString(0),
                        material = cursor.getString(1),
                        color = cursor.getString(2),
                        size = cursor.getString(3),
                        name = cursor.getString(4),
                        category = cursor.getString(5),
                        totalQty = cursor.getInt(6),
                        article = cursor.getString(7)
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return summaryList
    }

    fun getSummaryItemByBarcode(workingType: WorkingTypes, barcode: String): List<SummaryItem> {
        val db = dbHelper.readableDatabase
        val summaryList = mutableListOf<SummaryItem>()
        val searchPattern = "%$barcode%"

        val query = """
            SELECT 
                o.${DbContract.OpnameTable.COLUMN_ITEM_ID}, 
                i.${DbContract.ItemTable.COLUMN_MAT}, 
                i.${DbContract.ItemTable.COLUMN_COL}, 
                i.${DbContract.ItemTable.COLUMN_SIZE}, 
                i.${DbContract.ItemTable.COLUMN_NAME}, 
                i.${DbContract.ItemTable.COLUMN_CATEGORY}, 
                SUM(o.${DbContract.OpnameTable.COLUMN_SCANNED_QTY}) as totalQty,
                i.${DbContract.ItemTable.COLUMN_ART}
            FROM ${DbContract.OpnameTable.TABLE_NAME} o
            JOIN ${DbContract.ItemTable.TABLE_NAME} i ON o.${DbContract.OpnameTable.COLUMN_ITEM_ID} = i.${DbContract.ItemTable.COLUMN_ITEM_ID}
            WHERE o.${DbContract.OpnameTable.COLUMN_WORKING_TYPE} = ?
            AND o.${DbContract.OpnameTable.COLUMN_BARCODE} LIKE ?
            GROUP BY 
                o.${DbContract.OpnameTable.COLUMN_ITEM_ID}, 
                i.${DbContract.ItemTable.COLUMN_MAT}, 
                i.${DbContract.ItemTable.COLUMN_COL}, 
                i.${DbContract.ItemTable.COLUMN_SIZE}, 
                i.${DbContract.ItemTable.COLUMN_NAME}, 
                i.${DbContract.ItemTable.COLUMN_CATEGORY},
                i.${DbContract.ItemTable.COLUMN_ART}
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(workingType.name, searchPattern))

        if (cursor.moveToFirst()) {
            do {
                summaryList.add(
                    SummaryItem(
                        itemId = cursor.getString(0),
                        material = cursor.getString(1),
                        color = cursor.getString(2),
                        size = cursor.getString(3),
                        name = cursor.getString(4),
                        category = cursor.getString(5),
                        totalQty = cursor.getInt(6),
                        article = cursor.getString(7)
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return summaryList
    }

    fun deleteAll() {
        val db = dbHelper.writableDatabase
        db.delete(DbContract.OpnameTable.TABLE_NAME, null, null)
    }
}
