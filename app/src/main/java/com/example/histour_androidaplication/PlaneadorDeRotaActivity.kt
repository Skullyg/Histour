package com.example.histour_androidaplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.histour_androidaplication.models.Poi
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.jvm.java

class PlaneadorDeRotaActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var buttonConfirmar: Button
    private lateinit var adapter: POISelectionAdapter
    private val poiList = mutableListOf<Poi>()
    private var selectedPOIs = listOf<Poi>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_route)

        recyclerView = findViewById(R.id.recyclerViewPOIs)
        buttonConfirmar = findViewById(R.id.button_confirmar_rota)

        recyclerView.layoutManager = LinearLayoutManager(this)

        // Lê os POIs do Firebase
        FirebaseFirestore.getInstance().collection("POIs")
            .get()
            .addOnSuccessListener { result ->
                for (doc in result) {
                    val poi = doc.toObject(Poi::class.java)
                    poiList.add(poi)
                }

                adapter = POISelectionAdapter(poiList) { selecionados ->
                    selectedPOIs = selecionados
                }

                recyclerView.adapter = adapter
            }

        buttonConfirmar.setOnClickListener {
            if (selectedPOIs.size < 2) {
                Toast.makeText(this, "Escolhe pelo menos 2 POIs!", Toast.LENGTH_SHORT).show()
            } else {
                val rotaId = FirebaseFirestore.getInstance().collection("RotasTemporarias").document().id

                val rotaTemp = hashMapOf("pois" to selectedPOIs)

                FirebaseFirestore.getInstance().collection("RotasTemporarias")
                    .document(rotaId)
                    .set(rotaTemp)
                    .addOnSuccessListener {
                        val intent = Intent(this, MultiPOIActivity::class.java)
                        intent.putExtra("ROTA_ID", rotaId)
                        startActivity(intent)
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Erro ao guardar os POIs!", Toast.LENGTH_SHORT).show()
                    }

            }
        }

    }
}
