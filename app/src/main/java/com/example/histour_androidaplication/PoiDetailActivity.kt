package com.example.histour_androidaplication

import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.histour_androidaplication.models.Poi
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File

class PoiDetailActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: LatLng? = null
    private var mediaPlayer: MediaPlayer? = null

    private lateinit var db: FirebaseFirestore
    private lateinit var userId: String

    private lateinit var buttonFavorite: ImageButton
    private lateinit var buttonEliminarPoi: Button
    private lateinit var buttonEditarPoi: Button
    private lateinit var buttonOuvirAudio: Button

    private lateinit var poiId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.detalhes_poi)

        db = FirebaseFirestore.getInstance()
        userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        buttonFavorite = findViewById(R.id.button_favorite)
        buttonEliminarPoi = findViewById(R.id.button_eliminar_poi)
        buttonEditarPoi = findViewById(R.id.button_editar_poi)
        buttonOuvirAudio = findViewById(R.id.button_ouvir_audio)

        poiId = intent.getStringExtra("id") ?: run {
            Toast.makeText(this, "ID do POI não encontrado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        carregarPOI()
        getUserLocation()
    }

    private fun carregarPOI() {
        db.collection("POIs").document(poiId).get()
            .addOnSuccessListener { doc ->
                val poi = doc.toObject(Poi::class.java)
                if (poi != null) {
                    preencherUI(poi)
                } else {
                    Toast.makeText(this, "POI não encontrado!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao buscar POI", Toast.LENGTH_SHORT).show()
            }
    }

    private fun preencherUI(poi: Poi) {
        findViewById<TextView>(R.id.textNome).text = poi.nome
        findViewById<TextView>(R.id.textDescricao).text = poi.descricao
        findViewById<TextView>(R.id.textTipo).text = getString(R.string.tipo_label, poi.tipo)

        findViewById<TextView>(R.id.textDescricao).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Descrição completa")
                .setMessage(poi.descricao)
                .setPositiveButton("Fechar", null)
                .show()
        }

        poi.imagemBase64?.let {
            val imageBytes = Base64.decode(it, Base64.DEFAULT)
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            findViewById<ImageView>(R.id.imageViewPOI).setImageBitmap(bitmap)
        }

        if (!poi.audioBase64.isNullOrEmpty()) {
            val audioBytes = Base64.decode(poi.audioBase64, Base64.DEFAULT)
            val tempAudio = File.createTempFile("temp_audio", ".mp3", cacheDir)
            tempAudio.writeBytes(audioBytes)

            buttonOuvirAudio.visibility = View.VISIBLE
            buttonOuvirAudio.setOnClickListener {
                if (mediaPlayer == null) {
                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(tempAudio.absolutePath)
                        prepare()
                        start()
                    }
                } else {
                    if (mediaPlayer!!.isPlaying) {
                        mediaPlayer!!.pause()
                    } else {
                        mediaPlayer!!.start()
                    }
                }
            }
        } else {
            buttonOuvirAudio.visibility = View.GONE
        }

        findViewById<Button>(R.id.button_route).setOnClickListener {
            abrirRota(poi)
        }

        findViewById<Button>(R.id.button_comentar).setOnClickListener {
            startActivity(Intent(this, ComentarioActivity::class.java).putExtra("poi_nome", poi.nome))
        }

        findViewById<Button>(R.id.button_ver_comentarios).setOnClickListener {
            startActivity(Intent(this, VerComentariosActivity::class.java).putExtra("poi_nome", poi.nome))
        }

        verificarFavorito(poi)
        verificarSeVisitado(poi)

        findViewById<Button>(R.id.button_visited).setOnClickListener {
            marcarComoVisitado(poi)
        }

        // Ações de admin
        db.collection("Utilizadores").document(userId).get()
            .addOnSuccessListener { doc ->
                if (doc.getString("tipo") == "admin") {
                    buttonEliminarPoi.visibility = View.VISIBLE
                    buttonEditarPoi.visibility = View.VISIBLE

                    buttonEliminarPoi.setOnClickListener { eliminarPoi(poiId) }

                    buttonEditarPoi.setOnClickListener {
                        startActivity(Intent(this, EditPoiActivity::class.java).apply {
                            putExtra("id", poiId)
                        })
                    }
                }
            }
    }

    private fun abrirRota(poi: Poi) {
        val latUser = currentLocation?.latitude ?: 41.14961
        val lngUser = currentLocation?.longitude ?: -8.61099

        val origin = if (isUserInPorto(latUser, lngUser)) LatLng(latUser, lngUser) else LatLng(41.14961, -8.61099)

        val intent = Intent(this, RouteActivity::class.java).apply {
            putExtra("latitude", poi.latitude)
            putExtra("longitude", poi.longitude)
            putExtra("user_latitude", origin.latitude)
            putExtra("user_longitude", origin.longitude)
        }
        startActivity(intent)
    }

    private fun verificarFavorito(poi: Poi) {
        db.collection("Utilizadores").document(userId)
            .collection("Favoritos").document(poi.nome).get()
            .addOnSuccessListener {
                val resId = if (it.exists()) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_border
                buttonFavorite.setImageResource(resId)
            }

        buttonFavorite.setOnClickListener {
            val ref = db.collection("Utilizadores").document(userId)
                .collection("Favoritos").document(poi.nome)

            ref.get().addOnSuccessListener { doc ->
                if (doc.exists()) {
                    ref.delete().addOnSuccessListener {
                        buttonFavorite.setImageResource(R.drawable.ic_favorite_border)
                    }
                } else {
                    val fav = mapOf(
                        "nome" to poi.nome,
                        "descricao" to poi.descricao,
                        "latitude" to poi.latitude,
                        "longitude" to poi.longitude,
                        "imagemBase64" to poi.imagemBase64
                    )
                    ref.set(fav).addOnSuccessListener {
                        buttonFavorite.setImageResource(R.drawable.ic_favorite_filled)
                    }
                }
            }
        }
    }

    private fun verificarSeVisitado(poi: Poi) {
        val btn = findViewById<Button>(R.id.button_visited)
        db.collection("Utilizadores").document(userId)
            .collection("Visitas").document(poi.nome)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    btn.text = getString(R.string.ja_visitado)
                    btn.isEnabled = false
                    findViewById<Button>(R.id.button_comentar).visibility = View.VISIBLE
                }
            }
    }

    private fun marcarComoVisitado(poi: Poi) {
        val btn = findViewById<Button>(R.id.button_visited)
        val ref = db.collection("Utilizadores").document(userId)
            .collection("Visitas").document(poi.nome)

        val dados = mapOf(
            "nome" to poi.nome,
            "descricao" to poi.descricao,
            "latitude" to poi.latitude,
            "longitude" to poi.longitude,
            "imagemBase64" to poi.imagemBase64,
            "timestamp" to System.currentTimeMillis()
        )

        ref.set(dados).addOnSuccessListener {
            Toast.makeText(this, "Marcado como visitado!", Toast.LENGTH_SHORT).show()
            btn.text = getString(R.string.ja_visitado)
            btn.isEnabled = false
            findViewById<Button>(R.id.button_comentar).visibility = View.VISIBLE
        }
    }

    private fun eliminarPoi(poiId: String) {
        db.collection("POIs").document(poiId).delete()
            .addOnSuccessListener {
                Toast.makeText(this, "POI eliminado com sucesso!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao eliminar o POI!", Toast.LENGTH_SHORT).show()
            }
    }

    private fun getUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 1000)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                currentLocation = LatLng(location.latitude, location.longitude)
            }
        }
    }

    private fun isUserInPorto(lat: Double, lng: Double): Boolean {
        return lat in 41.110..41.200 && lng in -8.675..-8.560
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
