package com.example.histour_androidaplication

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.histour_androidaplication.models.Poi
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
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
    private lateinit var storage: FirebaseStorage

    private var poiId: String = ""
    private var poiImagemUrl: String? = null
    private var poiAudioUrl: String? = null

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
        storage = FirebaseStorage.getInstance()

        val nome = intent.getStringExtra("nome") ?: return
        val descricao = intent.getStringExtra("descricao") ?: ""
        val imagemUrl = intent.getStringExtra("imagemUrl")
        val audioUrl = intent.getStringExtra("audioUrl")
        val latitude = intent.getDoubleExtra("latitude", 0.0)
        val longitude = intent.getDoubleExtra("longitude", 0.0)
        val tipo = intent.getStringExtra("tipo") ?: "Outro"

        val poi = Poi(
            nome = nome,
            descricao = descricao,
            imagemUrl = imagemUrl,
            audioUrl = audioUrl,
            latitude = latitude,
            longitude = longitude,
            tipo = tipo
        )

        poiId = poi.nome
        editNome.setText(poi.nome)
        editDescricao.setText(poi.descricao)
        poiImagemUrl = poi.imagemUrl
        poiAudioUrl = poi.audioUrl

        Glide.with(this).load(poiImagemUrl).into(imageView)
        audioText.text = if (poiAudioUrl != null) "Áudio existente" else "Nenhum áudio selecionado"

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

    private fun guardarAlteracoes(poiOriginal: Poi) {
        val nome = editNome.text.toString().trim()
        val descricao = editDescricao.text.toString().trim()
        val tipo = spinnerTipo.selectedItem.toString()

        if (nome.isEmpty() || descricao.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos!", Toast.LENGTH_SHORT).show()
            return
        }

        val nomeAntigo = poiOriginal.nome
        val poiRefNovo = firestore.collection("POIs").document(nome)
        val poiRefAntigo = firestore.collection("POIs").document(nomeAntigo)


        if (novaImagemUri != null) {
            // Apagar imagem antiga se existir
            poiImagemUrl?.let { apagarFicheiroStorage(it) }

            val imagemRef = storage.reference.child("poi_imagens/${UUID.randomUUID()}.jpg")
            imagemRef.putFile(novaImagemUri!!)
                .continueWithTask { it.result.storage.downloadUrl }
                .addOnSuccessListener { uri ->
                    poiImagemUrl = uri.toString()
                    if (novoAudioUri != null) {
                        poiAudioUrl?.let { apagarFicheiroStorage(it) } // Apagar áudio antigo
                        uploadAudio(poiRefNovo, nome, descricao, tipo)
                    } else {
                        salvarNoFirestore(poiRefNovo, nome, descricao, tipo)
                    }
                }
        } else if (novoAudioUri != null) {
            poiAudioUrl?.let { apagarFicheiroStorage(it) } // Apagar áudio antigo
            uploadAudio(poiRefNovo, nome, descricao, tipo)
        } else {
            salvarNoFirestore(poiRefNovo, nome, descricao, tipo)
        }

    }

    private fun uploadAudio(
        poiRef: com.google.firebase.firestore.DocumentReference,
        nome: String,
        descricao: String,
        tipo: String
    ) {
        val audioRef = storage.reference.child("poi_audios/${UUID.randomUUID()}.mp3")
        audioRef.putFile(novoAudioUri!!)
            .continueWithTask { it.result.storage.downloadUrl }
            .addOnSuccessListener { uri ->
                poiAudioUrl = uri.toString()
                salvarNoFirestore(poiRef, nome, descricao, tipo)
            }
    }

    private fun salvarNoFirestore(
        poiRefNovo: com.google.firebase.firestore.DocumentReference,
        nome: String,
        descricao: String,
        tipo: String
    ) {
        val updates = mapOf(
            "nome" to nome,
            "descricao" to descricao,
            "tipo" to tipo,
            "imagemUrl" to poiImagemUrl,
            "audioUrl" to poiAudioUrl,
            "latitude" to intent.getDoubleExtra("latitude", 0.0),
            "longitude" to intent.getDoubleExtra("longitude", 0.0)
        )

        val nomeAntigo = intent.getStringExtra("nome") ?: return
        val poiRefAntigo = firestore.collection("POIs").document(nomeAntigo)

        // Se o nome mudou, cria novo e apaga o antigo
        if (nome != nomeAntigo) {
            poiRefNovo.set(updates)
                .addOnSuccessListener {
                    poiRefAntigo.delete()
                    Toast.makeText(this, "POI atualizado com novo nome!", Toast.LENGTH_SHORT).show()
                    val resultIntent = Intent().apply {
                        putExtra("nome", nome)
                        putExtra("descricao", descricao)
                        putExtra("imagemUrl", poiImagemUrl)
                        putExtra("audioUrl", poiAudioUrl)
                        putExtra("tipo", tipo)
                    }
                    setResult(Activity.RESULT_OK, resultIntent)

                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Erro ao atualizar POI!", Toast.LENGTH_SHORT).show()
                }
        } else {
            poiRefNovo.update(updates)
                .addOnSuccessListener {
                    Toast.makeText(this, "POI atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                    atualizarReferenciasUtilizador(nomeAntigo, nome)
                    val resultIntent = Intent().apply {
                        putExtra("nome", nome)
                        putExtra("descricao", descricao)
                        putExtra("imagemUrl", poiImagemUrl)
                        putExtra("audioUrl", poiAudioUrl)
                        putExtra("tipo", tipo)
                    }
                    setResult(Activity.RESULT_OK, resultIntent)

                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Erro ao atualizar POI!", Toast.LENGTH_SHORT).show()
                }
        }

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



    private fun apagarFicheiroStorage(url: String) {
        val ref = storage.getReferenceFromUrl(url)
        ref.delete()
            .addOnSuccessListener {
                Log.d("EditPoi", "Ficheiro apagado: $url")
            }
            .addOnFailureListener {
                Log.w("EditPoi", "Erro ao apagar ficheiro: $url", it)
            }
    }
}

