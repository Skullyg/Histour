package com.example.histour_androidaplication

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.histour_androidaplication.models.Utilizador
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        val radioGroupTipo = findViewById<RadioGroup>(R.id.radioGroupTipo)


        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val editNome = findViewById<EditText>(R.id.editNome)
        val editEmail = findViewById<EditText>(R.id.editEmail)
        val editPassword = findViewById<EditText>(R.id.editPassword)
        val editConfirmPassword = findViewById<EditText>(R.id.editConfirmPassword)
        val btnCriarConta = findViewById<Button>(R.id.btnCriarConta)
        val txtVoltarLogin = findViewById<TextView>(R.id.txtVoltarLogin)

        btnCriarConta.setOnClickListener {
            val nome = editNome.text.toString().trim()
            val email = editEmail.text.toString().trim()
            val password = editPassword.text.toString().trim()
            val confirmPassword = editConfirmPassword.text.toString().trim()

            if (!validarCampos(nome, email, password, confirmPassword)) return@setOnClickListener

            registerUser(nome, email, password)
        }

        txtVoltarLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)  // Corrigido para LoginActivity
            startActivity(intent)
            finish()
        }
    }

    private fun validarCampos(nome: String, email: String, password: String, confirmPassword: String): Boolean {
        if (nome.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos!", Toast.LENGTH_SHORT).show()
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Insira um email válido!", Toast.LENGTH_SHORT).show()
            return false
        }

        if (password.length < 6) {
            Toast.makeText(this, "A senha deve ter pelo menos 6 caracteres!", Toast.LENGTH_SHORT).show()
            return false
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "As senhas não coincidem!", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun registerUser(nome: String, email: String, password: String) {
        val radioGroupTipo = findViewById<RadioGroup>(R.id.radioGroupTipo)
        val tipoSelecionadoId = radioGroupTipo.checkedRadioButtonId
        val tipoConta = if (tipoSelecionadoId == R.id.radioAdmin) "admin" else "utilizador"

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.sendEmailVerification()
                        ?.addOnCompleteListener { verifyTask ->
                            if (verifyTask.isSuccessful) {
                                val userId = user.uid
                                val utilizador = Utilizador(userId, nome, email, tipoConta)

                                db.collection("Utilizadores").document(userId).set(utilizador)
                                    .addOnSuccessListener {
                                        Toast.makeText(this, "Conta criada! Verifique seu email antes de entrar.", Toast.LENGTH_LONG).show()
                                        auth.signOut() // Força logout após registo
                                        val intent = Intent(this, LoginActivity::class.java)
                                        startActivity(intent)
                                        finish()
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(this, "Erro ao salvar utilizador: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                            } else {
                                Toast.makeText(this, "Erro ao enviar verificação de email!", Toast.LENGTH_LONG).show()
                            }
                        }
                } else {
                    Toast.makeText(this, "Erro ao registar: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }

    }



}
