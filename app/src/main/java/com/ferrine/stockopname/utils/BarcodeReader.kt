package com.ferrine.stockopname.utils

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.ferrine.stockopname.R
import com.ferrine.stockopname.data.model.Label
import com.ferrine.stockopname.BaseScannerActivity
import com.ferrine.stockopname.data.model.Item
import com.ferrine.stockopname.data.model.OpnameRow
import com.ferrine.stockopname.data.model.WorkingTypes
import com.ferrine.stockopname.data.repository.ItemRepository
import com.ferrine.stockopname.data.repository.OpnameRowRepository
import com.ferrine.stockopname.ui.ScannerBarcodeFragment
import com.ferrine.stockopname.ui.ScannerCameraFragment
import com.ferrine.stockopname.ui.ScannerResultFragment
import com.ferrine.stockopname.ui.setting.SettingActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BarcodeReader(private val activity: BaseScannerActivity) {

	private val bluetoothPrintManager by lazy { BluetoothPrintManager(activity) }
	private val itemRepository by lazy { ItemRepository(activity) }
	private val opnameRowRepository by lazy { OpnameRowRepository(activity) }
	private val sessionManager by lazy { SessionManager(activity) }
	private var currentToast: Toast? = null

    private val resultFragment: ScannerResultFragment?
        get() = activity.supportFragmentManager.findFragmentById(R.id.fragment_scanner_result) as? ScannerResultFragment


    fun findBarcode(barcode: String, workingType: WorkingTypes) {

		currentToast?.cancel()
		currentToast = Toast.makeText(activity, "Mencari barcode $barcode...", Toast.LENGTH_SHORT)
		currentToast?.show()

		activity.lifecycleScope.launch {
			holdBarcodeReader(true)
			resultFragment?.setError(null)

			try {
				val item = withContext(Dispatchers.IO) {
					itemRepository.getItemByBarcode(barcode)
				}

				if (item == null) {
					soundBarcodeNotFound()
					resultFragment?.setError("Barcode <b>\"$barcode\"</b> tidak ditemukan".toHtml())
					return@launch
				}


				val prefs = activity.getSharedPreferences(SettingActivity.PREFS_NAME, Context.MODE_PRIVATE)
				val deviceId = prefs.getString(SettingActivity.KEY_DEVICE_ID, "") ?: ""
				val boxCode = activity.findViewById<TextView>(R.id.tvBoxCode)?.text?.toString() ?: ""

				val timestamp = System.currentTimeMillis()
				val row = OpnameRow(
					timestamp = timestamp,
					activity = workingType.name,
					projectId = "", // Bisa diisi jika ada ID proyek/opname yang aktif
					deviceId = deviceId,
					userId = sessionManager.username ?: "",
					barcode = barcode,
					boxcode = boxCode,
					itemId = item.itemId,
					scannedQty = 1,
				)

                // masukkan data OpnameRow ke table
                val rowId = withContext(Dispatchers.IO) {
                    try {
                        opnameRowRepository.insert(row)
                    } catch (e: Exception) {
                        Log.e("BarcodeReader", "Database Insert Error: ${e.message}")
                        -1L // Kembalikan nilai penanda gagal
                    }
                }

                if (rowId == -1L) {
                    soundBarcodeNotFound() // Suara error
                    resultFragment?.setError("Gagal menyimpan data ke database. Cek memori HP!".toHtml())
                    return@launch // Hentikan proses, jangan lanjut ke cetak label
                }

                // Ambil total akumulasi qty yang sudah discan untuk item ini
                val totalScanned = withContext(Dispatchers.IO) {
                    try {
                        // Menggunakan projectId dan itemId dari objek row yang baru dibuat
                        opnameRowRepository.getScannedQty(workingType, row.itemId)
                    } catch (e: Exception) {
                        Log.e("BarcodeReader", "Error getScannedQty: ${e.message}")
                        0.0 // Default jika error
                    }
                }
                
				soundBarcodeFound()

                // Setup callback untuk edit qty menggunakan function yang baru dibuat
                resultFragment?.onQtyUpdated = { newQty ->
                    updateQtyScanned(
                        timestamp = timestamp,
                        newQty = newQty,
                        barcode = barcode,
                        item = item,
                        workingType = workingType
                    )
                }

				resultFragment?.setData(barcode, item, totalScanned, workingType)

				when (workingType) {
					WorkingTypes.PRINTLABEL -> {
                        currentToast?.cancel()
                        currentToast = Toast.makeText(activity, "Printing label $barcode...", Toast.LENGTH_SHORT)
                        currentToast?.show()

						val label = BarcodeLabel.createBarcodeLabel(barcode, item)
						try {
                            printBarcode(label)
                        } catch (e: Exception) {
                            Log.e("BarcodeReader", "Print Error: ${e.message}")
                            // Jika print gagal, batalkan insert
                            withContext(Dispatchers.IO) {
                                opnameRowRepository.cancelInsertedRow(timestamp)
                            }
                            soundBarcodeNotFound()

                            // Update UI untuk menunjukkan qty kembali seperti semula
                            val currentQty = withContext(Dispatchers.IO) {
                                opnameRowRepository.getScannedQty(workingType, item.itemId)
                            }
                            resultFragment?.setData(barcode, item, currentQty, workingType)
                            resultFragment?.showErrorMessage("Gagal Print: ${e.message}. Print label dibatalkan.".toHtml())
                            return@launch
                        }
					}

					WorkingTypes.OPNAME, WorkingTypes.RECEIVING, WorkingTypes.TRANSFER -> {
                        currentToast?.cancel()
                        currentToast = Toast.makeText(activity, "Inserting item $barcode...", Toast.LENGTH_SHORT)
                        currentToast?.show()
					}

					else -> {}
				}

			} catch (e: Exception) {
				Log.e("BarcodeReader", "Error findBarcode: ${e.message}")
				resultFragment?.setError(e.message ?: "Gagal memproses barcode")
			} finally {
				holdBarcodeReader(false)
			}
		}
	}


    private fun updateQtyScanned(
        timestamp: Long,
        newQty: Double,
        barcode: String,
        item: Item,
        workingType: WorkingTypes
    ) {
        activity.lifecycleScope.launch {
            currentToast?.cancel()
            try {

                // Update qty di database berdasarkan timestamp row yang baru dibuat
                withContext(Dispatchers.IO) {
                    opnameRowRepository.updateScannedQty(timestamp, newQty.toInt())
                }

                // Ambil total akumulasi qty terbaru untuk item tersebut
                val totalScanned = withContext(Dispatchers.IO) {
                    opnameRowRepository.getScannedQty(workingType, item.itemId)
                }

                // update tampilan result
                resultFragment?.setData(barcode, item, totalScanned, workingType)
                currentToast = Toast.makeText(activity, "Qty berhasil diupdate", Toast.LENGTH_SHORT)
            } catch (e: Exception) {
                Log.e("BarcodeReader", "Error updateQtyScanned: ${e.message}")
                resultFragment?.setError("Kesalahan Database: ${e.localizedMessage}".toHtml())
                currentToast = Toast.makeText(activity, "Gagal menyimpan perubahan QtyScanned", Toast.LENGTH_LONG)
            } finally {
                currentToast?.show()
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
