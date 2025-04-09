package com.example.histour_androidaplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val btnEntrar = findViewById<Button>(R.id.btnEntrar)
        val textCriarConta = findViewById<TextView>(R.id.txtCriarConta)
        val btnGoogle = findViewById<ImageView>(R.id.btnGoogle)


        btnEntrar.setOnClickListener {
            val intent = Intent(this, ActivityLogin::class.java)
            startActivity(intent)
        }

        textCriarConta.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        // Configurar Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        btnGoogle.setOnClickListener {
            signInWithGoogle()
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, 1001)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1001) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account: GoogleSignInAccount = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(this, "Erro no login: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser

                    if (user != null) {
                        // Buscar dados do usuário no Firestore de forma assíncrona para evitar ANR
                        db.collection("Utilizadores").document(user.uid).get()
                            .addOnSuccessListener { document ->
                                if (!document.exists()) {
                                    // Se o utilizador não existir, criar no Firestore
                                    val novoUtilizador = hashMapOf(
                                        "nome" to user.displayName,
                                        "email" to user.email,
                                        "uid" to user.uid
                                    )
                                    db.collection("Utilizadores").document(user.uid).set(novoUtilizador)
                                }
                                Toast.makeText(this, "Bem-vindo, ${user.displayName}!", Toast.LENGTH_SHORT).show()

                                // Ir para a MainActivity depois do login bem-sucedido
                                val tema = document.getString("tema") ?: "light"
                                val modo = if (tema == "dark") AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
                                AppCompatDelegate.setDefaultNightMode(modo)

// Vai para MainActivity com o tema já aplicado
                                startActivity(Intent(this, MainActivity::class.java))
                                finish()
                                // Fechar a LoginActivity para evitar voltar
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Erro ao buscar utilizador: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    }
                } else {
                    Toast.makeText(this, "Erro ao autenticar com Google.", Toast.LENGTH_LONG).show()
                }
            }
    }
}
