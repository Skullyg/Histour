package com.example.histour_androidaplication

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.bumptech.glide.Glide
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.jvm.java
import android.media.MediaPlayer
import android.net.Uri

class PoiDetailActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: LatLng? = null
    private lateinit var buttonFavorite: ImageButton
    private var mediaPlayer: MediaPlayer? = null
    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid
    private lateinit var buttonEliminarPoi: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.detalhes_poi)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val nome = intent.getStringExtra("nome") ?: "Nome desconhecido"
        val descricao = intent.getStringExtra("descricao") ?: "Sem descrição"
        val imagemUrl = intent.getStringExtra("imagemUrl") ?: ""
        val latitude = intent.getDoubleExtra("latitude", 0.0)
        val longitude = intent.getDoubleExtra("longitude", 0.0)

        val textNome = findViewById<TextView>(R.id.textNome)
        val textDescricao = findViewById<TextView>(R.id.textDescricao)
        textDescricao.text = descricao
        textDescricao.setOnClickListener {
            val dialog = android.app.AlertDialog.Builder(this)
                .setTitle("Descrição completa")
                .setMessage(descricao)
                .setPositiveButton("Fechar", null)
                .create()

            dialog.show()
        }
        val imageViewPOI = findViewById<ImageView>(R.id.imageViewPOI)
        val buttonRoute = findViewById<Button>(R.id.button_route)
        buttonFavorite = findViewById(R.id.button_favorite)
        val tipo = intent.getStringExtra("tipo") ?: "Outro"
        val textTipo = findViewById<TextView>(R.id.textTipo)
        textTipo.text = getString(R.string.tipo_label, tipo)
        val buttonVisited = findViewById<Button>(R.id.button_visited)
        val buttonComentar = findViewById<Button>(R.id.button_comentar)
        val buttonVerComentarios = findViewById<Button>(R.id.button_ver_comentarios)
        val buttonOuvirAudio = findViewById<Button>(R.id.button_ouvir_audio)
        val audioUrl = intent.getStringExtra("audioUrl")
        buttonEliminarPoi = findViewById(R.id.button_eliminar_poi)
        val buttonEditarPoi = findViewById<Button>(R.id.button_editar_poi)




        textNome.text = nome
        textDescricao.text = descricao

        // Carregar a imagem com Glide
        if (imagemUrl.isNotEmpty()) {
            Glide.with(this).load(imagemUrl).into(imageViewPOI)
        } else {
            imageViewPOI.setImageResource(R.drawable.ic_launcher_background)
        }

        // Obter a localização do utilizador
        getUserLocation()

        // Configurar clique no botão para abrir a RouteActivity
        buttonRoute.setOnClickListener {
            val userLat = currentLocation?.latitude ?: 0.0
            val userLng = currentLocation?.longitude ?: 0.0

            // Definir ponto fixo no Porto (Avenida dos Aliados)
            val portoLat = 41.14961
            val portoLng = -8.61099

            // Se o utilizador não estiver dentro dos limites do Porto, usa o ponto fixo do Porto
            val originLat = if (isUserInPorto(userLat, userLng)) userLat else portoLat
            val originLng = if (isUserInPorto(userLat, userLng)) userLng else portoLng


            Log.d("PoiDetailActivity", "Origem: $originLat, $originLng | Destino: $latitude, $longitude")

            val intent = Intent(this, RouteActivity::class.java).apply {
                putExtra("latitude", latitude)
                putExtra("longitude", longitude)
                putExtra("user_latitude", originLat)
                putExtra("user_longitude", originLng)
            }

            startActivity(intent)
        }

        checkIfFavorite(nome)
        buttonFavorite.setOnClickListener {
            toggleFavorite(nome, descricao, latitude, longitude, imagemUrl)
        }

        verificarSeVisitado(nome, buttonVisited)


        buttonVisited.setOnClickListener {
            marcarComoVisitado(nome, descricao, latitude, longitude, imagemUrl, buttonVisited)
        }



        buttonComentar.setOnClickListener {
            val intent = Intent(this, ComentarioActivity::class.java)
            intent.putExtra("poi_nome", nome)
            startActivity(intent)
        }

        buttonVerComentarios.setOnClickListener {
            val intent = Intent(this, VerComentariosActivity::class.java)
            intent.putExtra("poi_nome", nome)
            startActivity(intent)
        }
        if (!audioUrl.isNullOrEmpty()) {
            buttonOuvirAudio.setOnClickListener {
                if (mediaPlayer == null) {
                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(audioUrl)
                        prepare()
                        start()
                    }
                    Toast.makeText(this, "A reproduzir áudio...", Toast.LENGTH_SHORT).show()
                } else {
                    if (mediaPlayer!!.isPlaying) {
                        mediaPlayer!!.pause()
                        Toast.makeText(this, "Áudio pausado", Toast.LENGTH_SHORT).show()
                    } else {
                        mediaPlayer!!.start()
                        Toast.makeText(this, "A reproduzir áudio...", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            buttonOuvirAudio.visibility = View.GONE
        }

        if (userId != null) {
            db.collection("Utilizadores").document(userId).get()
                .addOnSuccessListener { document ->
                    val tipo = document.getString("tipo")
                    if (tipo == "admin") {
                        buttonEliminarPoi.visibility = View.VISIBLE
                        buttonEditarPoi.visibility = View.VISIBLE

                        buttonEliminarPoi.setOnClickListener {
                            eliminarPoi(nome)
                        }

                        buttonEditarPoi.setOnClickListener {
                            val intent = Intent(this, EditPoiActivity::class.java).apply {
                                putExtra("nome", nome)
                                putExtra("descricao", descricao)
                                putExtra("imagemUrl", imagemUrl)
                                putExtra("latitude", latitude)
                                putExtra("longitude", longitude)
                                putExtra("tipo", tipo)
                                putExtra("audioUrl", audioUrl)
                            }
                            startActivity(intent)
                        }


                    }
                }
        }





    }

    private fun getUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 1000)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                currentLocation = LatLng(location.latitude, location.longitude)
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Erro ao obter localização", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isUserInPorto(lat: Double, lng: Double): Boolean {
        val portoBounds = listOf(
            LatLng(41.110, -8.675), // Sudoeste
            LatLng(41.200, -8.560)  // Nordeste
        )

        return lat in portoBounds[0].latitude..portoBounds[1].latitude &&
                lng in portoBounds[0].longitude..portoBounds[1].longitude
    }

    private fun checkIfFavorite(nome: String) {
        if (userId != null) {
            db.collection("Utilizadores").document(userId)
                .collection("Favoritos")
                .document(nome)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        buttonFavorite.setImageResource(R.drawable.ic_favorite_filled)
                    } else {
                        buttonFavorite.setImageResource(R.drawable.ic_favorite_border)
                    }
                }
        }
    }

    private fun toggleFavorite(nome: String, descricao: String, latitude: Double, longitude: Double, imagemUrl: String) {
        if (userId == null) {
            Toast.makeText(this, "É necessário iniciar sessão!", Toast.LENGTH_SHORT).show()
            return
        }

        val favoritoRef = db.collection("Utilizadores").document(userId)
            .collection("Favoritos").document(nome)

        favoritoRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                favoritoRef.delete()
                    .addOnSuccessListener {
                        buttonFavorite.setImageResource(R.drawable.ic_favorite_border)
                        Toast.makeText(this, "Removido dos Favoritos!", Toast.LENGTH_SHORT).show()
                    }
            } else {
                val favorito = mapOf(
                    "nome" to nome,
                    "descricao" to descricao,
                    "latitude" to latitude,
                    "longitude" to longitude,
                    "imagemUrl" to imagemUrl
                )
                favoritoRef.set(favorito)
                    .addOnSuccessListener {
                        buttonFavorite.setImageResource(R.drawable.ic_favorite_filled)
                        Toast.makeText(this, "Adicionado aos Favoritos!", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun marcarComoVisitado(
        nome: String,
        descricao: String,
        latitude: Double,
        longitude: Double,
        imagemUrl: String,
        button: Button
    ) {
        if (userId == null) {
            Toast.makeText(this, "É necessário iniciar sessão!", Toast.LENGTH_SHORT).show()
            return
        }

        val visitaRef = db.collection("Utilizadores").document(userId)
            .collection("Visitas").document(nome)

        visitaRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                Toast.makeText(this, "Já marcaste este ponto como visitado!", Toast.LENGTH_SHORT).show()
                button.text = getString(R.string.ja_visitado)
                button.isEnabled = false
                findViewById<Button>(R.id.button_comentar).visibility = View.VISIBLE
            } else {
                val visita = mapOf(
                    "nome" to nome,
                    "descricao" to descricao,
                    "latitude" to latitude,
                    "longitude" to longitude,
                    "imagemUrl" to imagemUrl,
                    "timestamp" to System.currentTimeMillis()
                )
                visitaRef.set(visita)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Marcado como visitado!", Toast.LENGTH_SHORT).show()
                        button.text = getString(R.string.ja_visitado)
                        button.isEnabled = false
                        findViewById<Button>(R.id.button_comentar).visibility = View.VISIBLE
                    }
            }
        }
    }



    private fun verificarSeVisitado(nome: String, button: Button) {
        if (userId == null) return

        val visitaRef = db.collection("Utilizadores").document(userId)
            .collection("Visitas").document(nome)

        visitaRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                button.text = getString(R.string.ja_visitado)
                button.isEnabled = false
                findViewById<Button>(R.id.button_comentar).visibility = View.VISIBLE
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun eliminarPoi(nome: String) {
        db.collection("POIs").whereEqualTo("nome", nome).get()
            .addOnSuccessListener { documents ->
                for (doc in documents) {
                    doc.reference.delete()
                }
                Toast.makeText(this, "POI eliminado com sucesso!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao eliminar o POI!", Toast.LENGTH_SHORT).show()
            }
    }



}
