package com.birumuda.stockopname

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.fragment.app.FragmentContainerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.birumuda.stockopname.data.model.BarcodeScannerOptions
import com.birumuda.stockopname.data.model.PrintLabelMode
import com.birumuda.stockopname.ui.ScannerCameraFragment
import com.birumuda.stockopname.ui.setting.SettingActivity
import com.birumuda.stockopname.utils.BarcodeReader

abstract class BaseScannerActivity : BaseDrawerActivity() {

    protected lateinit var containerCamera: FragmentContainerView
    protected lateinit var containerBarcode: FragmentContainerView
    lateinit var btnShowKeyboard: FloatingActionButton

    var isUseCamera = false
        protected set

    protected lateinit var barcodeReader: BarcodeReader
    abstract var currentMode: PrintLabelMode

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Mencegah keyboard muncul otomatis saat Activity dibuka
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
    }

    protected fun setupScanner() {
        containerCamera = findViewById(R.id.fragment_printlabel_camera)
        containerBarcode = findViewById(R.id.fragment_printlabel_barcode)
        btnShowKeyboard = findViewById(R.id.btnShowKeyboard)
        barcodeReader = BarcodeReader(this)
        updateScannerVisibility()
    }

    override fun onResume() {
        super.onResume()
        updateScannerVisibility()
    }

    protected fun updateScannerVisibility() {
        val prefs = getSharedPreferences(SettingActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val barcodeReaderName = prefs.getString("barcode_reader", BarcodeScannerOptions.SCANNER.name)
        isUseCamera = BarcodeScannerOptions.entries.find { it.name == barcodeReaderName }?.isUseCamera ?: false

        val cameraFragment = supportFragmentManager.findFragmentById(R.id.fragment_printlabel_camera) as? ScannerCameraFragment

        if (isUseCamera) {
            containerCamera.visibility = View.VISIBLE
            containerBarcode.visibility = View.GONE
            btnShowKeyboard.visibility = View.GONE
            cameraFragment?.startCamera()
        } else {
            containerCamera.visibility = View.GONE
            containerBarcode.visibility = View.VISIBLE
            btnShowKeyboard.visibility = View.VISIBLE
            cameraFragment?.stopCamera()
        }
    }

    fun findBarcode(barcode: String) {
        barcodeReader.findBarcode(barcode, currentMode)
    }
}
