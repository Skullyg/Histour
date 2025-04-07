package com.example.histour_androidaplication

import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.histour_androidaplication.models.Poi
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.maps.android.PolyUtil
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class MultiPOIActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private var selectedPOIs: List<Poi> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_multi_poi)

        selectedPOIs = intent.getParcelableArrayListExtra("selectedPOIs") ?: listOf()

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        if (selectedPOIs.size >= 2) {
            obterRotasTodosPOIs(selectedPOIs, "driving") { linha, duracao ->
                runOnUiThread {
                    linha?.let { googleMap.addPolyline(it) }
                    findViewById<TextView>(R.id.travel_time_car).text = "Carro: $duracao"
                }
            }

            obterRotasTodosPOIs(selectedPOIs, "walking") { linha, duracao ->
                runOnUiThread {
                    linha?.let { googleMap.addPolyline(it) }
                    findViewById<TextView>(R.id.travel_time_walk).text = "A pé: $duracao"
                }
            }

            obterRotasTodosPOIs(selectedPOIs, "transit") { linha, duracao ->
                runOnUiThread {
                    linha?.let { googleMap.addPolyline(it) }
                    findViewById<TextView>(R.id.travel_time_transit).text = "Transportes: $duracao"
                }
            }
        }

    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        if (selectedPOIs.isNotEmpty()) {
            selectedPOIs.forEach { poi ->
                val location = LatLng(poi.latitude, poi.longitude)
                googleMap.addMarker(MarkerOptions().position(location).title(poi.nome))
            }

            val firstLocation = LatLng(selectedPOIs[0].latitude, selectedPOIs[0].longitude)
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(firstLocation, 14f))
        }
    }

    fun obterRotasTodosPOIs(pois: List<Poi>, modo: String, callback: (PolylineOptions?, String?) -> Unit) {
        val apiKey = "AIzaSyCHkw-jTpg1EWmXfM8kF3swnfYK2dZaFaA"

        if (pois.size < 2) return  // precisamos de pelo menos 2 pontos

        val origem = "${pois.first().latitude},${pois.first().longitude}"
        val destino = "${pois.last().latitude},${pois.last().longitude}"
        val waypoints = pois.subList(1, pois.size - 1)
            .joinToString("|") { "${it.latitude},${it.longitude}" }

        val url = StringBuilder("https://maps.googleapis.com/maps/api/directions/json?")
        url.append("origin=$origem")
        url.append("&destination=$destino")
        if (waypoints.isNotEmpty()) {
            url.append("&waypoints=$waypoints")
        }
        url.append("&mode=$modo")
        url.append("&key=$apiKey")

        val client = OkHttpClient()
        val request = Request.Builder().url(url.toString()).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body()?.string()
                if (body != null) {
                    val json = JSONObject(body)
                    val routes = json.getJSONArray("routes")
                    if (routes.length() > 0) {
                        val route = routes.getJSONObject(0)
                        val overviewPolyline = route.getJSONObject("overview_polyline").getString("points")
                        val duration = route.getJSONArray("legs")
                            .let { legs ->
                                var totalSeconds = 0
                                for (i in 0 until legs.length()) {
                                    totalSeconds += legs.getJSONObject(i).getJSONObject("duration").getInt("value")
                                }
                                // converte para horas e minutos
                                val minutes = totalSeconds / 60
                                "${minutes} min"
                            }

                        val decodedPath = PolyUtil.decode(overviewPolyline)
                        val polylineOptions = PolylineOptions()
                            .addAll(decodedPath)
                            .color(
                                when (modo) {
                                    "driving" -> Color.BLUE
                                    "walking" -> Color.GREEN
                                    else -> Color.RED
                                }
                            )
                            .width(8f)

                        if (modo == "walking") {
                            polylineOptions.pattern(listOf(
                                com.google.android.gms.maps.model.Dot(),
                                com.google.android.gms.maps.model.Gap(10f)
                            ))
                        }

                        callback(polylineOptions, duration)
                    }
                }
            }
        })
    }

}



