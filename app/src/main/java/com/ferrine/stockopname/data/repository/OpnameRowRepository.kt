package com.ferrine.stockopname.data.repository

import android.content.Context
import com.ferrine.stockopname.BaseDataRepository
import com.ferrine.stockopname.data.db.AppDatabaseHelper
import com.ferrine.stockopname.data.db.DbContract

class OpnameRowRepository(context: Context) : BaseDataRepository() {
    private val dbHelper = AppDatabaseHelper(context)

    fun getCount(): Long {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM ${DbContract.OpnameTable.TABLE_NAME}", null)
        var count = 0L
        if (cursor.moveToFirst()) {
            count = cursor.getLong(0)
        }
        cursor.close()
        return count
    }

    fun deleteAll() {
        val db = dbHelper.writableDatabase
        db.delete(DbContract.OpnameTable.TABLE_NAME, null, null)
    }
}
