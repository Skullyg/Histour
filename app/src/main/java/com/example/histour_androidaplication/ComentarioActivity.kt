package com.example.histour_androidaplication

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ComentarioActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var poiNome: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comentario)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        poiNome = intent.getStringExtra("poi_nome") ?: return

        val editComentario = findViewById<EditText>(R.id.editComentario)
        val buttonSubmeter = findViewById<Button>(R.id.buttonSubmeter)

        buttonSubmeter.setOnClickListener {
            val texto = editComentario.text.toString().trim()
            val userId = auth.currentUser?.uid

            if (texto.isEmpty() || userId == null) {
                Toast.makeText(this, "Comentário vazio ou utilizador inválido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val comentario = hashMapOf(
                "userId" to userId,
                "comentario" to texto,
                "timestamp" to System.currentTimeMillis()
            )

            db.collection("POIs")
                .document(poiNome)
                .collection("Comentarios")
                .add(comentario)
                .addOnSuccessListener {
                    Toast.makeText(this, "Comentário enviado!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Erro ao enviar comentário", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
