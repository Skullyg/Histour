package com.example.histour_androidaplication

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.histour_androidaplication.models.Utilizador
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ActivityLogin : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activitylogin)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val emailInput = findViewById<EditText>(R.id.etEmail)
        val passwordInput = findViewById<EditText>(R.id.etSenha)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnReenviarVerificacao = findViewById<Button>(R.id.btnReenviarVerificacao)

        btnLogin.setOnClickListener {
            val input = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            btnReenviarVerificacao.visibility = View.GONE // Oculta antes de qualquer login

            if (input.isNotEmpty() && password.isNotEmpty()) {
                loginUser(input, password, btnReenviarVerificacao)
            } else {
                Toast.makeText(this, "Preencha todos os campos!", Toast.LENGTH_SHORT).show()
            }
        }


        btnReenviarVerificacao.setOnClickListener {
            val user = auth.currentUser
            user?.sendEmailVerification()
                ?.addOnCompleteListener { verifyTask ->
                    if (verifyTask.isSuccessful) {
                        Toast.makeText(
                            this,
                            "Email de verificação reenviado para ${user.email}",
                            Toast.LENGTH_LONG
                        ).show()
                        btnReenviarVerificacao.visibility = View.GONE
                    } else {
                        Toast.makeText(
                            this,
                            "Erro ao reenviar verificação: ${verifyTask.exception?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }


    }

    private fun loginUser(
        emailOrNome: String,
        password: String,
        btnReenviarVerificacao: Button
    ) {
        val isEmail = Patterns.EMAIL_ADDRESS.matcher(emailOrNome).matches()

        if (isEmail) {
            // Login normal com email diretamente
            signInWithEmail(emailOrNome, password, btnReenviarVerificacao)
        } else {
            // Buscar o email a partir do nome
            db.collection("Utilizadores")
                .whereEqualTo("nome", emailOrNome)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        val userDoc = documents.documents[0]
                        val emailFromNome = userDoc.getString("email") ?: ""

                        signInWithEmail(emailFromNome, password, btnReenviarVerificacao)
                    } else {
                        Toast.makeText(
                            this,
                            "Nome de utilizador não encontrado!",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        this,
                        "Erro ao buscar utilizador: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }

    private fun signInWithEmail(
        email: String,
        password: String,
        btnReenviarVerificacao: Button
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null && user.isEmailVerified) {
                        val userId = user.uid

                        db.collection("Utilizadores").document(userId).get()
                            .addOnSuccessListener { document ->
                                val utilizador = document.toObject(Utilizador::class.java)
                                Toast.makeText(
                                    this,
                                    "Bem-vindo, ${utilizador?.nome}!",
                                    Toast.LENGTH_SHORT
                                ).show()

                                val intent = Intent(this, MainActivity::class.java)
                                intent.putExtra("utilizadorNome", utilizador?.nome)

                                // Tema do utilizador, se quiseres adicionar depois
                                // val tema = document.getString("tema") ?: "light"
                                // ...

                                startActivity(intent)
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    this,
                                    "Erro ao buscar dados: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    } else {
                        Toast.makeText(
                            this,
                            "Email não verificado! Verifique seu email antes de entrar.",
                            Toast.LENGTH_LONG
                        ).show()
                        auth.signOut()
                        btnReenviarVerificacao.visibility = View.VISIBLE
                    }
                } else {
                    Toast.makeText(
                        this,
                        "Erro ao fazer login: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }
}
