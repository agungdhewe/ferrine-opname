package com.ferrine.stockopname.ui.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.ferrine.stockopname.R
import com.ferrine.stockopname.data.db.AppDatabaseHelper
import com.ferrine.stockopname.data.model.User
import com.ferrine.stockopname.data.model.WorkingTypes
import com.ferrine.stockopname.data.repository.UserRepository
import com.ferrine.stockopname.ui.main.MainActivity
import com.ferrine.stockopname.ui.setting.SettingActivity
import com.ferrine.stockopname.utils.SessionManager
import com.google.android.material.textfield.TextInputEditText

class LoginActivity : AppCompatActivity() {

	private lateinit var etUsername: TextInputEditText
	private lateinit var etPassword: TextInputEditText
	private lateinit var btnLogin: Button
	private lateinit var tvSetting: Button
	private lateinit var tvWorkingTypeTitle: TextView

	private lateinit var sessionManager: SessionManager
	private lateinit var userRepository: UserRepository

	private val prefs by lazy {
		getSharedPreferences(SettingActivity.PREFS_NAME, Context.MODE_PRIVATE)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
		super.onCreate(savedInstanceState)

		sessionManager = SessionManager(this)
		val dbHelper = AppDatabaseHelper(this)
		userRepository = UserRepository(dbHelper)

		if (sessionManager.isLoggedIn()) {
			navigateToMain()
			return
		}

		setContentView(R.layout.activity_login)
		initView()
		initAction()
		displayWorkingType()
	}

	private fun initView() {
		etUsername = findViewById(R.id.etUsername)
		etPassword = findViewById(R.id.etPassword)
		btnLogin = findViewById(R.id.btnLogin)
		tvSetting = findViewById(R.id.tvSetting)
		tvWorkingTypeTitle = findViewById(R.id.tvWorkingTypeTitle)
		etUsername.requestFocus()
	}

	private fun displayWorkingType() {
		val workingTypeName = prefs.getString(SettingActivity.KEY_WORKING_TYPE, WorkingTypes.NONE.name)
		val workingType = WorkingTypes.entries.find { it.name == workingTypeName } ?: WorkingTypes.NONE
		tvWorkingTypeTitle.text = workingType.displayName
		tvWorkingTypeTitle.visibility = if (workingType == WorkingTypes.NONE) View.GONE else View.VISIBLE
	}

	private fun initAction() {
		etUsername.setOnEditorActionListener { _, _, _ ->
			val username = etUsername.text.toString().trim()
			if (username.isEmpty()) {
				etUsername.error = "Username wajib diisi"
				etUsername.requestFocus()
			} else {
				etPassword.requestFocus()
			}
			true
		}

		etPassword.setOnEditorActionListener { _, _, _ ->
			val username = etUsername.text.toString().trim()
			val password = etPassword.text.toString().trim()
			if (validateInput(username, password)) {
				doLogin(username, password)
			}
			true
		}

		btnLogin.setOnClickListener {
			val username = etUsername.text.toString().trim()
			val password = etPassword.text.toString().trim()
			if (validateInput(username, password)) {
				doLogin(username, password)
			}
		}

		tvSetting.setOnClickListener {
			val intent = Intent(this, SettingActivity::class.java)
			startActivity(intent)
		}
	}

	override fun onResume() {
		super.onResume()
		displayWorkingType()
	}

	private fun validateInput(username: String, password: String): Boolean {
		if (username.isEmpty()) {
			etUsername.error = "Username wajib diisi"
			etUsername.requestFocus()
			return false
		}
		if (password.isEmpty()) {
			etPassword.error = "Password wajib diisi"
			etPassword.requestFocus()
			return false
		}
		return true
	}

	private fun doLogin(username: String, pass: String) {
		val user = userRepository.login(username, pass)
		if (user != null) {
			if (checkAuthorization(user)) {
				sessionManager.createLoginSession(user)
				Toast.makeText(this, "Login berhasil sebagai ${user.fullname}", Toast.LENGTH_SHORT).show()
				navigateToMain()
			} else {
				Toast.makeText(this, "Anda tidak memiliki akses untuk working type ini", Toast.LENGTH_LONG).show()
			}
		} else {
			Toast.makeText(this, "Username atau password salah", Toast.LENGTH_SHORT).show()
		}
	}

	private fun checkAuthorization(user: User): Boolean {
		if (user.isAdmin) return true
		
		val workingTypeName = prefs.getString(SettingActivity.KEY_WORKING_TYPE, WorkingTypes.NONE.name)
		return when (workingTypeName) {
			WorkingTypes.OPNAME.name -> user.allowOpname
			WorkingTypes.RECEIVING.name -> user.allowReceiving
			WorkingTypes.TRANSFER.name -> user.allowTransfer
			WorkingTypes.PRINTLABEL.name -> user.allowPrintlabel
			else -> false
		}
	}

	private fun navigateToMain() {
		startActivity(Intent(this, MainActivity::class.java))
		finish()
	}
}
