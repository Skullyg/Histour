package com.example.histour_androidaplication.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Poi(
    val nome: String = "",
    val descricao: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val imagemUrl: String? = null,
    val tipo: String = "Outro",
    val audioUrl: String? = null
) : Parcelable
