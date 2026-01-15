package com.ferrine.stockopname.ui.item

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
import com.ferrine.stockopname.data.repository.ItemRepository

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
        adapter = ItemAdapter(emptyList())
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
}
