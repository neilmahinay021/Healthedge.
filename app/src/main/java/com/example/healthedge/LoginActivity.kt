package com.example.healthedge

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.healthedge.api.ApiClient
import com.example.healthedge.models.LoginRequest
import kotlinx.coroutines.launch
import android.util.Log

class LoginActivity : AppCompatActivity() {
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var registerTextView: TextView
    private lateinit var loginRegisterTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        emailEditText = findViewById(R.id.editTextEmail)
        passwordEditText = findViewById(R.id.editTextPassword)
        loginButton = findViewById(R.id.buttonLogin)
        registerTextView = findViewById(R.id.textViewRegister)
        loginRegisterTextView = findViewById(R.id.textViewLoginRegister)

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            if (email.isNotEmpty() && password.isNotEmpty()) {
                loginUser(email, password)
            } else {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            }
        }

        registerTextView.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        loginRegisterTextView.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun loginUser(email: String, password: String) {
        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.login(LoginRequest(email, password))
                Log.d("LOGIN_DEBUG", "Response: isSuccessful=${response.isSuccessful}, body=${response.body()}, errorBody=${response.errorBody()?.string()}")
                if (response.isSuccessful && response.body()?.success == true) {
                    val user = response.body()?.user
                    if (user != null) {
                        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
                        prefs.edit()
                            .putString("role", user.role)
                            .putInt("user_id", user.id)
                            .putString("user_name", user.name)
                            .putString("name", user.name)
                            .apply()
                        if (user.role == "user") {
                            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        } else if (user.role == "doctor") {
                            startActivity(Intent(this@LoginActivity, DoctorDashboardActivity::class.java))
                        }
                        finish()
                    } else {
                        Log.e("LOGIN_DEBUG", "Login failed: No user data")
                        Toast.makeText(this@LoginActivity, "Login failed: No user data", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val errorMsg = response.body()?.error ?: "Invalid credentials"
                    Log.e("LOGIN_DEBUG", "Login failed: $errorMsg")
                    Toast.makeText(this@LoginActivity, "Login failed: $errorMsg", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("LOGIN_DEBUG", "Network error", e)
                Toast.makeText(this@LoginActivity, "Network error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }
} 