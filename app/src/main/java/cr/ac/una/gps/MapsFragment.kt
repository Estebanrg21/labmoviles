package cr.ac.una.gps

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import androidx.fragment.app.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import cr.ac.una.roomdb.UbicacionDao
import cr.ac.una.roomdb.db.AppDatabase
import cr.ac.una.roomdb.entity.Ubicacion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class MapsFragment : Fragment() {
    private lateinit var locationReceiver: BroadcastReceiver
    private lateinit var ubicacionDao: UbicacionDao
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    @SuppressLint("MissingPermission")
    private val callback = OnMapReadyCallback { googleMap ->
        /**
         * Manipulates the map once available.
         * This callback is triggered when the map is ready to be used.
         * This is where we can add markers or lines, add listeners or move the camera.
         * In this case, we just add a marker near Sydney, Australia.
         * If Google Play services is not installed on the device, the user will be prompted to
         * install it inside the SupportMapFragment. This method will only be triggered once the
         * user has installed Google Play services and returned to the app.
         */

        val sydney = LatLng(-34.0, 151.0)
        map = googleMap
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

        } else {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    ubicacionDao.getAll()?.forEach {
                        if (it != null) {
                            withContext(Dispatchers.Main) {
                                map.addMarker(
                                    MarkerOptions().position(LatLng(it.latitud, it.longitud))
                                )
                            }
                        }
                    }
                }
            }
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                // Ubicación obtenida con éxito
                if (location != null) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    map.addMarker(
                        MarkerOptions().position(currentLatLng)
                    )
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_maps, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)
        iniciaServicio()
        ubicacionDao = AppDatabase.getInstance(activity as AppCompatActivity).ubicacionDao()
        locationReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val latitud = intent?.getDoubleExtra("latitud", 0.0) ?: 0.0
                val longitud = intent?.getDoubleExtra("longitud", 0.0) ?: 0.0
                saveLocation(latitud, longitud)
                val newLatLng = LatLng(latitud, longitud)
                map.addMarker(
                    MarkerOptions()
                        .position(newLatLng)
                )
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(newLatLng, 15f))
                println(latitud.toString() + "    " + longitud)

            }
        }
        context?.registerReceiver(locationReceiver, IntentFilter("ubicacionActualizada"))


    }

    override fun onResume() {
        super.onResume()
        // Registrar el receptor para recibir actualizaciones de ubicación
        context?.registerReceiver(locationReceiver, IntentFilter("ubicacionActualizada"))
    }

    override fun onPause() {
        super.onPause()
        // Desregistrar el receptor al pausar el fragmento
        context?.unregisterReceiver(locationReceiver)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    iniciaServicio()
                }
            } else {
                // Permiso denegado, maneja la situación de acuerdo a tus necesidades
            }
        }
    }

    private fun iniciaServicio() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                1
            )
        } else {
            val intent = Intent(context, LocationService::class.java)
            context?.startService(intent)
        }
    }

    private fun saveLocation(lat: Double, lng: Double) {
        val entity = Ubicacion(
            id = null,
            latitud = lat,
            longitud = lng,
            fecha = Date()
        )
        insertEntity(entity)
    }

    private fun insertEntity(entity: Ubicacion) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                ubicacionDao.insert(entity)
            }
        }

    }
}