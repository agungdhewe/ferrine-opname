package com.ferrine.stockopname.ui.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.ferrine.stockopname.BaseDrawerActivity
import com.ferrine.stockopname.R
import com.ferrine.stockopname.data.model.WorkingTypes
import com.ferrine.stockopname.data.repository.ItemRepository
import com.ferrine.stockopname.data.repository.OpnameRowRepository
import com.ferrine.stockopname.ui.opname.OpnameActivity
import com.ferrine.stockopname.ui.printlabel.PrintlabelActivity
import com.ferrine.stockopname.ui.receiving.ReceivingActivity
import com.ferrine.stockopname.ui.setting.SettingActivity
import com.ferrine.stockopname.ui.transfer.TransferActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : BaseDrawerActivity() {

    private lateinit var tvSiteCode: TextView
    private lateinit var tvBrandCode: TextView
    private lateinit var tvItemCount: TextView
    private lateinit var tvOpnameCount: TextView
    private lateinit var btnStart: Button
    private lateinit var btnDownloadDb: Button
    private lateinit var btnClearCollectedData: Button

    private val itemRepository by lazy { ItemRepository(this) }
    private val opnameRowRepository by lazy { OpnameRowRepository(this) }

    private val prefs by lazy {
        getSharedPreferences(SettingActivity.PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val createDbLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/x-sqlite3")) { uri: Uri? ->
        uri?.let {
            exportDatabase(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setupDrawer(toolbar)

        bindView()
        setupListeners()
    }

    private fun bindView() {
        tvSiteCode = findViewById(R.id.tvSiteCode)
        tvBrandCode = findViewById(R.id.tvBrandCode)
        tvItemCount = findViewById(R.id.tvItemCount)
        tvOpnameCount = findViewById(R.id.tvOpnameCount)
        btnStart = findViewById(R.id.btnStart)
        btnDownloadDb = findViewById(R.id.btnDownloadDb)
        btnClearCollectedData = findViewById(R.id.btnClearCollectedData)
    }

    private fun setupListeners() {
        btnStart.setOnClickListener {
            startWorkingActivity()
        }
        btnDownloadDb.setOnClickListener {
            createDbLauncher.launch("stockopname.db")
        }
        btnClearCollectedData.setOnClickListener {
            showClearCollectedDataDialog()
        }
    }

    private fun startWorkingActivity() {
        val workingTypeName = prefs.getString(SettingActivity.KEY_WORKING_TYPE, WorkingTypes.NONE.name)
        val workingType = try {
            WorkingTypes.valueOf(workingTypeName ?: WorkingTypes.NONE.name)
        } catch (e: Exception) {
            WorkingTypes.NONE
        }

        val intent = when (workingType) {
            WorkingTypes.OPNAME -> Intent(this, OpnameActivity::class.java)
            WorkingTypes.RECEIVING -> Intent(this, ReceivingActivity::class.java)
            WorkingTypes.TRANSFER -> Intent(this, TransferActivity::class.java)
            WorkingTypes.PRINTLABEL -> Intent(this, PrintlabelActivity::class.java)
            else -> {
                Toast.makeText(this, "Silahkan pilih Working Type di menu Setting", Toast.LENGTH_SHORT).show()
                null
            }
        }
        intent?.let { startActivity(it) }
    }

    private fun showClearCollectedDataDialog() {
        val input = EditText(this)
        input.hint = "type 'clear data' here"
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        lp.setMargins(48, 20, 48, 0)
        input.layoutParams = lp
        container.addView(input)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Clear Collected Data")
            .setMessage("Data opname yang telah dikumpulkan akan dihapus permanen. Ketik \"clear data\" untuk melanjutkan.")
            .setView(container)
            .setNegativeButton("Batal", null)
            .setPositiveButton("Lanjut Delete") { _, _ ->
                val typedText = input.text.toString()
                if (typedText == "clear data") {
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) { opnameRowRepository.deleteAll() }
                        updateCounts()
                        Toast.makeText(this@MainActivity, "Data opname berhasil dihapus", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@MainActivity, "Konfirmasi salah, data tidak dihapus", Toast.LENGTH_SHORT).show()
                }
            }
            .create()

        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(resources.getColor(android.R.color.holo_red_dark))
    }

    override fun onResume() {
        super.onResume()
        displaySettings()
        updateCounts()
    }

    private fun displaySettings() {
        val siteCode = prefs.getString(SettingActivity.KEY_SITE_CODE, "-") ?: "-"
        val brandCode = prefs.getString(SettingActivity.KEY_BRAND_CODE, "-") ?: "-"
        val workingTypeName = prefs.getString(SettingActivity.KEY_WORKING_TYPE, WorkingTypes.NONE.name)
        
        val workingType = try {
            WorkingTypes.valueOf(workingTypeName ?: WorkingTypes.NONE.name)
        } catch (e: Exception) {
            WorkingTypes.NONE
        }

        if (workingType == WorkingTypes.NONE || sessionManager.isAdmin) {
            supportActionBar?.title = "Main"
            btnStart.visibility = View.GONE
        } else {
            supportActionBar?.title = workingType.displayName
            btnStart.visibility = View.VISIBLE
            btnStart.text = "START ${workingType.displayName.uppercase()}"
        }

        tvSiteCode.text = siteCode
        tvBrandCode.text = brandCode
    }

    private fun updateCounts() {
        val workingTypeName = prefs.getString(SettingActivity.KEY_WORKING_TYPE, WorkingTypes.NONE.name)
        val workingType = try {
            WorkingTypes.valueOf(workingTypeName ?: WorkingTypes.NONE.name)
        } catch (e: Exception) {
            WorkingTypes.NONE
        }

        lifecycleScope.launch {
            val itemCount = withContext(Dispatchers.IO) { itemRepository.getCount() }
            val opnameCount = withContext(Dispatchers.IO) { opnameRowRepository.getCount(workingType) }
            
            tvItemCount.text = itemCount.toString()
            tvOpnameCount.text = opnameCount.toString()
        }
    }

    private fun exportDatabase(uri: Uri) {
        lifecycleScope.launch {
            try {
                val dbFile = getDatabasePath("stockopname.db")
                if (!dbFile.exists()) {
                    Toast.makeText(this@MainActivity, "Database tidak ditemukan", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                withContext(Dispatchers.IO) {
                    contentResolver.openOutputStream(uri)?.use { outputStream ->
                        dbFile.inputStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                }
                Toast.makeText(this@MainActivity, "Database berhasil diekspor", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@MainActivity, "Gagal mengekspor database: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun drawerIconColor(): Int {
        return android.R.color.black
    }
}
