package com.example.histour_androidaplication

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.histour_androidaplication.models.Comentario
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class VerComentariosActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ComentariosAdapter
    private val listaComentarios = mutableListOf<Comentario>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ver_comentarios)

        val poiNome = intent.getStringExtra("poi_nome") ?: return

        recyclerView = findViewById(R.id.recyclerComentarios)
        adapter = ComentariosAdapter(listaComentarios)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        val db = FirebaseFirestore.getInstance()
        db.collection("POIs").document(poiNome).collection("Comentarios")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documentos ->
                for (doc in documentos) {
                    val comentario = doc.getString("comentario") ?: ""
                    val userId = doc.getString("userId") ?: "Anónimo"
                    val timestamp = doc.getLong("timestamp") ?: 0

                    listaComentarios.add(Comentario(userId, comentario, timestamp))
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao carregar comentários", Toast.LENGTH_SHORT).show()
            }
    }
}
