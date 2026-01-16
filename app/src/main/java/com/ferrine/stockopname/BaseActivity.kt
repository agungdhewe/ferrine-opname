package com.ferrine.stockopname

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.ferrine.stockopname.ui.login.LoginActivity
import com.ferrine.stockopname.utils.SessionManager

abstract class BaseActivity : AppCompatActivity() {

	lateinit var sessionManager: SessionManager
		private set

	override fun onCreate(savedInstanceState: Bundle?) {
		// harus menggunakan Light Theme
		AppCompatDelegate.setDefaultNightMode(
			AppCompatDelegate.MODE_NIGHT_NO
		)

		super.onCreate(savedInstanceState)

		// Inisialisasi SessionManager SEKALI untuk semua Activity
		sessionManager = SessionManager(this)
	}

	/**
	 * Wajib dipanggil oleh Activity yang membutuhkan login
	 */
	protected fun requireLogin() {
		if (!sessionManager.isLoggedIn()) {
			redirectToLogin()
		}
	}


	/**
	 * Menutup keyboard secara paksa
	 */
	protected fun hideKeyboard() {
		val view = this.currentFocus
		if (view != null) {
			val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
			imm.hideSoftInputFromWindow(view.windowToken, 0)
		}
	}

	/**
	 * Redirect ke LoginActivity dengan clear task
	 * Digunakan saat:
	 * - User belum login
	 * - Session expired
	 * - Logout
	 */
	protected fun redirectToLogin() {
		val intent = Intent(this, LoginActivity::class.java)
		intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
		startActivity(intent)
		finish()
	}
}
