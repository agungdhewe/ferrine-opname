package com.ferrine.stockopname.ui.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.ferrine.stockopname.BaseDrawerActivity
import com.ferrine.stockopname.R
import com.ferrine.stockopname.data.model.WorkingTypes
import com.ferrine.stockopname.data.repository.ItemRepository
import com.ferrine.stockopname.data.repository.OpnameRowRepository
import com.ferrine.stockopname.ui.opname.OpnameActivity
import com.ferrine.stockopname.ui.printlabel.PrintlabelActivity
import com.ferrine.stockopname.ui.receiving.ReceivingActivity
import com.ferrine.stockopname.ui.setting.SettingActivity
import com.ferrine.stockopname.ui.transfer.TransferActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : BaseDrawerActivity() {

    private lateinit var tvSiteCode: TextView
    private lateinit var tvBrandCode: TextView
    private lateinit var tvItemCount: TextView
    private lateinit var tvOpnameRowCount: TextView
    private lateinit var tvOpnameLabel: TextView
    private lateinit var tvOpnameCount: TextView
    private lateinit var btnStart: Button

    private val itemRepository by lazy { ItemRepository(this) }
    private val opnameRowRepository by lazy { OpnameRowRepository(this) }

    private val prefs by lazy {
        getSharedPreferences(SettingActivity.PREFS_NAME, Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setupDrawer(toolbar)

        bindView()
        setupListeners()
    }

    private fun bindView() {
        tvSiteCode = findViewById(R.id.tvSiteCode)
        tvBrandCode = findViewById(R.id.tvBrandCode)
        tvItemCount = findViewById(R.id.tvItemCount)
        tvOpnameRowCount = findViewById(R.id.tvOpnameRowCount)
        tvOpnameLabel = findViewById(R.id.tvOpnameLabel)
        tvOpnameCount = findViewById(R.id.tvOpnameCount)
        btnStart = findViewById(R.id.btnStart)
    }

    private fun setupListeners() {
        btnStart.setOnClickListener {
            startWorkingActivity()
        }
    }

    private fun startWorkingActivity() {
        val workingTypeName = prefs.getString(SettingActivity.KEY_WORKING_TYPE, WorkingTypes.NONE.name)
        val workingType = try {
            WorkingTypes.valueOf(workingTypeName ?: WorkingTypes.NONE.name)
        } catch (e: Exception) {
            WorkingTypes.NONE
        }

        val intent = when (workingType) {
            WorkingTypes.OPNAME -> Intent(this, OpnameActivity::class.java)
            WorkingTypes.RECEIVING -> Intent(this, ReceivingActivity::class.java)
            WorkingTypes.TRANSFER -> Intent(this, TransferActivity::class.java)
            WorkingTypes.PRINTLABEL -> Intent(this, PrintlabelActivity::class.java)
            else -> {
                Toast.makeText(this, "Silahkan pilih Working Type di menu Setting", Toast.LENGTH_SHORT).show()
                null
            }
        }
        intent?.let { startActivity(it) }
    }

    override fun onResume() {
        super.onResume()
        displaySettings()
        updateCounts()
    }

    private fun displaySettings() {
        val siteCode = prefs.getString(SettingActivity.KEY_SITE_CODE, "-") ?: "-"
        val brandCode = prefs.getString(SettingActivity.KEY_BRAND_CODE, "-") ?: "-"
        val workingTypeName = prefs.getString(SettingActivity.KEY_WORKING_TYPE, WorkingTypes.NONE.name)
        
        val workingType = try {
            WorkingTypes.valueOf(workingTypeName ?: WorkingTypes.NONE.name)
        } catch (e: Exception) {
            WorkingTypes.NONE
        }

        if (workingType == WorkingTypes.NONE || sessionManager.isAdmin) {
            supportActionBar?.title = "Main"
            btnStart.visibility = View.GONE
        } else {
            supportActionBar?.title = workingType.displayName
            btnStart.visibility = View.VISIBLE
            btnStart.text = "START ${workingType.displayName.uppercase()}"
        }

        tvSiteCode.text = siteCode
        tvBrandCode.text = brandCode
    }

    private fun updateCounts() {
        val workingTypeName = prefs.getString(SettingActivity.KEY_WORKING_TYPE, WorkingTypes.NONE.name)
        val workingType = try {
            WorkingTypes.valueOf(workingTypeName ?: WorkingTypes.NONE.name)
        } catch (e: Exception) {
            WorkingTypes.NONE
        }

        tvOpnameLabel.text = if (workingType == WorkingTypes.PRINTLABEL) "Total Printed" else "Total Qty"

        lifecycleScope.launch {
            val itemCount = withContext(Dispatchers.IO) { itemRepository.getCount() }
            val opnameRowCount = withContext(Dispatchers.IO) { opnameRowRepository.getCount(workingType) }
            val totalScanned = withContext(Dispatchers.IO) { opnameRowRepository.getTotalScannedQty(workingType) }
            
            tvItemCount.text = itemCount.toString()
            tvOpnameRowCount.text = opnameRowCount.toString()
            tvOpnameCount.text = totalScanned.toString()
        }
    }

    override fun drawerIconColor(): Int {
        return android.R.color.black
    }
}
