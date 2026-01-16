package com.ferrine.stockopname.ui

import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.ferrine.stockopname.R
import com.ferrine.stockopname.data.model.Item
import com.ferrine.stockopname.data.model.WorkingTypes
import com.ferrine.stockopname.ui.item.ItemActivity
import com.ferrine.stockopname.ui.item.ItemAdapter

class ScannerResultFragment : Fragment() {

    private var tvErrorMessage: TextView? = null
    private var resultContent: LinearLayout? = null
    private var btnEditQty: View? = null

    // Properti untuk menyimpan callback atau data terakhir
    var onQtyUpdated: ((Double) -> Unit)? = null

    override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_scanner_result, container, false)
        tvErrorMessage = view.findViewById(R.id.tvErrorMessage)
        resultContent = view.findViewById(R.id.resultContent)
        btnEditQty = view.findViewById(R.id.btnEditQty) // Tambahkan ini

        // Set Listener Klik
        btnEditQty?.setOnClickListener {
            showEditQtyDialog()
        }

        return view
    }

    private fun showEditQtyDialog() {
        val context = context ?: return

        // Buat EditText secara programmatically
        val input = EditText(context).apply {
            // Tipe input: Angka, mengizinkan tanda negatif (-)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED
            hint = "Edit Qty (0 untuk membatalkan scan)"
        }

        // Buat kontainer untuk memberi margin
        val container = android.widget.FrameLayout(context)
        val params = android.widget.FrameLayout.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // Konversi dp ke pixel (48dp)
        val marginInDp = 16
        val marginInPx = (marginInDp * context.resources.displayMetrics.density).toInt()

        params.leftMargin = marginInPx
        params.rightMargin = marginInPx
        input.layoutParams = params

        container.addView(input)

        val dialog = AlertDialog.Builder(context)
            .setTitle("Edit Quantity")
            .setMessage("Masukkan jumlah perubahan stok:")
            .setView(container) // Gunakan container, bukan input langsung
            .setPositiveButton("Simpan") { _, _ ->
                val newValueStr = input.text.toString()
                if (newValueStr.isNotEmpty()) {
                    val newValue = newValueStr.toDoubleOrNull() ?: 0.0
                    onQtyUpdated?.invoke(newValue)
                }
            }
            .setNegativeButton("Batal", null)
            .create()


        dialog.show()
    }


    fun showErrorMessage(message: CharSequence?) {
        tvErrorMessage?.apply {
            if (message.isNullOrEmpty()) {
                visibility = View.GONE
            } else {
                text = message
                visibility = View.VISIBLE
            }
        }
    }


    fun setError(message: CharSequence?) {
        tvErrorMessage?.apply {
            if (message.isNullOrEmpty()) {
                visibility = View.GONE
            } else {
                text = message
                visibility = View.VISIBLE
            }
        }
        if (!message.isNullOrEmpty()) {
            resultContent?.removeAllViews()
            btnEditQty?.visibility = View.GONE // Sembunyikan jika error
        }
    }

    fun setData(barcode: String, item: Item, totalScanned: Double, workingtype: WorkingTypes) {
        setError(null)
        resultContent?.removeAllViews()
        
        val context = context ?: return

        // Tentukan infoQty berdasarkan workingtype
        val infoQty = if (workingtype == WorkingTypes.PRINTLABEL) {
            "${totalScanned.toInt()} / ${item.printQty}"
        } else {
            "${totalScanned.toInt()} / ${item.stockQty}"
        }

        val rowLabelStr = if (workingtype == WorkingTypes.PRINTLABEL) {
            "Printed"
        } else {
            "Scanned"
        }

        // Ambil savedViewType menggunakan konstanta dari ItemActivity
        val sharedPrefs = context.getSharedPreferences(ItemActivity.PREFS_ITEM, Context.MODE_PRIVATE)
        val savedViewType = sharedPrefs.getInt(ItemActivity.KEY_VIEW_TYPE, ItemAdapter.VIEW_TYPE_FASHION)


        // Buat logic pemisahan list details
        val details = if (savedViewType == ItemAdapter.VIEW_TYPE_SIMPLE) {
            // Mode SIMPLE: Hanya menampilkan informasi dasar
            listOf(
                "Barcode" to barcode,
                "ID" to item.itemId,
                rowLabelStr to infoQty,
                "Name" to item.name,
                "Category" to item.category,

            )
        } else {
            // Mode FASHION (Default): Menampilkan informasi lengkap (Art, Material, Col, dll)
            listOf(
                "Barcode" to barcode,
                "ID" to item.itemId,
                rowLabelStr to infoQty,
                "Name" to item.name,
                "Art" to item.article,
                "Material" to item.material,
                "Color" to item.color,
                "Size" to item.size,
                "Category" to item.category,
                "Description" to item.description
            )
        }



        for ((labelStr, value) in details) {
            val tv = TextView(context).apply {
                text = "$labelStr: $value"
                setPadding(0, 4, 0, 4)
            }
            resultContent?.addView(tv)
        }

        // Tampilkan tombol Edit Qty jika bukan mode PRINTLABEL
        btnEditQty?.visibility = if (workingtype != WorkingTypes.PRINTLABEL) {
            View.VISIBLE
        } else {
            View.GONE
        }


    }
}