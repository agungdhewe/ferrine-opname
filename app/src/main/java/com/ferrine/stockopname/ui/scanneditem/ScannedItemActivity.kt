package com.ferrine.stockopname.ui.scanneditem

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ferrine.stockopname.BaseDrawerActivity
import com.ferrine.stockopname.R
import com.ferrine.stockopname.data.model.CsvDelimiter
import com.ferrine.stockopname.data.model.WorkingTypes
import com.ferrine.stockopname.data.repository.OpnameRowRepository
import com.ferrine.stockopname.ui.item.ItemActivity
import com.ferrine.stockopname.ui.item.ItemAdapter
import com.ferrine.stockopname.ui.setting.SettingActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScannedItemActivity : BaseDrawerActivity() {

    private lateinit var rvScannedItems: RecyclerView
    private lateinit var spinnerSearchOption: Spinner
    private lateinit var etSearch: EditText
    private lateinit var tvTotalRows: TextView
    private lateinit var tvTotalScannedLabel: TextView
    private lateinit var tvTotalScannedQty: TextView

    private lateinit var adapter: ScannedItemAdapter
    private val opnameRowRepository by lazy { OpnameRowRepository(this) }
    private val prefs by lazy { getSharedPreferences(SettingActivity.PREFS_NAME, Context.MODE_PRIVATE) }
    
    private val searchOptions = listOf("Item Properties", "Barcode")

    private val exportCsvLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? ->
        uri?.let {
            exportScannedDataCsv(it)
        }
    }

    private val exportSummaryLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? ->
        uri?.let {
            exportSummaryCsv(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanned_item)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setupDrawer(toolbar)
        supportActionBar?.title = "Scanned Items"

        bindViews()
        setupSpinner()
        setupRecyclerView()
        setupSearch()
        
        loadData()
    }

    private fun bindViews() {
        rvScannedItems = findViewById(R.id.rvScannedItems)
        spinnerSearchOption = findViewById(R.id.spinnerSearchOption)
        etSearch = findViewById(R.id.etSearch)
        tvTotalRows = findViewById(R.id.tvTotalRows)
        tvTotalScannedLabel = findViewById(R.id.tvTotalScannedLabel)
        tvTotalScannedQty = findViewById(R.id.tvTotalScannedQty)
    }

    private fun setupSpinner() {
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, searchOptions)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSearchOption.adapter = spinnerAdapter
    }

    private fun setupRecyclerView() {
        val sharedPrefs = getSharedPreferences(ItemActivity.PREFS_ITEM, MODE_PRIVATE)
        val savedViewType = sharedPrefs.getInt(ItemActivity.KEY_VIEW_TYPE, ItemAdapter.VIEW_TYPE_FASHION)
        
        val workingType = getWorkingType()

        val qtyLabel = if (workingType == WorkingTypes.PRINTLABEL) "Printed" else "Qty"
        tvTotalScannedLabel.text = "Total $qtyLabel"

        adapter = ScannedItemAdapter(emptyList(), savedViewType, qtyLabel)
        rvScannedItems.layoutManager = LinearLayoutManager(this)
        rvScannedItems.adapter = adapter
    }

    private fun setupSearch() {
        etSearch.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                loadData()
                true
            } else {
                false
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_scanned_item, menu)
        
        // Make "Clear Collected Data" text red
        val clearDataItem = menu.findItem(R.id.action_clear_collected_data)
        clearDataItem?.let {
            val s = SpannableString(it.title)
            s.setSpan(ForegroundColorSpan(Color.RED), 0, s.length, 0)
            it.title = s
        }
        
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_view_simple -> {
                saveViewType(ItemAdapter.VIEW_TYPE_SIMPLE)
                true
            }
            R.id.action_view_fashion -> {
                saveViewType(ItemAdapter.VIEW_TYPE_FASHION)
                true
            }
            R.id.action_download_scanned_data -> {
                val workingType = getWorkingType()
                val fileName = "scanned_data_${workingType.name.lowercase()}.csv"
                exportCsvLauncher.launch(fileName)
                true
            }
            R.id.action_download_summary -> {
                val workingType = getWorkingType()
                val fileName = "summary_${workingType.name.lowercase()}.csv"
                exportSummaryLauncher.launch(fileName)
                true
            }
            R.id.action_clear_collected_data -> {
                showClearCollectedDataDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun saveViewType(viewType: Int) {
        val sharedPrefs = getSharedPreferences(ItemActivity.PREFS_ITEM, MODE_PRIVATE)
        sharedPrefs.edit { putInt(ItemActivity.KEY_VIEW_TYPE, viewType) }
        adapter.setViewType(viewType)
    }

    private fun getWorkingType(): WorkingTypes {
        val workingTypeName = prefs.getString(SettingActivity.KEY_WORKING_TYPE, WorkingTypes.NONE.name)
        return try {
            WorkingTypes.valueOf(workingTypeName ?: WorkingTypes.NONE.name)
        } catch (e: Exception) {
            WorkingTypes.NONE
        }
    }

    private fun loadData() {
        val workingType = getWorkingType()
        if (workingType == WorkingTypes.NONE) return

        val searchText = etSearch.text.toString().trim()
        val searchOption = spinnerSearchOption.selectedItem.toString()

        val summaryList = when {
            searchText.isEmpty() -> {
                opnameRowRepository.getSummaryItem(workingType)
            }
            searchOption == "Item Properties" -> {
                opnameRowRepository.getSummaryItemByProperty(workingType, searchText)
            }
            searchOption == "Barcode" -> {
                opnameRowRepository.getSummaryItemByBarcode(workingType, searchText)
            }
            else -> {
                opnameRowRepository.getSummaryItem(workingType)
            }
        }

        adapter.updateData(summaryList)

        // Update statistics
        tvTotalRows.text = summaryList.size.toString()
        tvTotalScannedQty.text = summaryList.sumOf { it.totalQty }.toString()
    }

    private fun showClearCollectedDataDialog() {
        val input = EditText(this)
        input.hint = "type 'clear data' here"
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        lp.setMargins(48, 20, 48, 0)
        input.layoutParams = lp
        container.addView(input)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Clear Collected Data")
            .setMessage("Data opname yang telah dikumpulkan akan dihapus permanen. Ketik \"clear data\" untuk melanjutkan.")
            .setView(container)
            .setNegativeButton("Batal", null)
            .setPositiveButton("Lanjut Delete") { _, _ ->
                val typedText = input.text.toString()
                if (typedText == "clear data") {
                    val workingType = getWorkingType()
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) { opnameRowRepository.deleteByWorkingType(workingType) }
                        loadData()
                        Toast.makeText(this@ScannedItemActivity, "Data opname berhasil dihapus", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@ScannedItemActivity, "Konfirmasi salah, data tidak dihapus", Toast.LENGTH_SHORT).show()
                }
            }
            .create()

        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(resources.getColor(android.R.color.holo_red_dark))
    }

    private fun exportScannedDataCsv(uri: Uri) {
        lifecycleScope.launch {
            try {
                val workingType = getWorkingType()
                val rows = withContext(Dispatchers.IO) { opnameRowRepository.getAllRows(workingType) }
                
                if (rows.isEmpty()) {
                    Toast.makeText(this@ScannedItemActivity, "No data to export", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val siteCode = prefs.getString(SettingActivity.KEY_SITE_CODE, "") ?: ""
                val brandCode = prefs.getString(SettingActivity.KEY_BRAND_CODE, "") ?: ""

                val delimiterName = prefs.getString(SettingActivity.KEY_CSV_DELIMITER, CsvDelimiter.COMMA.name)
                val selectedDelimiter = CsvDelimiter.entries.find { it.name == delimiterName } ?: CsvDelimiter.COMMA
                val d = selectedDelimiter.character

                val header = "siteCode${d}brandCode${d}timestamp${d}activity${d}projectId${d}deviceId${d}userId${d}barcode${d}boxcode${d}itemId${d}scannedQty\n"
                
                withContext(Dispatchers.IO) {
                    contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(header.toByteArray())
                        rows.forEach { row ->
                            val line = "$siteCode${d}$brandCode${d}${row.timestamp}${d}${row.activity}${d}${row.projectId}${d}${row.deviceId}${d}${row.userId}${d}${row.barcode}${d}${row.boxcode}${d}${row.itemId}${d}${row.scannedQty}\n"
                            outputStream.write(line.toByteArray())
                        }
                    }
                }
                Toast.makeText(this@ScannedItemActivity, "Data exported successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@ScannedItemActivity, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun exportSummaryCsv(uri: Uri) {
        lifecycleScope.launch {
            try {
                val workingType = getWorkingType()
                val summary = withContext(Dispatchers.IO) { opnameRowRepository.getSummaryItemExtended(workingType) }
                
                if (summary.isEmpty()) {
                    Toast.makeText(this@ScannedItemActivity, "No data to export", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val siteCode = prefs.getString(SettingActivity.KEY_SITE_CODE, "") ?: ""
                val brandCode = prefs.getString(SettingActivity.KEY_BRAND_CODE, "") ?: ""

                val delimiterName = prefs.getString(SettingActivity.KEY_CSV_DELIMITER, CsvDelimiter.COMMA.name)
                val selectedDelimiter = CsvDelimiter.entries.find { it.name == delimiterName } ?: CsvDelimiter.COMMA
                val d = selectedDelimiter.character

                val header = "siteCode${d}brandCode${d}projectId${d}workingType${d}deviceId${d}itemId${d}name${d}article${d}material${d}size${d}description${d}totalQty\n"
                
                withContext(Dispatchers.IO) {
                    contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(header.toByteArray())
                        summary.forEach { row ->
                            val line = "$siteCode${d}$brandCode${d}${row["projectId"]}${d}${row["workingType"]}${d}${row["deviceId"]}${d}${row["itemId"]}${d}${row["name"]}${d}${row["article"]}${d}${row["material"]}${d}${row["size"]}${d}${row["description"]}${d}${row["totalQty"]}\n"
                            outputStream.write(line.toByteArray())
                        }
                    }
                }
                Toast.makeText(this@ScannedItemActivity, "Summary exported successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@ScannedItemActivity, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
