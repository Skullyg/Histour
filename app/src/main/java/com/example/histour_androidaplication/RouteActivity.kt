package com.example.histour_androidaplication

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.histour_androidaplication.models.DirectionsResponse
import com.example.histour_androidaplication.models.Poi
import com.example.histour_androidaplication.network.RetrofitClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.google.android.gms.maps.model.PatternItem
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.Gap
import kotlin.collections.isNullOrEmpty
import kotlin.collections.map

class RouteActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private var destination: LatLng? = null
    private var origin: LatLng? = null // Localização do utilizador
    private lateinit var travelTimeCar: TextView
    private lateinit var travelTimeWalk: TextView
    private lateinit var travelTimeTransit: TextView
    private var poiList: ArrayList<LatLng> = arrayListOf()
    companion object {
        val PATTERN_DASHED: List<PatternItem> = listOf(Dash(30f), Gap(20f))
    }
    private val drawnPolylines = mutableListOf<com.google.android.gms.maps.model.Polyline>()





    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_route)

        // Obter latitude e longitude do destino
        val latitude = intent.getDoubleExtra("latitude", 0.0)
        val longitude = intent.getDoubleExtra("longitude", 0.0)
        destination = LatLng(latitude, longitude)
        travelTimeCar = findViewById(R.id.travel_time_car)
        travelTimeWalk = findViewById(R.id.travel_time_walk)
        travelTimeTransit = findViewById(R.id.travel_time_transit)
        val selectedPOIs = intent.getParcelableArrayListExtra<Poi>("selectedPOIs")
        if (!selectedPOIs.isNullOrEmpty()) {
            poiList = selectedPOIs.map { LatLng(it.latitude, it.longitude) } as ArrayList<LatLng>
            destination = poiList.last() // Define o destino como o último POI
        }







        // Obter localização do utilizador passada via Intent
        val userLat = intent.getDoubleExtra("user_latitude", 0.0)
        val userLng = intent.getDoubleExtra("user_longitude", 0.0)
        origin = if (userLat != 0.0 && userLng != 0.0) LatLng(userLat, userLng) else null

        if (origin == null || (origin!!.latitude == 0.0 && origin!!.longitude == 0.0)) {
            Toast.makeText(this, "Erro ao obter a localização.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Verificar se os valores são válidos
        if (origin!!.latitude == 0.0 && origin!!.longitude == 0.0) {
            Toast.makeText(this, "Erro ao obter a localização.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        travelTimeCar.setOnClickListener {
            clearRoutes()
            if (poiList.size > 2)
                getMultiStopRoute("driving", Color.RED, travelTimeCar)
            else
                getRoute("driving", Color.RED, travelTimeCar)
        }

        travelTimeWalk.setOnClickListener {
            clearRoutes()
            if (poiList.size > 2)
                getMultiStopRoute("walking", Color.BLUE, travelTimeWalk)
            else
                getRoute("walking", Color.BLUE, travelTimeWalk)
        }

        travelTimeTransit.setOnClickListener {
            clearRoutes()
            if (poiList.size > 2)
                getMultiStopRoute("transit", Color.GREEN, travelTimeTransit)
            else
                getRoute("transit", Color.GREEN, travelTimeTransit)
        }


        // Iniciar o mapa
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this) ?: Log.e("RouteActivity", "Erro ao carregar mapa!")
    }


    override fun onMapReady(map: GoogleMap) {

        googleMap = map
        googleMap.addMarker(
            MarkerOptions()
                .position(origin!!)
                .title("Localização Atual")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
        )

        // Adiciona marcador no destino (ponto de interesse)
        googleMap.addMarker(
            MarkerOptions()
                .position(destination!!)
                .title("Destino")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
        )
        googleMap.uiSettings.isZoomControlsEnabled = true

        if (origin != null && destination != null) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(origin!!, 15f))

            if (poiList.size > 2) {
                getMultiStopRoute("driving", Color.RED, travelTimeCar)   // 🚗 Carro (vermelho)
                getMultiStopRoute("walking", Color.BLUE, travelTimeWalk) // 🚶 A pé (azul)
                getMultiStopRoute("transit", Color.GREEN, travelTimeTransit) // 🚌 Transportes públicos (verde)
            } else {
                getRoute("driving", Color.RED, travelTimeCar)
                getRoute("walking", Color.BLUE, travelTimeWalk)
                getRoute("transit", Color.GREEN, travelTimeTransit)
            }
        }
    }

    private fun getRoute(mode: String, color: Int, travelTimeTextView: TextView) {
        if (origin == null || destination == null) return

        val originStr = "${origin!!.latitude},${origin!!.longitude}"
        val destinationStr = "${destination!!.latitude},${destination!!.longitude}"
        val apiKey = "AIzaSyCHkw-jTpg1EWmXfM8kF3swnfYK2dZaFaA"

        RetrofitClient.instance.getDirections(originStr, destinationStr, mode, apiKey)
            .enqueue(object : Callback<DirectionsResponse> {
                override fun onResponse(
                    call: Call<DirectionsResponse>,
                    response: Response<DirectionsResponse>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        val route = response.body()?.routes?.firstOrNull()
                        val leg = route?.legs?.firstOrNull()

                        val duration = leg?.duration?.text ?: "Desconhecido"

                        // Atualiza a interface com o tempo estimado
                        runOnUiThread {
                            travelTimeTextView.text = duration

                        }

                        // Se a rota tiver uma polyline, desenhá-la no mapa
                        val points = route?.overviewPolyline?.points
                        if (!points.isNullOrEmpty()) {
                            drawRoute(points, color,mode)
                        } else {
                            Log.e("RouteActivity", "Nenhuma polyline recebida!")
                            Toast.makeText(this@RouteActivity, "Erro: Nenhuma rota encontrada.", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Log.e("RouteActivity", "Erro na resposta: ${response.errorBody()?.string()}")
                        Toast.makeText(this@RouteActivity, "Erro ao obter a rota.", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                    Toast.makeText(this@RouteActivity, "Erro ao conectar com a API.", Toast.LENGTH_LONG).show()
                }
            })
    }

    private fun getMultiStopRoute(mode: String, color: Int, travelTimeTextView: TextView) {
        val originStr = "${origin!!.latitude},${origin!!.longitude}"
        val destinationStr = "${destination!!.latitude},${destination!!.longitude}"
        val apiKey = "AIzaSyCHkw-jTpg1EWmXfM8kF3swnfYK2dZaFaA"

        // Criar string de waypoints excluindo o primeiro e o último ponto (origem e destino)
        val waypoints = poiList.drop(1).dropLast(1)
            .joinToString("|") { "${it.latitude},${it.longitude}" }

        RetrofitClient.instance.getDirectionsWithWaypoints(originStr, destinationStr, waypoints, mode, apiKey)
            .enqueue(object : Callback<DirectionsResponse> {
                @SuppressLint("SetTextI18n")
                override fun onResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {
                    if (response.isSuccessful) {
                        val route = response.body()?.routes?.firstOrNull()
                        val duration = route?.legs?.sumOf { it.duration.value } ?: 0
                        runOnUiThread { travelTimeTextView.text = "${duration / 60} min" }
                        route?.overviewPolyline?.points?.let { drawRoute(it, color, mode) }
                    }
                }

                override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                    Toast.makeText(this@RouteActivity, "Erro ao obter a rota.", Toast.LENGTH_LONG).show()
                }
            })
    }

    // 🔹 ✅ Agora a função drawRoute EXISTE e não dará erro!
    private fun drawRoute(encodedPolyline: String, color: Int, mode: String) {
        val decodedPath = decodePolyline(encodedPolyline)
        val polylineOptions = PolylineOptions()
            .addAll(decodedPath)
            .width(if (mode == "walking") 10f else 12f)
            .color(color)

        if (mode == "walking") {
            polylineOptions.pattern(PATTERN_DASHED)
        }

        val polyline = googleMap.addPolyline(polylineOptions)
        drawnPolylines.add(polyline)

        val boundsBuilder = com.google.android.gms.maps.model.LatLngBounds.Builder()
        for (point in decodedPath) {
            boundsBuilder.include(point)
        }
        googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100))
    }


    // 🔹 ✅ Agora a função decodePolyline EXISTE e não dará erro!
    private fun decodePolyline(encoded: String): List<LatLng> {
        val poly = mutableListOf<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1F shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1F shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val latLng = LatLng(lat / 1E5, lng / 1E5)
            poly.add(latLng)
        }
        return poly
    }

    private fun clearRoutes() {
        for (polyline in drawnPolylines) {
            polyline.remove()
        }
        drawnPolylines.clear()
    }

}

