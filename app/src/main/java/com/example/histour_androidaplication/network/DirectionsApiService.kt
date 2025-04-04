package com.example.histour_androidaplication.network

import com.example.histour_androidaplication.models.DirectionsResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query


interface DirectionsApiService {
    @GET("directions/json")
    fun getDirections(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("mode") mode: String = "walking", // Pode ser driving, walking, bicycling, transit
        @Query("key") apiKey: String
    ): Call<DirectionsResponse>

    // ✅ Adicione esta função para suportar múltiplos pontos de passagem (waypoints)
    @GET("directions/json")
    fun getDirectionsWithWaypoints(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("waypoints") waypoints: String,  // 🚀 Waypoints adicionados
        @Query("mode") mode: String,
        @Query("key") apiKey: String
    ): Call<DirectionsResponse>
}
