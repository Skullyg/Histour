package com.example.histour_androidaplication

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.histour_androidaplication.models.Poi
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

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

        val nome = intent.getStringExtra("nome") ?: return
        val descricao = intent.getStringExtra("descricao") ?: ""
        val imagemBase64 = intent.getStringExtra("imagemBase64")
        val audioBase64 = intent.getStringExtra("audioBase64")
        val latitude = intent.getDoubleExtra("latitude", 0.0)
        val longitude = intent.getDoubleExtra("longitude", 0.0)
        val tipo = intent.getStringExtra("tipo") ?: "Outro"

        val poi = Poi(
            nome = nome,
            descricao = descricao,
            imagemBase64 = imagemBase64,
            audioBase64 = audioBase64,
            latitude = latitude,
            longitude = longitude,
            tipo = tipo
        )


        poiId = poi.nome
        editNome.setText(poi.nome)
        editDescricao.setText(poi.descricao)
        poi.imagemBase64?.let {
            val imageBytes = android.util.Base64.decode(it, android.util.Base64.DEFAULT)
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            imageView.setImageBitmap(bitmap)
        }

        audioText.text = if (!poi.audioBase64.isNullOrEmpty()) "Áudio existente" else "Nenhum áudio selecionado"

        // Spinner
        val tiposPoi = arrayOf("Museu", "Monumento Histórico", "Praça", "Outro")
        spinnerTipo.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, tiposPoi)
        val tipoIndex = tiposPoi.indexOf(poi.tipo)
        if (tipoIndex >= 0) spinnerTipo.setSelection(tipoIndex)

        buttonNovaImagem.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
            startActivityForResult(intent, REQUEST_IMAGE_PICK)
        }

        buttonNovoAudio.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "audio/*" }
            startActivityForResult(intent, REQUEST_AUDIO_PICK)
        }

        buttonSalvar.setOnClickListener {
            guardarAlteracoes(poi)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && data != null) {
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

    private fun guardarAlteracoes(poiOriginal: Poi) {
        val nome = editNome.text.toString().trim()
        val descricao = editDescricao.text.toString().trim()
        val tipo = spinnerTipo.selectedItem.toString()

        if (nome.isEmpty() || descricao.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos!", Toast.LENGTH_SHORT).show()
            return
        }

        val imagemBase64 = novaImagemUri?.let { uriToBase64(it) } ?: poiOriginal.imagemBase64
        val audioBase64 = novoAudioUri?.let { uriToBase64(it) } ?: poiOriginal.audioBase64

        val updates = mapOf(
            "nome" to nome,
            "descricao" to descricao,
            "tipo" to tipo,
            "imagemBase64" to imagemBase64,
            "audioBase64" to audioBase64,
            "latitude" to poiOriginal.latitude,
            "longitude" to poiOriginal.longitude
        )

        val nomeAntigo = poiOriginal.nome
        val refAntigo = firestore.collection("POIs").document(nomeAntigo)
        val refNovo = firestore.collection("POIs").document(nome)

        if (nome != nomeAntigo) {
            refNovo.set(updates).addOnSuccessListener {
                refAntigo.delete()
                atualizarReferenciasUtilizador(nomeAntigo, nome)
                enviarResultado(nome, descricao, tipo, imagemBase64, audioBase64)
            }
        } else {
            refNovo.update(updates).addOnSuccessListener {
                enviarResultado(nome, descricao, tipo, imagemBase64, audioBase64)
            }.addOnFailureListener {
                Toast.makeText(this, "Erro ao atualizar POI!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun enviarResultado(
        nome: String,
        descricao: String,
        tipo: String,
        imagemBase64: String?,
        audioBase64: String?
    ) {
        val resultIntent = Intent().apply {
            putExtra("nome", nome)
            putExtra("descricao", descricao)
            putExtra("tipo", tipo)
            putExtra("imagemBase64", imagemBase64)
            putExtra("audioBase64", audioBase64)
        }
        setResult(Activity.RESULT_OK, resultIntent)
        Toast.makeText(this, "POI atualizado com sucesso!", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun atualizarReferenciasUtilizador(nomeAntigo: String, nomeNovo: String) {
        val utilizadoresRef = firestore.collection("Utilizadores")
        utilizadoresRef.get().addOnSuccessListener { result ->
            for (userDoc in result) {
                val uid = userDoc.id

                // Atualizar nos Favoritos
                val favoritoRef = utilizadoresRef.document(uid).collection("Favoritos").document(nomeAntigo)
                favoritoRef.get().addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val dados = doc.data
                        if (dados != null) {
                            val novoDados = HashMap(dados)
                            novoDados["nome"] = nomeNovo
                            utilizadoresRef.document(uid).collection("Favoritos").document(nomeNovo).set(novoDados)
                            favoritoRef.delete()
                        }
                    }
                }

                // Atualizar nas Visitas
                val visitaRef = utilizadoresRef.document(uid).collection("Visitas").document(nomeAntigo)
                visitaRef.get().addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val dados = doc.data
                        if (dados != null) {
                            val novoDados = HashMap(dados)
                            novoDados["nome"] = nomeNovo
                            utilizadoresRef.document(uid).collection("Visitas").document(nomeNovo).set(novoDados)
                            visitaRef.delete()
                        }
                    }
                }
            }
        }
    }
}

