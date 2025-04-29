package com.example.histour_androidaplication

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
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

        val poi = favoritos[position]

        textNome.text = poi.nome

        if (!poi.imagemBase64.isNullOrEmpty()) {
            val imageBytes = android.util.Base64.decode(poi.imagemBase64, android.util.Base64.DEFAULT)
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            imageView.setImageBitmap(bitmap)
        } else {
            imageView.setImageResource(R.drawable.ic_launcher_background)
        }


        // Remover favorito ao clicar no ícone
        view.findViewById<ImageView>(R.id.buttonRemoveFavorite).setOnClickListener {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId != null) {
                FirebaseFirestore.getInstance().collection("Utilizadores")
                    .document(userId)
                    .collection("Favoritos")
                    .document(poi.nome)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(context, "Removido dos favoritos!", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        return view
    }
}
