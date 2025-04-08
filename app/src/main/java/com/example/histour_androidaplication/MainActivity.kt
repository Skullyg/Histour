package com.example.histour_androidaplication

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.histour_androidaplication.models.Poi
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var searchBar: AutoCompleteTextView
    private lateinit var menuButton: ImageView
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val db = FirebaseFirestore.getInstance()
    private var currentLocation: LatLng? = null
    private val CREATE_POI_REQUEST = 1
    private val poiList = mutableListOf<Poi>()
    private lateinit var searchAdapter: ArrayAdapter<String>
    private var firestoreListener: ListenerRegistration? = null
    private var isAdmin: Boolean = false


    private val portoLocation = LatLng(41.1496, -8.6109) // 📌 Localização padrão: Porto

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawerLayout = findViewById(R.id.drawer_layout)
        searchBar = findViewById(R.id.search_bar)
        menuButton = findViewById(R.id.menu_button)
        val navView: NavigationView = findViewById(R.id.nav_view)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Configurar barra de pesquisa
        searchAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, mutableListOf())
        searchBar.setAdapter(searchAdapter)

        searchBar.setOnItemClickListener { _, _, position, _ ->
            val selectedPoiName = searchAdapter.getItem(position)
            val selectedPoi = poiList.find { it.nome == selectedPoiName }

            if (::googleMap.isInitialized && selectedPoi != null) {
                val poiLocation = LatLng(selectedPoi.latitude, selectedPoi.longitude)
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(poiLocation, 17f))
            }
        }

        menuButton.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        navView.setNavigationItemSelectedListener(this)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        requestLocationPermission()

        // 🔹 Atualizar POIs automaticamente ao adicionar/remover no Firestore
        firestoreListener = db.collection("POIs")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Toast.makeText(this, "Erro ao atualizar POIs!", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                loadPoisFromFirestore()
            }

        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.uid?.let { uid ->
            db.collection("Utilizadores").document(uid).get()
                .addOnSuccessListener { document ->
                    val tipo = document.getString("tipo")
                    isAdmin = tipo == "admin"
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Erro ao verificar tipo de utilizador", Toast.LENGTH_SHORT).show()
                }
        }


    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.uiSettings.isZoomControlsEnabled = true

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.isMyLocationEnabled = true
            getLocation()
        } else {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(portoLocation, 15f))
        }

        loadPoisFromFirestore()

        googleMap.setOnMarkerClickListener { marker ->
            if (marker.tag is Poi) {
                val poi = marker.tag as Poi
                val intent = Intent(this, PoiDetailActivity::class.java)

                intent.putExtra("nome", poi.nome)
                intent.putExtra("descricao", poi.descricao)
                intent.putExtra("latitude", poi.latitude)
                intent.putExtra("longitude", poi.longitude)
                intent.putExtra("imagemUrl", poi.imagemUrl ?: "")
                intent.putExtra("tipo", poi.tipo)  // ✅ Adiciona esta linha!

                startActivity(intent)
                true
            } else {
                false
            }
        }

        googleMap.setOnMapClickListener { latLng ->
            if (isAdmin) {
                showCreatePoiDialog(latLng)
            } else {
                Toast.makeText(this, "Apenas administradores podem criar POIs.", Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }
    }

    private fun getLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                currentLocation = if (location != null && isLocationInPorto(location.latitude, location.longitude)) {
                    LatLng(location.latitude, location.longitude)
                } else {
                    portoLocation // 📍 Define o centro do Porto como localização padrão
                }

                googleMap.let {
                    it.clear()
                    it.addMarker(MarkerOptions().position(currentLocation!!).title("Minha Localização"))
                    it.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation!!, 15f))
                    loadPoisFromFirestore()
                }
            }
        }
    }


    private fun isLocationInPorto(latitude: Double, longitude: Double): Boolean {
        val latMin = 41.1200
        val latMax = 41.1900
        val lngMin = -8.6600
        val lngMax = -8.5600

        return latitude in latMin..latMax && longitude in lngMin..lngMax
    }

    private fun loadPoisFromFirestore() {
        db.collection("POIs").get()
            .addOnSuccessListener { result ->
                googleMap.clear()
                poiList.clear()

                currentLocation?.let {
                    googleMap.addMarker(MarkerOptions().position(it).title("Minha Localização"))
                }

                val poiNames = mutableListOf<String>()

                for (document in result) {
                    val poi = document.toObject(Poi::class.java)
                    poiList.add(poi)
                    poiNames.add(poi.nome)

                    val position = LatLng(poi.latitude, poi.longitude)
                    val markerIcon = BitmapDescriptorFactory.fromResource(R.drawable.marker_orange) // <- usa ícone laranja
                    val marker = googleMap.addMarker(
                        MarkerOptions()
                            .position(position)
                            .title(poi.nome)
                            .icon(markerIcon)
                    )
                    marker?.tag = poi
                }

                searchAdapter.clear()
                searchAdapter.addAll(poiNames)
                searchAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao carregar POIs!", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showCreatePoiDialog(latLng: LatLng) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Criar novo POI")
        builder.setMessage("Deseja adicionar um novo Ponto de Interesse aqui?")

        builder.setPositiveButton("Sim") { _, _ ->
            val intent = Intent(this, CreatePoiActivity::class.java)
            intent.putExtra("latitude", latLng.latitude)
            intent.putExtra("longitude", latLng.longitude)
            startActivityForResult(intent, CREATE_POI_REQUEST)
        }

        builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getLocation()
        } else {
            Toast.makeText(this, "Permissão de localização negada!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        firestoreListener?.remove()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_favorites -> {
                val intent = Intent(this, FavoritesActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_route -> {
                val intent = Intent(this, PlaneadorDeRotaActivity::class.java)
                startActivity(intent)
            }


            R.id.nav_logout -> {
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
            else -> return false
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }




}
