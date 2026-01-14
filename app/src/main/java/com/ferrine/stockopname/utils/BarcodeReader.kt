package com.ferrine.stockopname.utils

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.ferrine.stockopname.R
import com.ferrine.stockopname.data.model.Label
import com.ferrine.stockopname.BaseScannerActivity
import com.ferrine.stockopname.data.model.Item
import com.ferrine.stockopname.data.model.PrintLabelMode
import com.ferrine.stockopname.ui.ScannerBarcodeFragment
import com.ferrine.stockopname.ui.ScannerCameraFragment
import com.ferrine.stockopname.ui.ScannerResultFragment
import com.ferrine.stockopname.ui.setting.SettingActivity
import kotlinx.coroutines.launch

class BarcodeReader(private val activity: BaseScannerActivity) {

	private val bluetoothPrintManager by lazy { BluetoothPrintManager(activity) }
	private var currentToast: Toast? = null


	fun findBarcode(barcode: String, mode: PrintLabelMode) {

		currentToast?.cancel()
		currentToast = Toast.makeText(activity, "getting data barcode $barcode", Toast.LENGTH_SHORT)
		currentToast?.show()

		activity.lifecycleScope.launch {
			holdBarcodeReader(true)
			val resultFragment =
				activity.supportFragmentManager.findFragmentById(R.id.fragment_scanner_result) as? ScannerResultFragment
			resultFragment?.setError(null)

			try {
				if (barcode.equals("TM34567890123")) {
					soundBarcodeNotFound()
					resultFragment?.setError("Barcode <b>\"$barcode\"</b> tidak ditemukan".toHtml())
					return@launch
				}

				// Simulasi Data (Idealnya ini dari Repository/API)
				val item = Item(
					itemId = barcode,
					article = "ART-12345",
					material = "Cotton Combed 30s",
					color = "Black",
					name = "Dummy T-Shirt Limited Edition",
					description = "Kaos berkualitas tinggi",
					category = "Apparel",
					price = 150000.0,
					stockQty = 10,
					printQty = 1
				)
				val label = BarcodeLabel.createBarcodeLabel(barcode, item)

				soundBarcodeFound()

				when (mode) {
					PrintLabelMode.PRINT_LABEL -> {
						printBarcode(label)
					}

					PrintLabelMode.OPNAME -> {

					}

					PrintLabelMode.RECEIVING -> {
					}

					PrintLabelMode.TRANSFER -> {
					}
				}

			} catch (e: Exception) {
				resultFragment?.setError(e.message ?: "Gagal memproses barcode")
			} finally {
				holdBarcodeReader(false)
			}
		}
	}

	private suspend fun printBarcode(label: Label) {
		val prefs = activity.getSharedPreferences(SettingActivity.PREFS_NAME, Context.MODE_PRIVATE)
		val printerPrefix = prefs.getString(SettingActivity.KEY_PRINTER_PREFIX, "")

		if (printerPrefix.isNullOrEmpty()) {
			throw Exception("Printer belum diatur di Setting")
		}

		val tsplCommand = BarcodeLabel.generateTspl(label)

		bluetoothPrintManager.sendToPrinter(printerPrefix, tsplCommand)
	}

	fun holdBarcodeReader(hold: Boolean) {
		if (activity.isUseCamera) {
			val cameraFragment =
				activity.supportFragmentManager.findFragmentById(R.id.fragment_scanner_camera) as? ScannerCameraFragment
			cameraFragment?.setHold(hold)
		} else {
			val barcodeFragment =
				activity.supportFragmentManager.findFragmentById(R.id.fragment_scanner_barcode) as? ScannerBarcodeFragment
			barcodeFragment?.setHold(hold)
		}
	}

	fun soundBarcodeFound() {
		try {
			val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
			toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
		} catch (e: Exception) {
			Log.e("PrintlabelCamera", "Failed to play beep: ${e.message}")
		}
	}

	fun soundBarcodeNotFound() {
		try {
			// Menggunakan STREAM_ALARM agar suara lebih keras/menggelegar
			val toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 100)
			// TONE_SUP_ERROR memberikan suara alert error yang tegas
			toneGen.startTone(ToneGenerator.TONE_SUP_ERROR, 1000)
		} catch (e: Exception) {
			Log.e("PrintlabelCamera", "Failed to play alert: ${e.message}")
		}
	}
}
