package com.ferrine.stockopname.ui.item

import android.content.Context
import android.content.Intent
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
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ferrine.stockopname.BaseDrawerActivity
import com.ferrine.stockopname.R
import com.ferrine.stockopname.data.model.Barcode
import com.ferrine.stockopname.data.model.CsvDelimiter
import com.ferrine.stockopname.data.model.Item
import com.ferrine.stockopname.data.repository.ItemRepository
import com.ferrine.stockopname.ui.setting.SettingActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader

class ItemActivity : BaseDrawerActivity() {

    private lateinit var rvItems: RecyclerView
    private lateinit var spinnerSearchColumn: Spinner
    private lateinit var etSearch: EditText
    private lateinit var tvTotalRows: TextView
    private lateinit var tvTotalStockQty: TextView
    private lateinit var tvTotalPrintQty: TextView

    private lateinit var adapter: ItemAdapter
    private val itemRepository by lazy { ItemRepository(this) }

    private val searchColumns = listOf("All", "itemId", "barcode", "name", "description", "art", "material", "col", "category")

    private val prefs by lazy {
        getSharedPreferences(SettingActivity.PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val selectCsvLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            importItemCsv(it)
        }
    }

    companion object {
        const val PREFS_ITEM = "prefs_item"
        const val KEY_VIEW_TYPE = "view_type"

        const val ITEM_CSV_HEADER = "barcode|itemId|article|material|color|size|name|description|category|price|sellPrice|discount|isSpecialPrice|stockQty|printQty|pricingId"
        const val COL_BARCODE = 0
        const val COL_ITEM_ID = 1
        const val COL_ARTICLE = 2
        const val COL_MATERIAL = 3
        const val COL_COLOR = 4
        const val COL_SIZE = 5
        const val COL_NAME = 6
        const val COL_DESCRIPTION = 7
        const val COL_CATEGORY = 8
        const val COL_PRICE = 9
        const val COL_SELL_PRICE = 10
        const val COL_DISCOUNT = 11
        const val COL_IS_SPECIAL_PRICE = 12
        const val COL_STOCK_QTY = 13
        const val COL_PRINT_QTY = 14
        const val COL_PRICING_ID = 15
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_item)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setupDrawer(toolbar)
        supportActionBar?.title = "Item"

        rvItems = findViewById(R.id.rvItems)
        spinnerSearchColumn = findViewById(R.id.spinnerSearchColumn)
        etSearch = findViewById(R.id.etSearch)
        tvTotalRows = findViewById(R.id.tvTotalRows)
        tvTotalStockQty = findViewById(R.id.tvTotalStockQty)
        tvTotalPrintQty = findViewById(R.id.tvTotalPrintQty)

        setupSpinner()
        setupRecyclerView()
        setupSearch()

        loadData()
    }

    private fun setupSpinner() {
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, searchColumns)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSearchColumn.adapter = spinnerAdapter
    }

    private fun setupRecyclerView() {
        val sharedPrefs = getSharedPreferences(PREFS_ITEM, MODE_PRIVATE)
        val savedViewType = sharedPrefs.getInt(KEY_VIEW_TYPE, ItemAdapter.VIEW_TYPE_FASHION)
        
        adapter = ItemAdapter(emptyList(), savedViewType)
        rvItems.layoutManager = LinearLayoutManager(this)
        rvItems.adapter = adapter
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
        menuInflater.inflate(R.menu.menu_item, menu)
        
        // Make "Clear Item Data" text red
        val clearDataItem = menu.findItem(R.id.action_clear_data)
        clearDataItem?.let {
            val s = SpannableString(it.title)
            s.setSpan(ForegroundColorSpan(Color.RED), 0, s.length, 0)
            it.title = s
        }
        
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_upload_csv -> {
                selectCsvLauncher.launch("text/comma-separated-values")
                true
            }
            R.id.action_download_template -> {
                downloadTemplate()
                true
            }
            R.id.action_clear_data -> {
                confirmClearData()
                true
            }
            R.id.action_view_simple -> {
                saveViewType(ItemAdapter.VIEW_TYPE_SIMPLE)
                true
            }
            R.id.action_view_fashion -> {
                saveViewType(ItemAdapter.VIEW_TYPE_FASHION)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun saveViewType(viewType: Int) {
        val sharedPrefs = getSharedPreferences(PREFS_ITEM, MODE_PRIVATE)
        sharedPrefs.edit { putInt(KEY_VIEW_TYPE, viewType) }
        adapter.setViewType(viewType)
    }

    private fun loadData() {
        val query = etSearch.text.toString()
        val column = spinnerSearchColumn.selectedItem.toString()
        
        val items = itemRepository.searchItems(query, if (column == "All") null else column)
        adapter.updateData(items)

        // Update summaries
        tvTotalRows.text = items.size.toString()
        tvTotalStockQty.text = items.sumOf { it.stockQty }.toString()
        tvTotalPrintQty.text = items.sumOf { it.printQty }.toString()
    }

    private fun confirmClearData() {
        AlertDialog.Builder(this)
            .setTitle("Clear Item Data")
            .setMessage("Are you sure you want to delete all item data? This action cannot be undone.")
            .setPositiveButton("Clear") { _, _ ->
                itemRepository.deleteAll()
                loadData()
                Toast.makeText(this, "All item data cleared", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun downloadTemplate() {
        try {
            val delimiterName = prefs.getString(SettingActivity.KEY_CSV_DELIMITER, CsvDelimiter.COMMA.name)
            val selectedDelimiter = CsvDelimiter.entries.find { it.name == delimiterName } ?: CsvDelimiter.COMMA
            val delimiterChar = selectedDelimiter.character
            
            val content = ITEM_CSV_HEADER.replace('|', delimiterChar)
            val fileName = "item_template.csv"
            
            val file = File(cacheDir, fileName)
            FileOutputStream(file).use { 
                it.write(content.toByteArray())
            }
            
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Download Template"))
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun importItemCsv(uri: Uri) {
        lifecycleScope.launch {
            try {
                val (itemsWithBarcodes, failedCount) = withContext(Dispatchers.IO) {
                    parseItemCsv(uri)
                }
                
                if (itemsWithBarcodes.isEmpty()) {
                    val msg = if (failedCount > 0) "Gagal: $failedCount baris tidak valid" else "File CSV kosong atau format salah"
                    showErrorDialog("Impor Gagal", msg)
                    return@launch
                }

                withContext(Dispatchers.IO) {
                    itemRepository.insertOrUpdateBatch(itemsWithBarcodes)
                }

                val message = if (failedCount > 0) {
                    "Berhasil impor ${itemsWithBarcodes.size} item. ($failedCount baris mismatch diabaikan)"
                } else {
                    "Berhasil mengimpor ${itemsWithBarcodes.size} item"
                }
                
                Toast.makeText(this@ItemActivity, message, Toast.LENGTH_LONG).show()
                loadData()
            } catch (e: Exception) {
                e.printStackTrace()
                showErrorDialog("Error Impor CSV", e.message ?: "Terjadi kesalahan yang tidak diketahui")
            }
        }
    }

    private fun parseItemCsv(uri: Uri): Pair<List<Pair<Item, Barcode>>, Int> {
        val result = mutableListOf<Pair<Item, Barcode>>()
        var failedCount = 0

        val delimiterName = prefs.getString(SettingActivity.KEY_CSV_DELIMITER, CsvDelimiter.COMMA.name)
        val selectedDelimiter = CsvDelimiter.entries.find { it.name == delimiterName } ?: CsvDelimiter.COMMA
        val delimiterChar = selectedDelimiter.character

        contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                val header = reader.readLine()
                val expectedHeader = ITEM_CSV_HEADER.replace('|', delimiterChar)

                if (header == null) {
                    throw Exception("File CSV kosong.")
                }

                if (header.trim() != expectedHeader) {
                    throw Exception("Format Header CSV tidak valid.\n\nPastikan header sesuai:\n$expectedHeader")
                }

                var line: String? = reader.readLine()
                while (line != null) {
                    if (line.isBlank()) {
                        line = reader.readLine()
                        continue
                    }

                    val tokens = line.split(delimiterChar)

                    if (tokens.size >= 2) {
                        try {
                            val barcodeStr = tokens[COL_BARCODE].trim()
                            val itemIdStr = tokens[COL_ITEM_ID].trim()
                            
                            if (barcodeStr.isEmpty() || itemIdStr.isEmpty()) {
                                failedCount++
                            } else {
                                val item = Item(
                                    itemId = itemIdStr,
                                    article = tokens.getOrElse(COL_ARTICLE) { "" }.trim(),
                                    material = tokens.getOrElse(COL_MATERIAL) { "" }.trim(),
                                    color = tokens.getOrElse(COL_COLOR) { "" }.trim(),
                                    size = tokens.getOrElse(COL_SIZE) { "" }.trim(),
                                    name = tokens.getOrElse(COL_NAME) { "" }.trim(),
                                    description = tokens.getOrElse(COL_DESCRIPTION) { "" }.trim(),
                                    category = tokens.getOrElse(COL_CATEGORY) { "" }.trim(),
                                    price = tokens.getOrElse(COL_PRICE) { "0" }.trim().replace(",", ".").toDoubleOrNull() ?: 0.0,
                                    sellPrice = tokens.getOrElse(COL_SELL_PRICE) { "0" }.trim().replace(",", ".").toDoubleOrNull() ?: 0.0,
                                    discount = tokens.getOrElse(COL_DISCOUNT) { "0" }.trim().replace(",", ".").toDoubleOrNull() ?: 0.0,
                                    isSpecialPrice = tokens.getOrElse(COL_IS_SPECIAL_PRICE) { "" }.trim().lowercase().let { it == "true" || it == "1" || it == "yes" },
                                    stockQty = tokens.getOrElse(COL_STOCK_QTY) { "0" }.trim().toIntOrNull() ?: 0,
                                    printQty = tokens.getOrElse(COL_PRINT_QTY) { "0" }.trim().toIntOrNull() ?: 0,
                                    pricingId = tokens.getOrElse(COL_PRICING_ID) { "" }.trim()
                                )
                                
                                val barcode = Barcode(
                                    barcode = barcodeStr,
                                    itemId = itemIdStr
                                )
                                
                                result.add(Pair(item, barcode))
                            }
                        } catch (e: Exception) {
                            failedCount++
                        }
                    } else {
                        failedCount++
                    }
                    line = reader.readLine()
                }
            }
        }
        return Pair(result, failedCount)
    }

    private fun showErrorDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
}
