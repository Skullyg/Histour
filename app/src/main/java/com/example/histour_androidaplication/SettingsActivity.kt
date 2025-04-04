package com.example.histour_androidaplication

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SettingsActivity : AppCompatActivity() {

    private lateinit var switchTheme: Switch
    private lateinit var editTextUsername: EditText
    private lateinit var buttonSaveUsername: Button
    private lateinit var editTextPassword: EditText
    private lateinit var buttonSavePassword: Button
    private lateinit var sharedPreferences: SharedPreferences

    private val db = FirebaseFirestore.getInstance()
    private val user = FirebaseAuth.getInstance().currentUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        switchTheme = findViewById(R.id.switch_theme)
        editTextUsername = findViewById(R.id.editTextUsername)
        buttonSaveUsername = findViewById(R.id.button_save_username)
        editTextPassword = findViewById(R.id.editTextPassword)
        buttonSavePassword = findViewById(R.id.button_save_password)

        sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE)

        // Carregar preferências de tema
        switchTheme.isChecked = sharedPreferences.getBoolean("DarkMode", false)
        switchTheme.setOnCheckedChangeListener { _, isChecked ->
            val editor = sharedPreferences.edit()
            editor.putBoolean("DarkMode", isChecked)
            editor.apply()

            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        // Atualizar nome de utilizador
        buttonSaveUsername.setOnClickListener {
            val newUsername = editTextUsername.text.toString().trim()
            if (newUsername.isNotEmpty() && user != null) {
                db.collection("Utilizadores").document(user.uid)
                    .update("nome", newUsername)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Nome atualizado!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Erro ao atualizar nome!", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        // Atualizar password
        buttonSavePassword.setOnClickListener {
            val newPassword = editTextPassword.text.toString().trim()
            if (newPassword.length >= 6 && user != null) {
                user.updatePassword(newPassword)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Password alterada!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Erro ao alterar password!", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "A password deve ter pelo menos 6 caracteres!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
