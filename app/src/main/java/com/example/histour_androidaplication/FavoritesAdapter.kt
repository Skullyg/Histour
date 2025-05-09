package com.example.histour_androidaplication

import android.content.Context
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.example.histour_androidaplication.models.Poi
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FavoritesAdapter(private val context: Context, private val favoritos: List<Poi>) :
    android.widget.ArrayAdapter<Poi>(context, R.layout.item_favorite, favoritos) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = LayoutInflater.from(context)
        val view = convertView ?: inflater.inflate(R.layout.item_favorite, parent, false)

        val textNome = view.findViewById<TextView>(R.id.textFavoriteNome)
        val imageView = view.findViewById<ImageView>(R.id.imageFavorite)
        val buttonRemove = view.findViewById<ImageView>(R.id.buttonRemoveFavorite)

        val poi = favoritos[position]

        textNome.text = poi.nome

        // Mostrar imagem base64 (ou placeholder)
        if (!poi.imagemBase64.isNullOrEmpty()) {
            val imageBytes = Base64.decode(poi.imagemBase64, Base64.DEFAULT)
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            imageView.setImageBitmap(bitmap)
        } else {
            imageView.setImageResource(R.drawable.ic_launcher_background)
        }

        // Remover favorito
        buttonRemove.setOnClickListener {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId != null && poi.id.isNotEmpty()) {
                FirebaseFirestore.getInstance()
                    .collection("Utilizadores")
                    .document(userId)
                    .collection("Favoritos")
                    .document(poi.id)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(context, "Removido dos favoritos!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Erro ao remover favorito!", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        return view
    }
}
