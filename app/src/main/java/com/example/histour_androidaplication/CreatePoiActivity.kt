package com.example.histour_androidaplication

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.histour_androidaplication.models.Poi
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

class CreatePoiActivity : AppCompatActivity() {

    private lateinit var editTextNome: EditText
    private lateinit var editTextDescricao: EditText
    private lateinit var spinnerTipoPoi: Spinner
    private lateinit var btnGuardarPoi: Button
    private lateinit var btnSelectImage: Button
    private lateinit var imgPreview: ImageView
    private lateinit var btnSelectAudio: Button
    private var audioUri: Uri? = null
    private val AUDIO_PICK_CODE = 101


    private var imageUri: Uri? = null
    private val db = FirebaseFirestore.getInstance()
    private fun uriToBase64(uri: Uri): String? {
        return contentResolver.openInputStream(uri)?.use { inputStream ->
            val bytes = inputStream.readBytes()
            android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
        }
    }



    private var latitude: Double = 0.0
    private var longitude: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.criar_poi)

        editTextNome = findViewById(R.id.editTextNome)
        editTextDescricao = findViewById(R.id.editTextDescricao)
        spinnerTipoPoi = findViewById(R.id.spinner_tipo_poi)
        btnGuardarPoi = findViewById(R.id.btnSalvar)
        btnSelectImage = findViewById(R.id.btn_select_image)
        imgPreview = findViewById(R.id.poi_image_preview)
        btnSelectAudio = findViewById(R.id.btnSelectAudio)

        latitude = intent.getDoubleExtra("latitude", 0.0)
        longitude = intent.getDoubleExtra("longitude", 0.0)

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                1
            )
        }

        // Definir opções do Spinner
        val tiposPoi = arrayOf("Museu", "Monumento Histórico", "Praça", "Outro")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, tiposPoi)
        spinnerTipoPoi.adapter = adapter

        btnSelectImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 100)
        }

        btnSelectAudio.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "audio/*"
            startActivityForResult(intent, AUDIO_PICK_CODE)
        }

        btnGuardarPoi.setOnClickListener {
            val nome = editTextNome.text.toString().trim()
            val descricao = editTextDescricao.text.toString().trim()
            val tipoPoi = spinnerTipoPoi.selectedItem.toString()

            if (nome.isEmpty() || descricao.isEmpty()) {
                Toast.makeText(this, "Preencha todos os campos!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val imagemBase64 = imageUri?.let { uriToBase64(it) }
            val audioBase64 = audioUri?.let { uriToBase64(it) }

            val poi = Poi(
                nome = nome,
                descricao = descricao,
                latitude = latitude,
                longitude = longitude,
                imagemBase64 = imagemBase64,
                tipo = tipoPoi,
                audioBase64 = audioBase64
            )

            db.collection("POIs").add(poi)
                .addOnSuccessListener {
                    Toast.makeText(this, "POI criado com sucesso!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Erro ao guardar o POI!", Toast.LENGTH_SHORT).show()
                }
        }


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 100 && resultCode == Activity.RESULT_OK && data != null) {
            imageUri = data.data
            imgPreview.setImageURI(imageUri)
        }
        else if (requestCode == AUDIO_PICK_CODE && resultCode == Activity.RESULT_OK && data != null) {
            audioUri = data.data
            Toast.makeText(this, "Áudio selecionado com sucesso!", Toast.LENGTH_SHORT).show()
        }
    }
}
