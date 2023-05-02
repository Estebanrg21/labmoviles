package cr.ac.una.gps

import android.Manifest
import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.location.Location
import androidx.fragment.app.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polygon
import com.google.android.gms.maps.model.PolygonOptions
import com.google.maps.android.PolyUtil
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
    private lateinit var polygon: Polygon

    private companion object CONSTANTS {
        val DEFAULT_MAP_ZOOM: Float = 15f
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
        mapFragment?.getMapAsync(this::mapReadyCallback)
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
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(newLatLng, DEFAULT_MAP_ZOOM))
                println("" + latitud + "    " + longitud)

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
            if (grantResults.isNotEmpty() && grantResults[0] == PERMISSION_GRANTED) {
                if (checkPermissions()) {
                    iniciaServicio()
                }
            } else {
                // Permiso denegado, maneja la situación de acuerdo a tus necesidades
            }
        }
    }

    private fun iniciaServicio() {
        if (checkPermissions()) {
            val intent = Intent(context, LocationService::class.java)
            context?.startService(intent)
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION),
                1
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun mapReadyCallback(googleMap: GoogleMap) {
        map = googleMap
        map.uiSettings.isZoomControlsEnabled = true
        polygon = createPolygon()
        if (checkPermissions()) {
            showStoredLocations()
        }
    }

    private fun showStoredLocations() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                ubicacionDao.getAll()?.forEach { ubicacion ->
                    if (ubicacion != null) {
                        withContext(Dispatchers.Main) {
                            map.addMarker(
                                MarkerOptions().position(
                                    LatLng(ubicacion.latitud, ubicacion.longitud)
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    private fun saveLocation(lat: Double, lng: Double) {
        val entity = Ubicacion(
            id = null,
            latitud = lat,
            longitud = lng,
            fecha = Date(),
            isInPoligon = isLocationInsidePolygon(LatLng(lat, lng))
        )
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                ubicacionDao.insert(entity)
            }
        }
    }


    private fun isLocationInsidePolygon(location: LatLng): Boolean {
        return polygon != null && PolyUtil.containsLocation(location, polygon?.points, true)
    }

    private fun createPolygon(): Polygon {
        val polygonOptions = PolygonOptions()
        polygonOptions.add(LatLng(10.1584697, -84.2370056))
        polygonOptions.add(LatLng(9.9407624, -84.3935608))
        polygonOptions.add(LatLng(9.6944838, -83.9335083))
        polygonOptions.add(LatLng(10.0597759, -83.7769531))
        polygonOptions.add(LatLng(10.1584697, -84.2370056))
        return map.addPolygon(polygonOptions)
    }

    private fun checkPermissions(): Boolean {
        return checkSelfPermission(
            requireContext(),
            ACCESS_FINE_LOCATION
        ) == PERMISSION_GRANTED
                && checkSelfPermission(
            requireContext(),
            ACCESS_COARSE_LOCATION
        ) == PERMISSION_GRANTED
    }
}