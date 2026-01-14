package com.ferrine.stockopname

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentContainerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.ferrine.stockopname.data.model.BarcodeScannerOptions
import com.ferrine.stockopname.data.model.PrintLabelMode
import com.ferrine.stockopname.ui.ScannerCameraFragment
import com.ferrine.stockopname.ui.setting.SettingActivity
import com.ferrine.stockopname.utils.BarcodeReader

abstract class BaseScannerActivity : BaseDrawerActivity() {

    protected lateinit var containerCamera: FragmentContainerView
    protected lateinit var containerBarcode: FragmentContainerView
    protected lateinit var tvBoxCode: TextView
    protected lateinit var btnEditContent: ImageButton
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
        containerCamera = findViewById(R.id.fragment_scanner_camera)
        containerBarcode = findViewById(R.id.fragment_scanner_barcode)
        tvBoxCode = findViewById(R.id.tvBoxCode)
        btnEditContent = findViewById(R.id.btnEditContent)
        btnShowKeyboard = findViewById(R.id.btnShowKeyboard)
        barcodeReader = BarcodeReader(this)
        
        btnEditContent.setOnClickListener {
            showEditContentDialog()
        }
        
        updateScannerVisibility()
    }

    private fun showEditContentDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Edit Box / Shelf / Room")

        val input = EditText(this)
        input.setText(tvBoxCode.text.toString())
        input.setSelection(input.text.length)
        input.setSingleLine(true)

        // Membungkus EditText dalam FrameLayout untuk memberikan margin
        val container = FrameLayout(this)
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        val marginPx = (16 * resources.displayMetrics.density).toInt()
        params.marginStart = marginPx
        params.marginEnd = marginPx
        input.layoutParams = params
        container.addView(input)

        builder.setView(container)

        builder.setPositiveButton("OK") { dialog, _ ->
            val newCode = input.text.toString()
            tvBoxCode.text = newCode
            if (newCode.isNotEmpty()) {
                findBarcode(newCode)
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Batal") { dialog, _ ->
            dialog.cancel()
        }
        builder.show()
    }

    override fun onResume() {
        super.onResume()
        updateScannerVisibility()
    }

    protected fun updateScannerVisibility() {
        val prefs = getSharedPreferences(SettingActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val barcodeReaderName = prefs.getString("barcode_reader", BarcodeScannerOptions.SCANNER.name)
        isUseCamera = BarcodeScannerOptions.entries.find { it.name == barcodeReaderName }?.isUseCamera ?: false

        val cameraFragment = supportFragmentManager.findFragmentById(R.id.fragment_scanner_camera) as? ScannerCameraFragment

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
