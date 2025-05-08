package com.example.histour_androidaplication.models

data class Favorito(
    val poiId: String = "",
    val nome: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val imagemBase64: String = ""
)
