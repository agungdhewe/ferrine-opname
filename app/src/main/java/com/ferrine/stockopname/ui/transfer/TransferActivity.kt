package com.ferrine.stockopname.ui.transfer

import android.os.Bundle
import com.ferrine.stockopname.BaseScannerActivity
import com.ferrine.stockopname.R
import com.ferrine.stockopname.data.model.WorkingTypes

class TransferActivity : BaseScannerActivity() {

    override var currentWorkingType: WorkingTypes = WorkingTypes.TRANSFER

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanner)

        setupDrawer(findViewById(R.id.toolbar))
        supportActionBar?.title = "Transfer"
        setupScanner()
    }
}
