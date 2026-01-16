package com.ferrine.stockopname.ui.scanneditem

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ferrine.stockopname.BaseDrawerActivity
import com.ferrine.stockopname.R
import com.ferrine.stockopname.data.model.WorkingTypes
import com.ferrine.stockopname.data.repository.OpnameRowRepository
import com.ferrine.stockopname.ui.item.ItemActivity
import com.ferrine.stockopname.ui.item.ItemAdapter
import com.ferrine.stockopname.ui.setting.SettingActivity

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
        
        val workingTypeName = prefs.getString(SettingActivity.KEY_WORKING_TYPE, WorkingTypes.NONE.name)
        val workingType = try {
            WorkingTypes.valueOf(workingTypeName ?: WorkingTypes.NONE.name)
        } catch (e: Exception) {
            WorkingTypes.NONE
        }

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

    private fun loadData() {
        val workingTypeName = prefs.getString(SettingActivity.KEY_WORKING_TYPE, WorkingTypes.NONE.name)
        val workingType = try {
            WorkingTypes.valueOf(workingTypeName ?: WorkingTypes.NONE.name)
        } catch (e: Exception) {
            WorkingTypes.NONE
        }

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
}
