package com.ferrine.stockopname.ui.receiving

import android.os.Bundle
import com.ferrine.stockopname.BaseScannerActivity
import com.ferrine.stockopname.R
import com.ferrine.stockopname.data.model.WorkingTypes

class ReceivingActivity : BaseScannerActivity() {

    override var currentWorkingType: WorkingTypes = WorkingTypes.RECEIVING

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanner)

        setupDrawer(findViewById(R.id.toolbar))
        supportActionBar?.title = "Receiving"
        setupScanner()
    }
}
