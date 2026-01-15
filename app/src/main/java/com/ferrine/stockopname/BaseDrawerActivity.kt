package com.ferrine.stockopname

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.ferrine.stockopname.data.model.WorkingTypes
import com.ferrine.stockopname.ui.item.ItemActivity
import com.ferrine.stockopname.ui.main.MainActivity
import com.ferrine.stockopname.ui.opname.OpnameActivity
import com.ferrine.stockopname.ui.printlabel.PrintlabelActivity
import com.ferrine.stockopname.ui.receiving.ReceivingActivity
import com.ferrine.stockopname.ui.setting.SettingActivity
import com.ferrine.stockopname.ui.transfer.TransferActivity
import com.ferrine.stockopname.ui.user.UserActivity

abstract class BaseDrawerActivity : BaseActivity(),
	NavigationView.OnNavigationItemSelectedListener {

	protected lateinit var drawerLayout: DrawerLayout
	protected lateinit var navigationView: NavigationView

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		// child activity WAJIB memanggil setContentView lebih dulu

		onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
			override fun handleOnBackPressed() {
				if (drawerLayout.isDrawerOpen(navigationView)) {
					drawerLayout.closeDrawers()
				} else if (this@BaseDrawerActivity !is MainActivity) {
					val intent = Intent(this@BaseDrawerActivity, MainActivity::class.java)
					intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
					startActivity(intent)
					finish()
				} else {
					isEnabled = false
					onBackPressedDispatcher.onBackPressed()
				}
			}
		})
	}

	override fun onResume() {
		super.onResume()
		if (::navigationView.isInitialized) {
			filterMenuByWorkingType()
		}
	}

	protected open fun drawerIconColor(): Int {
		return android.R.color.white   // DEFAULT PUTIH
	}

	protected fun setupDrawer(toolbar: androidx.appcompat.widget.Toolbar) {
		drawerLayout = findViewById(R.id.drawerLayout)
		navigationView = findViewById(R.id.navigationView)

		setSupportActionBar(toolbar)

		val toggle = ActionBarDrawerToggle(
			this,
			drawerLayout,
			toolbar,
			0,
			0
		)
		drawerLayout.addDrawerListener(toggle)
		toggle.syncState()

		toggle.drawerArrowDrawable.color = ContextCompat.getColor(this, drawerIconColor())

		filterMenuByWorkingType()
		setupHeader()
		navigationView.setNavigationItemSelectedListener(this)
	}

	private fun filterMenuByWorkingType() {
		val prefs = getSharedPreferences(SettingActivity.PREFS_NAME, Context.MODE_PRIVATE)
		val workingType = prefs.getString(SettingActivity.KEY_WORKING_TYPE, WorkingTypes.NONE.name)

		val menu = navigationView.menu
        if (sessionManager.isAdmin) {
            menu.findItem(R.id.menu_opname).isVisible = false
            menu.findItem(R.id.menu_receiving).isVisible = false
            menu.findItem(R.id.menu_transfer).isVisible = false
            menu.findItem(R.id.menu_print_label).isVisible = false
        } else {
            menu.findItem(R.id.menu_opname).isVisible = workingType == WorkingTypes.OPNAME.name
            menu.findItem(R.id.menu_receiving).isVisible = workingType == WorkingTypes.RECEIVING.name
            menu.findItem(R.id.menu_transfer).isVisible = workingType == WorkingTypes.TRANSFER.name
            menu.findItem(R.id.menu_print_label).isVisible = workingType == WorkingTypes.PRINTLABEL.name
        }

		// Menu Item muncul untuk semua user
		menu.findItem(R.id.menu_item).isVisible = true

		// Menu User hanya muncul untuk Admin
		menu.findItem(R.id.menu_user).isVisible = sessionManager.isAdmin
		
		// Menu Setting muncul untuk semua user (seperti yang direvisi)
		menu.findItem(R.id.menu_setting).isVisible = true
	}

	private fun setupHeader() {
		val headerView = navigationView.getHeaderView(0)
		val tvUsername = headerView.findViewById<android.widget.TextView>(R.id.tvUsername)
		tvUsername.text = sessionManager.username ?: "Unknown User"
	}

	override fun onNavigationItemSelected(item: android.view.MenuItem): Boolean {
		when (item.itemId) {
			R.id.menu_home -> navigate(MainActivity::class.java)
			R.id.menu_item -> navigate(ItemActivity::class.java)
			R.id.menu_opname -> navigate(OpnameActivity::class.java)
			R.id.menu_receiving -> navigate(ReceivingActivity::class.java)
			R.id.menu_transfer -> navigate(TransferActivity::class.java)
			R.id.menu_print_label -> navigate(PrintlabelActivity::class.java)
			R.id.menu_user -> navigate(UserActivity::class.java)
			R.id.menu_setting -> navigate(SettingActivity::class.java)
			R.id.menu_logout -> showLogoutDialog()
		}
		drawerLayout.closeDrawers()
		return true
	}

	protected fun navigate(target: Class<*>) {
		if (this::class.java != target) {
			val intent = Intent(this, target)
			intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
			startActivity(intent)

			// Jika pindah ke SettingActivity atau UserActivity, JANGAN finish activity saat ini
			// supaya bisa kembali ke activity sebelumnya saat tombol back ditekan.
			if (this !is MainActivity && 
				target != SettingActivity::class.java && 
				target != UserActivity::class.java &&
				target != ItemActivity::class.java) {
				finish()
			}
		}
	}

	private fun showLogoutDialog() {
		AlertDialog.Builder(this)
			.setTitle("Logout")
			.setMessage("Apakah Anda yakin ingin logout?")
			.setPositiveButton("Ya") { _, _ ->
				sessionManager.logout()
				redirectToLogin()
				finishAffinity()
			}
			.setNegativeButton("Batal") { dialog, _ ->
				dialog.dismiss()
			}
			.setCancelable(true)
			.show()
	}
}
