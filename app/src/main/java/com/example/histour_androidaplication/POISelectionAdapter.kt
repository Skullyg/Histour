package com.example.histour_androidaplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.histour_androidaplication.models.Poi

class POISelectionAdapter(
    private val poiList: List<Poi>,
    private val onSelectionChanged: (List<Poi>) -> Unit
) : RecyclerView.Adapter<POISelectionAdapter.POIViewHolder>() {

    private val selectedPOIs = mutableListOf<Poi>()

    inner class POIViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val checkBox: CheckBox = view.findViewById(R.id.checkbox)
        val nomeText: TextView = view.findViewById(R.id.poi_nome)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): POIViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_poi_checkbox, parent, false)
        return POIViewHolder(view)
    }

    override fun onBindViewHolder(holder: POIViewHolder, position: Int) {
        val poi = poiList[position]
        holder.nomeText.text = poi.nome
        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.isChecked = selectedPOIs.contains(poi)

        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) selectedPOIs.add(poi) else selectedPOIs.remove(poi)
            onSelectionChanged(selectedPOIs)
        }
    }

    override fun getItemCount() = poiList.size
}
