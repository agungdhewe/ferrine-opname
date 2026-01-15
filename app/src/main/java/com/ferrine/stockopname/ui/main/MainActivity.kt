package com.ferrine.stockopname.ui.main

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.ferrine.stockopname.BaseDrawerActivity
import com.ferrine.stockopname.R
import com.ferrine.stockopname.data.model.Item
import com.ferrine.stockopname.data.model.WorkingTypes
import com.ferrine.stockopname.data.repository.ItemRepository
import com.ferrine.stockopname.data.repository.OpnameRowRepository
import com.ferrine.stockopname.ui.setting.SettingActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : BaseDrawerActivity() {

    private lateinit var tvSiteCode: TextView
    private lateinit var tvBrandCode: TextView
    private lateinit var tvItemCount: TextView
    private lateinit var tvOpnameCount: TextView
    private lateinit var btnUploadCsv: Button

    private val itemRepository by lazy { ItemRepository(this) }
    private val opnameRowRepository by lazy { OpnameRowRepository(this) }

    private val prefs by lazy {
        getSharedPreferences(SettingActivity.PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val selectCsvLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            importCsv(it)
        }
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
        tvOpnameCount = findViewById(R.id.tvOpnameCount)
        btnUploadCsv = findViewById(R.id.btnUploadCsv)
    }

    private fun setupListeners() {
        btnUploadCsv.setOnClickListener {
            selectCsvLauncher.launch("text/comma-separated-values")
        }
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
        val useCentralServer = prefs.getBoolean(SettingActivity.KEY_USE_CENTRAL_SERVER, false)
        
        val workingType = try {
            WorkingTypes.valueOf(workingTypeName ?: WorkingTypes.NONE.name)
        } catch (e: Exception) {
            WorkingTypes.NONE
        }

        if (workingType == WorkingTypes.NONE || sessionManager.isAdmin) {
            supportActionBar?.title = "Main"
        } else {
            supportActionBar?.title = workingType.displayName
        }

        tvSiteCode.text = siteCode
        tvBrandCode.text = brandCode

        // Fitur upload CSV muncul jika Use Central Server aktif
        btnUploadCsv.visibility = if (!useCentralServer) View.VISIBLE else View.GONE
    }

    private fun updateCounts() {
        lifecycleScope.launch {
            val itemCount = withContext(Dispatchers.IO) { itemRepository.getCount() }
            val opnameCount = withContext(Dispatchers.IO) { opnameRowRepository.getCount() }
            
            tvItemCount.text = itemCount.toString()
            tvOpnameCount.text = opnameCount.toString()
        }
    }

    private fun importCsv(uri: Uri) {
        lifecycleScope.launch {
            try {
                val items = withContext(Dispatchers.IO) {
                    parseCsv(uri)
                }
                withContext(Dispatchers.IO) {
                    itemRepository.insertOrUpdateBatch(items)
                }
                Toast.makeText(this@MainActivity, "Berhasil mengimpor ${items.size} item", Toast.LENGTH_SHORT).show()
                updateCounts()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@MainActivity, "Gagal mengimpor CSV: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun parseCsv(uri: Uri): List<Item> {
        val items = mutableListOf<Item>()
        contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                // Skip header
                val header = reader.readLine()
                var line: String? = reader.readLine()
                while (line != null) {
                    val tokens = line.split("|")
                    if (tokens.size >= 15) {
                        val item = Item(
                            itemId = tokens[0].trim(),
                            article = tokens[1].trim(),
                            material = tokens[2].trim(),
                            color = tokens[3].trim(),
                            size = tokens[4].trim(),
                            name = tokens[5].trim(),
                            description = tokens[6].trim(),
                            category = tokens[7].trim(),
                            price = tokens[8].trim().toDoubleOrNull() ?: 0.0,
                            sellPrice = tokens[9].trim().toDoubleOrNull() ?: 0.0,
                            discount = tokens[10].trim().toDoubleOrNull() ?: 0.0,
                            isSpecialPrice = tokens[11].trim().lowercase() == "true" || tokens[11].trim() == "1",
                            stockQty = tokens[12].trim().toIntOrNull() ?: 0,
                            printQty = tokens[13].trim().toIntOrNull() ?: 0,
                            pricingId = tokens[14].trim()
                        )
                        items.add(item)
                    }
                    line = reader.readLine()
                }
            }
        }
        return items
    }

    override fun drawerIconColor(): Int {
        return android.R.color.black
    }
}
