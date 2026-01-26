package com.ferrine.stockopname.ui

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import com.ferrine.stockopname.BaseScannerActivity
import com.ferrine.stockopname.R
import com.google.android.material.button.MaterialButton

class ScannerBarcodeFragment : Fragment() {

	private lateinit var etBarcode: EditText
	private lateinit var btnSubmitBarcode: MaterialButton

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		val view = inflater.inflate(R.layout.fragment_scanner_barcode, container, false)
		etBarcode = view.findViewById(R.id.etBarcode)
		btnSubmitBarcode = view.findViewById(R.id.btnSubmitBarcode)

		setupBarcodeScanner()

		return view
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		etBarcode.requestFocus()
		setupKeyboardToggle()
	}

	private fun setupBarcodeScanner() {
		etBarcode.showSoftInputOnFocus = false
		etBarcode.setOnEditorActionListener { _, actionId, event ->
			if (actionId == EditorInfo.IME_ACTION_DONE ||
				(event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
			) {
				processBarcode()
				true
			} else {
				false
			}
		}

		btnSubmitBarcode.setOnClickListener {
			processBarcode()
            showKeyboard()
		}
	}

	private fun processBarcode() {
		val barcode = etBarcode.text.toString().trim()
		if (barcode.isNotEmpty()) {
			(activity as? BaseScannerActivity)?.findBarcode(barcode)
		}
	}


    private fun showKeyboard() {
        val window = requireActivity().window
        val controller = WindowInsetsControllerCompat(window, etBarcode)
        controller.show(WindowInsetsCompat.Type.ime())
    }

	private fun setupKeyboardToggle() {
		(activity as? BaseScannerActivity)?.btnShowKeyboard?.setOnClickListener {
			etBarcode.requestFocus()
			val window = requireActivity().window
			val controller = WindowInsetsControllerCompat(window, etBarcode)

			val isKeyboardVisible = ViewCompat.getRootWindowInsets(etBarcode)
				?.isVisible(WindowInsetsCompat.Type.ime()) ?: false

			if (isKeyboardVisible) {
				controller.hide(WindowInsetsCompat.Type.ime())
			} else {
				controller.show(WindowInsetsCompat.Type.ime())
			}
		}
	}

	fun setHold(hold: Boolean) {
		val btnShowKeyboard = (activity as? BaseScannerActivity)?.btnShowKeyboard
		if (hold) {
			etBarcode.isEnabled = false
			btnSubmitBarcode.isEnabled = false
			btnShowKeyboard?.isEnabled = false
			etBarcode.setText("Wait...")
		} else {
			etBarcode.isEnabled = true
			btnSubmitBarcode.isEnabled = true
			btnShowKeyboard?.isEnabled = true
			etBarcode.text.clear()
			etBarcode.requestFocus()
		}
	}
}
