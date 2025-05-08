package com.example.histour_androidaplication

import android.content.Intent
import android.os.Bundle
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.histour_androidaplication.models.Poi
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.histour_androidaplication.FavoritesAdapter


class FavoritesActivity : AppCompatActivity() {  // ✅ Sem construtor

    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid
    private lateinit var listView: ListView
    private lateinit var adapter: FavoritesAdapter
    private val favoritePois = mutableListOf<Poi>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites) // ✅ Certifica-te de que existe esse XML!

        listView = findViewById(R.id.listViewFavorites)
        adapter = FavoritesAdapter(this, favoritePois)
        listView.adapter = adapter

        loadFavorites()

        listView.setOnItemClickListener { _, _, position, _ ->
            val poi = favoritePois[position]
            val intent = Intent(this, PoiDetailActivity::class.java)
            intent.putExtra("nome", poi.nome)
            intent.putExtra("descricao", poi.descricao)
            intent.putExtra("latitude", poi.latitude)
            intent.putExtra("longitude", poi.longitude)
            intent.putExtra("imagemBase64", poi.imagemBase64)
            startActivity(intent)
        }
    }

    private fun loadFavorites() {
        if (userId == null) {
            Toast.makeText(this, "É necessário iniciar sessão!", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("Utilizadores").document(userId)
            .collection("Favoritos")
            .get()
            .addOnSuccessListener { result ->
                favoritePois.clear() // Limpar lista antes de recarregar

                for (document in result) {
                    val poi = document.toObject(Poi::class.java)
                    favoritePois.add(poi)
                }

                adapter.notifyDataSetChanged() // Atualizar a lista na UI
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao carregar favoritos!", Toast.LENGTH_SHORT).show()
            }
    }
}
