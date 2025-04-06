package com.example.histour_androidaplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.histour_androidaplication.models.Comentario

class ComentariosAdapter(private val comentarios: List<Comentario>) :
    RecyclerView.Adapter<ComentariosAdapter.ComentarioViewHolder>() {

    class ComentarioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textUser = itemView.findViewById<TextView>(R.id.textUser)
        val textComentario = itemView.findViewById<TextView>(R.id.textComentario)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComentarioViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comentario, parent, false)
        return ComentarioViewHolder(view)
    }

    override fun onBindViewHolder(holder: ComentarioViewHolder, position: Int) {
        val comentario = comentarios[position]
        holder.textUser.text = "User: ${comentario.userId.take(6)}..." // Exemplo
        holder.textComentario.text = comentario.comentario
    }

    override fun getItemCount(): Int = comentarios.size
}
