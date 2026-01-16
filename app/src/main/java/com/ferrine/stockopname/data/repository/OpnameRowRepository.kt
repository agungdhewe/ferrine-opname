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

    fun getAllRows(workingType: WorkingTypes): List<OpnameRow> {
        val db = dbHelper.readableDatabase
        val rows = mutableListOf<OpnameRow>()
        val cursor = db.query(
            DbContract.OpnameTable.TABLE_NAME,
            null,
            "${DbContract.OpnameTable.COLUMN_WORKING_TYPE} = ?",
            arrayOf(workingType.name),
            null, null, null
        )

        if (cursor.moveToFirst()) {
            do {
                rows.add(
                    OpnameRow(
                        timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(DbContract.OpnameTable.COLUMN_TIMESTAMP)),
                        activity = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.OpnameTable.COLUMN_WORKING_TYPE)),
                        projectId = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.OpnameTable.COLUMN_PROJECT_ID)),
                        deviceId = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.OpnameTable.COLUMN_DEVICE_ID)),
                        userId = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.OpnameTable.COLUMN_USER_ID)),
                        barcode = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.OpnameTable.COLUMN_BARCODE)),
                        boxcode = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.OpnameTable.COLUMN_BOXCODE)),
                        itemId = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.OpnameTable.COLUMN_ITEM_ID)),
                        scannedQty = cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.OpnameTable.COLUMN_SCANNED_QTY))
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return rows
    }

    fun getSummaryItem(workingType: WorkingTypes): List<SummaryItem> {
        val db = dbHelper.readableDatabase
        val summaryList = mutableListOf<SummaryItem>()

        val query = """
            SELECT 
                o.${DbContract.OpnameTable.COLUMN_ITEM_ID}, 
                i.${DbContract.ItemTable.COLUMN_ART}, 
                i.${DbContract.ItemTable.COLUMN_MAT}, 
                i.${DbContract.ItemTable.COLUMN_COL}, 
                i.${DbContract.ItemTable.COLUMN_SIZE}, 
                i.${DbContract.ItemTable.COLUMN_NAME}, 
                i.${DbContract.ItemTable.COLUMN_CATEGORY}, 
                SUM(o.${DbContract.OpnameTable.COLUMN_SCANNED_QTY}) as totalQty,
                i.${DbContract.ItemTable.COLUMN_DESC}
            FROM ${DbContract.OpnameTable.TABLE_NAME} o
            JOIN ${DbContract.ItemTable.TABLE_NAME} i ON o.${DbContract.OpnameTable.COLUMN_ITEM_ID} = i.${DbContract.ItemTable.COLUMN_ITEM_ID}
            WHERE o.${DbContract.OpnameTable.COLUMN_WORKING_TYPE} = ?
            GROUP BY 
                o.${DbContract.OpnameTable.COLUMN_ITEM_ID}, 
                i.${DbContract.ItemTable.COLUMN_ART}, 
                i.${DbContract.ItemTable.COLUMN_MAT}, 
                i.${DbContract.ItemTable.COLUMN_COL}, 
                i.${DbContract.ItemTable.COLUMN_SIZE}, 
                i.${DbContract.ItemTable.COLUMN_NAME}, 
                i.${DbContract.ItemTable.COLUMN_CATEGORY},
                i.${DbContract.ItemTable.COLUMN_DESC}
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(workingType.name))

        if (cursor.moveToFirst()) {
            do {
                summaryList.add(
                    SummaryItem(
                        itemId = cursor.getString(0),
                        article = cursor.getString(1),
                        material = cursor.getString(2),
                        color = cursor.getString(3),
                        size = cursor.getString(4),
                        name = cursor.getString(5),
                        category = cursor.getString(6),
                        totalQty = cursor.getInt(7),
                        description = cursor.getString(8)
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return summaryList
    }

    fun getSummaryItemExtended(workingType: WorkingTypes): List<Map<String, Any>> {
        val db = dbHelper.readableDatabase
        val summaryList = mutableListOf<Map<String, Any>>()

        val query = """
            SELECT 
                o.${DbContract.OpnameTable.COLUMN_PROJECT_ID}, 
                o.${DbContract.OpnameTable.COLUMN_WORKING_TYPE},
                o.${DbContract.OpnameTable.COLUMN_DEVICE_ID}, 
                o.${DbContract.OpnameTable.COLUMN_ITEM_ID}, 
                i.${DbContract.ItemTable.COLUMN_NAME}, 
                i.${DbContract.ItemTable.COLUMN_ART}, 
                i.${DbContract.ItemTable.COLUMN_MAT}, 
                i.${DbContract.ItemTable.COLUMN_SIZE}, 
                i.${DbContract.ItemTable.COLUMN_DESC}, 
                SUM(o.${DbContract.OpnameTable.COLUMN_SCANNED_QTY}) as totalQty
            FROM ${DbContract.OpnameTable.TABLE_NAME} o
            JOIN ${DbContract.ItemTable.TABLE_NAME} i ON o.${DbContract.OpnameTable.COLUMN_ITEM_ID} = i.${DbContract.ItemTable.COLUMN_ITEM_ID}
            WHERE o.${DbContract.OpnameTable.COLUMN_WORKING_TYPE} = ?
            GROUP BY 
                o.${DbContract.OpnameTable.COLUMN_PROJECT_ID}, 
                o.${DbContract.OpnameTable.COLUMN_WORKING_TYPE},
                o.${DbContract.OpnameTable.COLUMN_DEVICE_ID}, 
                o.${DbContract.OpnameTable.COLUMN_ITEM_ID}, 
                i.${DbContract.ItemTable.COLUMN_NAME}, 
                i.${DbContract.ItemTable.COLUMN_ART}, 
                i.${DbContract.ItemTable.COLUMN_MAT}, 
                i.${DbContract.ItemTable.COLUMN_SIZE}, 
                i.${DbContract.ItemTable.COLUMN_DESC}
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(workingType.name))

        if (cursor.moveToFirst()) {
            do {
                summaryList.add(
                    mapOf(
                        "projectId" to cursor.getString(0),
                        "workingType" to cursor.getString(1),
                        "deviceId" to cursor.getString(2),
                        "itemId" to cursor.getString(3),
                        "name" to cursor.getString(4),
                        "article" to cursor.getString(5),
                        "material" to cursor.getString(6),
                        "size" to cursor.getString(7),
                        "description" to cursor.getString(8),
                        "totalQty" to cursor.getInt(9)
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
                i.${DbContract.ItemTable.COLUMN_ART}, 
                i.${DbContract.ItemTable.COLUMN_MAT}, 
                i.${DbContract.ItemTable.COLUMN_COL}, 
                i.${DbContract.ItemTable.COLUMN_SIZE}, 
                i.${DbContract.ItemTable.COLUMN_NAME}, 
                i.${DbContract.ItemTable.COLUMN_CATEGORY}, 
                SUM(o.${DbContract.OpnameTable.COLUMN_SCANNED_QTY}) as totalQty,
                i.${DbContract.ItemTable.COLUMN_DESC}
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
                i.${DbContract.ItemTable.COLUMN_ART}, 
                i.${DbContract.ItemTable.COLUMN_MAT}, 
                i.${DbContract.ItemTable.COLUMN_COL}, 
                i.${DbContract.ItemTable.COLUMN_SIZE}, 
                i.${DbContract.ItemTable.COLUMN_NAME}, 
                i.${DbContract.ItemTable.COLUMN_CATEGORY},
                i.${DbContract.ItemTable.COLUMN_DESC}
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(workingType.name, searchPattern, searchPattern, searchPattern, searchPattern, searchPattern, searchPattern))

        if (cursor.moveToFirst()) {
            do {
                summaryList.add(
                    SummaryItem(
                        itemId = cursor.getString(0),
                        article = cursor.getString(1),
                        material = cursor.getString(2),
                        color = cursor.getString(3),
                        size = cursor.getString(4),
                        name = cursor.getString(5),
                        category = cursor.getString(6),
                        totalQty = cursor.getInt(7),
                        description = cursor.getString(8)
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
                i.${DbContract.ItemTable.COLUMN_ART}, 
                i.${DbContract.ItemTable.COLUMN_MAT}, 
                i.${DbContract.ItemTable.COLUMN_COL}, 
                i.${DbContract.ItemTable.COLUMN_SIZE}, 
                i.${DbContract.ItemTable.COLUMN_NAME}, 
                i.${DbContract.ItemTable.COLUMN_CATEGORY}, 
                SUM(o.${DbContract.OpnameTable.COLUMN_SCANNED_QTY}) as totalQty,
                i.${DbContract.ItemTable.COLUMN_DESC}
            FROM ${DbContract.OpnameTable.TABLE_NAME} o
            JOIN ${DbContract.ItemTable.TABLE_NAME} i ON o.${DbContract.OpnameTable.COLUMN_ITEM_ID} = i.${DbContract.ItemTable.COLUMN_ITEM_ID}
            WHERE o.${DbContract.OpnameTable.COLUMN_WORKING_TYPE} = ?
            AND o.${DbContract.OpnameTable.COLUMN_BARCODE} LIKE ?
            GROUP BY 
                o.${DbContract.OpnameTable.COLUMN_ITEM_ID}, 
                i.${DbContract.ItemTable.COLUMN_ART}, 
                i.${DbContract.ItemTable.COLUMN_MAT}, 
                i.${DbContract.ItemTable.COLUMN_COL}, 
                i.${DbContract.ItemTable.COLUMN_SIZE}, 
                i.${DbContract.ItemTable.COLUMN_NAME}, 
                i.${DbContract.ItemTable.COLUMN_CATEGORY},
                i.${DbContract.ItemTable.COLUMN_DESC}
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(workingType.name, searchPattern))

        if (cursor.moveToFirst()) {
            do {
                summaryList.add(
                    SummaryItem(
                        itemId = cursor.getString(0),
                        article = cursor.getString(1),
                        material = cursor.getString(2),
                        color = cursor.getString(3),
                        size = cursor.getString(4),
                        name = cursor.getString(5),
                        category = cursor.getString(6),
                        totalQty = cursor.getInt(7),
                        description = cursor.getString(8)
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

    fun deleteByWorkingType(workingType: WorkingTypes) {
        val db = dbHelper.writableDatabase
        db.delete(
            DbContract.OpnameTable.TABLE_NAME,
            "${DbContract.OpnameTable.COLUMN_WORKING_TYPE} = ?",
            arrayOf(workingType.name)
        )
    }
}
