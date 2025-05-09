package com.example.histour_androidaplication

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.histour_androidaplication.models.Poi
import com.google.firebase.firestore.FirebaseFirestore

class EditPoiActivity : AppCompatActivity() {

    private lateinit var editNome: EditText
    private lateinit var editDescricao: EditText
    private lateinit var spinnerTipo: Spinner
    private lateinit var imageView: ImageView
    private lateinit var buttonNovaImagem: Button
    private lateinit var buttonNovoAudio: Button
    private lateinit var buttonSalvar: Button
    private lateinit var audioText: TextView

    private lateinit var firestore: FirebaseFirestore
    private var poiId: String = ""

    private var novaImagemUri: Uri? = null
    private var novoAudioUri: Uri? = null

    companion object {
        private const val REQUEST_IMAGE_PICK = 100
        private const val REQUEST_AUDIO_PICK = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_poi)

        editNome = findViewById(R.id.editNomePoi)
        editDescricao = findViewById(R.id.editDescricao)
        spinnerTipo = findViewById(R.id.edit_spinner_tipo_poi)
        imageView = findViewById(R.id.edit_image_poi)
        buttonNovaImagem = findViewById(R.id.btn_nova_imagem)
        buttonNovoAudio = findViewById(R.id.btn_novo_audio)
        buttonSalvar = findViewById(R.id.buttonSalvarAlteracoes)
        audioText = findViewById(R.id.selected_audio_name)

        firestore = FirebaseFirestore.getInstance()

        poiId = intent.getStringExtra("id") ?: return

        firestore.collection("POIs").document(poiId).get()
            .addOnSuccessListener { document ->
                val poi = document.toObject(Poi::class.java)
                if (poi != null) {
                    preencherCampos(poi)
                } else {
                    Toast.makeText(this, "Erro ao carregar POI", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }

        buttonNovaImagem.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
            startActivityForResult(intent, REQUEST_IMAGE_PICK)
        }

        buttonNovoAudio.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "audio/*" }
            startActivityForResult(intent, REQUEST_AUDIO_PICK)
        }

        buttonSalvar.setOnClickListener {
            guardarAlteracoes()
        }
    }

    private fun preencherCampos(poi: Poi) {
        editNome.setText(poi.nome)
        editDescricao.setText(poi.descricao)

        poi.imagemBase64?.let {
            val imageBytes = android.util.Base64.decode(it, android.util.Base64.DEFAULT)
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            imageView.setImageBitmap(bitmap)
        }

        audioText.text = if (!poi.audioBase64.isNullOrEmpty()) "Áudio existente" else "Nenhum áudio selecionado"

        val tiposPoi = arrayOf("Museu", "Monumento Histórico", "Praça", "Outro")
        spinnerTipo.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, tiposPoi)
        val tipoIndex = tiposPoi.indexOf(poi.tipo)
        if (tipoIndex >= 0) spinnerTipo.setSelection(tipoIndex)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK && data != null) {
            when (requestCode) {
                REQUEST_IMAGE_PICK -> {
                    novaImagemUri = data.data
                    imageView.setImageURI(novaImagemUri)
                }

                REQUEST_AUDIO_PICK -> {
                    novoAudioUri = data.data
                    audioText.text = novoAudioUri?.lastPathSegment ?: "Áudio selecionado"
                }
            }
        }
    }

    private fun uriToBase64(uri: Uri): String? {
        return contentResolver.openInputStream(uri)?.use { inputStream ->
            val bytes = inputStream.readBytes()
            android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
        }
    }

    private fun guardarAlteracoes() {
        val nome = editNome.text.toString().trim()
        val descricao = editDescricao.text.toString().trim()
        val tipo = spinnerTipo.selectedItem.toString()

        if (nome.isEmpty() || descricao.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos!", Toast.LENGTH_SHORT).show()
            return
        }

        firestore.collection("POIs").document(poiId).get().addOnSuccessListener { snapshot ->
            val poiOriginal = snapshot.toObject(Poi::class.java)
            if (poiOriginal != null) {
                val imagemBase64 = novaImagemUri?.let { uriToBase64(it) } ?: poiOriginal.imagemBase64
                val audioBase64 = novoAudioUri?.let { uriToBase64(it) } ?: poiOriginal.audioBase64

                val updates = mapOf(
                    "nome" to nome,
                    "descricao" to descricao,
                    "tipo" to tipo,
                    "imagemBase64" to imagemBase64,
                    "audioBase64" to audioBase64,
                    "latitude" to poiOriginal.latitude,
                    "longitude" to poiOriginal.longitude,
                    "id" to poiOriginal.id // manter o mesmo ID
                )

                firestore.collection("POIs").document(poiId).update(updates)
                    .addOnSuccessListener {
                        Toast.makeText(this, "POI atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Erro ao atualizar POI!", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }
}
