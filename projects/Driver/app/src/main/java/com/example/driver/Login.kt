package com.example.driver

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.driver.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth

class Login : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        if (isUserAuthenticated()) {
            // If user is already authenticated, skip the login screen and proceed to MainActivity
            startMainActivity()
            return
        }

        val buttonSignIn = findViewById<Button>(R.id.signInButton)
        binding.signInButton.setOnClickListener {
            val signinEmail = binding.emailEditText.text?.toString() ?: ""
            val signinPassword = binding.passwordEditText.text?.toString() ?: ""

            if (signinEmail.isNotEmpty() && signinPassword.isNotEmpty()) {
                signIn(signinEmail, signinPassword)
            } else {
                Toast.makeText(this@Login, "All fields are mandatory", Toast.LENGTH_SHORT).show()
            }
        }


    }

    private fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Store the authentication state locally
                    saveAuthenticationState(true)
                    // Start MainActivity
                    startMainActivity()
                    finish() // Finish LoginActivity to prevent returning back to it
                } else {
                    Toast.makeText(this@Login, "Authentication failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }
    private fun startMainActivity() {
        startActivity(Intent(this@Login, MainActivity::class.java))
    }

    private fun isUserAuthenticated(): Boolean {
        // Retrieve the authentication state from SharedPreferences
        val sharedPreferences = getSharedPreferences("auth", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("authenticated", false)
    }

    private fun saveAuthenticationState(authenticated: Boolean) {
        // Store the authentication state in SharedPreferences
        val sharedPreferences = getSharedPreferences("auth", Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean("authenticated", authenticated).apply()
    }
}
