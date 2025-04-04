package com.example.histour_androidaplication

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.histour_androidaplication.models.Poi
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class CreatePoiActivity : AppCompatActivity() {

    private lateinit var editTextNome: EditText
    private lateinit var editTextDescricao: EditText
    private lateinit var spinnerTipoPoi: Spinner
    private lateinit var btnGuardarPoi: Button
    private lateinit var btnSelectImage: Button
    private lateinit var imgPreview: ImageView

    private var imageUri: Uri? = null
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

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

        latitude = intent.getDoubleExtra("latitude", 0.0)
        longitude = intent.getDoubleExtra("longitude", 0.0)

        // Definir opções do Spinner
        val tiposPoi = arrayOf("Museu", "Monumento Histórico", "Praça", "Outro")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, tiposPoi)
        spinnerTipoPoi.adapter = adapter

        btnSelectImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 100)
        }

        btnGuardarPoi.setOnClickListener {
            if (imageUri != null) {
                uploadImageAndSavePoi()
            } else {
                savePoiToFirestore("")
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 100 && resultCode == Activity.RESULT_OK && data != null) {
            imageUri = data.data
            imgPreview.setImageURI(imageUri)
        }
    }

    private fun uploadImageAndSavePoi() {
        val fileName = "pois/${UUID.randomUUID()}.jpg"
        val storageRef = storage.reference.child(fileName)

        storageRef.putFile(imageUri!!)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { imageUrl ->
                    savePoiToFirestore(imageUrl.toString())
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao carregar imagem", Toast.LENGTH_SHORT).show()
            }
    }

    private fun savePoiToFirestore(imageUrl: String) {
        val nome = editTextNome.text.toString().trim()
        val descricao = editTextDescricao.text.toString().trim()
        val tipoPoi = spinnerTipoPoi.selectedItem.toString()

        if (nome.isEmpty() || descricao.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos!", Toast.LENGTH_SHORT).show()
            return
        }

        val poi = Poi(nome, descricao, latitude, longitude, imagemUrl = imageUrl, tipo = tipoPoi)

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
