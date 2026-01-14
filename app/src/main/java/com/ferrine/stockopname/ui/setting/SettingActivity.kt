package com.ferrine.stockopname.ui.setting

import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import com.ferrine.stockopname.R
import com.ferrine.stockopname.data.db.AppDatabaseHelper
import com.ferrine.stockopname.data.model.BarcodeScannerOptions
import com.ferrine.stockopname.data.model.PrinterOptions
import com.ferrine.stockopname.data.model.WorkingTypes
import com.ferrine.stockopname.utils.SessionManager
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class SettingActivity : AppCompatActivity() {

    private lateinit var tilSiteCode: TextInputLayout
    private lateinit var tilBrandCode: TextInputLayout
    private lateinit var tilWorkingType: TextInputLayout
    private lateinit var tilDeviceId: TextInputLayout
    private lateinit var tilServerAddress: TextInputLayout
    
    private lateinit var etSiteCode: TextInputEditText
    private lateinit var etBrandCode: TextInputEditText
    private lateinit var etDeviceId: TextInputEditText
    private lateinit var etServerAddress: TextInputEditText
    private lateinit var cbUseCentralServer: MaterialCheckBox
    
    private lateinit var spWorkingType: AutoCompleteTextView
    private lateinit var spBarcodeReader: AutoCompleteTextView
    private lateinit var spPrinter: AutoCompleteTextView
    private lateinit var btnResetData: Button

    private val sessionManager by lazy { SessionManager(this) }

    private val prefs by lazy {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // harus menggunakan Light Theme
        AppCompatDelegate.setDefaultNightMode(
            AppCompatDelegate.MODE_NIGHT_NO
        )

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        // Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = "Setting"
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        bindView()
        setupSpinners()
        loadSetting()
        setupUIBasedOnLoginStatus()
        setupListeners()
    }

    private fun bindView() {
        tilSiteCode = findViewById(R.id.tilSiteCode)
        tilBrandCode = findViewById(R.id.tilBrandCode)
        tilWorkingType = findViewById(R.id.tilWorkingType)
        tilDeviceId = findViewById(R.id.tilDeviceId)
        tilServerAddress = findViewById(R.id.tilServerAddress)
        
        etSiteCode = findViewById(R.id.etSiteCode)
        etBrandCode = findViewById(R.id.etBrandCode)
        etDeviceId = findViewById(R.id.etDeviceId)
        etServerAddress = findViewById(R.id.etServerAddress)
        cbUseCentralServer = findViewById(R.id.cbUseCentralServer)
        
        spWorkingType = findViewById(R.id.spWorkingType)
        spBarcodeReader = findViewById(R.id.spBarcodeReader)
        spPrinter = findViewById(R.id.spPrinter)
        btnResetData = findViewById(R.id.btnResetData)
    }

    private fun setupListeners() {
        btnResetData.setOnClickListener {
            showResetConfirmationDialog()
        }
        
        cbUseCentralServer.setOnCheckedChangeListener { _, isChecked ->
            updateServerAddressEnableState(isChecked)
        }
    }
    
    private fun updateServerAddressEnableState(isCentralServerChecked: Boolean) {
        val isAdmin = sessionManager.isAdmin
        tilServerAddress.isEnabled = isAdmin && isCentralServerChecked
    }

    private fun showResetConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Reset Data")
            .setMessage("Apakah Anda yakin ingin menghapus semua data transaksi dan master data? Tindakan ini tidak dapat dibatalkan.")
            .setPositiveButton("Ya, Reset") { _, _ ->
                resetData()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun resetData() {
        try {
            val dbHelper = AppDatabaseHelper(this)
            dbHelper.resetDatabase()
            Toast.makeText(this, "Data berhasil direset", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal mereset data: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupUIBasedOnLoginStatus() {
        val isLoggedIn = sessionManager.isLoggedIn()
        val isAdmin = sessionManager.isAdmin
        
        // Disable input jika sudah login, enable jika belum/sudah logout
        tilSiteCode.isEnabled = !isLoggedIn
        tilBrandCode.isEnabled = !isLoggedIn
        tilWorkingType.isEnabled = !isLoggedIn

        // Tambahan fitur Admin
        tilDeviceId.isEnabled = isAdmin
        cbUseCentralServer.isEnabled = isAdmin
        updateServerAddressEnableState(cbUseCentralServer.isChecked)

        // Tombol Reset Data hanya muncul jika Admin dan sudah Login
        btnResetData.visibility = if (isLoggedIn && isAdmin) View.VISIBLE else View.GONE
    }

    private fun setupSpinners() {
        // Working Type
        val workingTypeOptions = WorkingTypes.entries.map { it.displayName }
        val wtAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, workingTypeOptions)
        spWorkingType.setAdapter(wtAdapter)

        // Barcode Reader
        val barcodeOptions = BarcodeScannerOptions.entries.map { it.displayName }
        val brAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, barcodeOptions)
        spBarcodeReader.setAdapter(brAdapter)

        // Printer
        val printerOptions = PrinterOptions.entries.map { it.displayName }
        val pAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, printerOptions)
        spPrinter.setAdapter(pAdapter)
    }

    /**
     * Load setting saat Activity dibuka
     */
    private fun loadSetting() {
        etSiteCode.setText(prefs.getString(KEY_SITE_CODE, ""))
        etBrandCode.setText(prefs.getString(KEY_BRAND_CODE, ""))
        etDeviceId.setText(prefs.getString(KEY_DEVICE_ID, ""))
        
        cbUseCentralServer.isChecked = prefs.getBoolean(KEY_USE_CENTRAL_SERVER, false)
        etServerAddress.setText(prefs.getString(KEY_SERVER_ADDRESS, ""))

        val workingTypeName = prefs.getString(KEY_WORKING_TYPE, WorkingTypes.NONE.name)
        val workingType = WorkingTypes.entries.find { it.name == workingTypeName } ?: WorkingTypes.NONE
        spWorkingType.setText(workingType.displayName, false)

        val barcodeReaderName = prefs.getString(KEY_BARCODE_READER, BarcodeScannerOptions.SCANNER.name)
        val barcodeOption = BarcodeScannerOptions.entries.find { it.name == barcodeReaderName } ?: BarcodeScannerOptions.SCANNER
        spBarcodeReader.setText(barcodeOption.displayName, false)

        val printerPrefix = prefs.getString(KEY_PRINTER_PREFIX, "")
        val printerOption = PrinterOptions.entries.find { it.prefix == printerPrefix } ?: PrinterOptions.NONE
        spPrinter.setText(printerOption.displayName, false)
    }

    /**
     * Simpan setting otomatis
     */
    private fun saveSetting() {
        val workingTypeDisplayName = spWorkingType.text.toString()
        val selectedWorkingType = WorkingTypes.entries.find { it.displayName == workingTypeDisplayName } ?: WorkingTypes.NONE

        val barcodeDisplayName = spBarcodeReader.text.toString()
        val selectedBarcodeReader = BarcodeScannerOptions.entries.find { it.displayName == barcodeDisplayName } ?: BarcodeScannerOptions.SCANNER

        val printerDisplayName = spPrinter.text.toString()
        val selectedPrinter = PrinterOptions.entries.find { it.displayName == printerDisplayName } ?: PrinterOptions.NONE

        prefs.edit().apply {
            putString(KEY_SITE_CODE, etSiteCode.text.toString())
            putString(KEY_BRAND_CODE, etBrandCode.text.toString())
            putString(KEY_DEVICE_ID, etDeviceId.text.toString())
            putBoolean(KEY_USE_CENTRAL_SERVER, cbUseCentralServer.isChecked)
            putString(KEY_SERVER_ADDRESS, etServerAddress.text.toString())
            
            putString(KEY_WORKING_TYPE, selectedWorkingType.name)
            putString(KEY_BARCODE_READER, selectedBarcodeReader.name)
            putString(KEY_PRINTER_PREFIX, selectedPrinter.prefix)
            apply()
        }
    }

    override fun onPause() {
        super.onPause()
        saveSetting() // AUTO SAVE saat Back / Home
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        const val PREFS_NAME = "app_setting"
        const val KEY_SITE_CODE = "site_code"
        const val KEY_BRAND_CODE = "brand_code"
        const val KEY_WORKING_TYPE = "working_type"
        const val KEY_BARCODE_READER = "barcode_reader"
        const val KEY_PRINTER_PREFIX = "printer_prefix"
        const val KEY_DEVICE_ID = "device_id"
        const val KEY_USE_CENTRAL_SERVER = "use_central_server"
        const val KEY_SERVER_ADDRESS = "server_address"
    }
}
