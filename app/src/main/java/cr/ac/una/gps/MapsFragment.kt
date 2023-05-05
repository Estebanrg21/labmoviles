package cr.ac.una.gps

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import androidx.fragment.app.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.lifecycle.lifecycleScope

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.maps.android.PolyUtil
import cr.ac.una.roomdb.UbicacionDao
import cr.ac.una.roomdb.db.AppDatabase
import cr.ac.una.roomdb.entity.Ubicacion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.collections.ArrayList

class MapsFragment : FragmentWithLocationPermissions() {
    private lateinit var locationReceiver: BroadcastReceiver
    private lateinit var ubicacionDao: UbicacionDao
    private lateinit var map: GoogleMap
    private lateinit var polygon: Polygon
    private lateinit var datepicker: MaterialDatePicker<Long>
    private lateinit var markers: ArrayList<Marker?>
    private val callRequestPermissionCode: Int = 123456789

    private companion object CONSTANTS {
        val DEFAULT_MAP_ZOOM: Float = 9f
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
        markers = ArrayList()

        datepicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Seleccione una fecha")
            .build()
        datepicker.addOnPositiveButtonClickListener(this::showStoredLocationsByDate)
        activity?.findViewById<Button>(R.id.select_by_date_btn)?.setOnClickListener {
            datepicker.show(parentFragmentManager, "datepicker")
        }
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
                    createMarker(newLatLng)
                )
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(newLatLng, DEFAULT_MAP_ZOOM))
                if (isLocationInsidePolygon(newLatLng)) {
                    makePhoneCallWithPermission()
                }
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

    private fun iniciaServicio() {
        if (checkPermissions()) {
            val intent = Intent(context, LocationService::class.java)
            context?.startService(intent)
        } else {
            askForLocationPermissions()
        }
    }

    override fun onLocationPermissionsGranted() {
        iniciaServicio()
    }

    private fun mapReadyCallback(googleMap: GoogleMap) {
        map = googleMap
        map.uiSettings.isZoomControlsEnabled = true
        createPolygon()
        if (checkPermissions()) {
            showStoredLocations()
        }
    }

    private fun removeStoredLocationMarkers() {
        markers.forEach { marker -> marker?.remove() }
        markers.clear()
    }

    private fun showStoredLocationsByDate(timestamp: Long) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                ubicacionDao.getAllByDate(timestamp).let { list ->
                    withContext(Dispatchers.Main) {
                        if (list != null) {
                            if (list.size > 0) {
                                removeStoredLocationMarkers()
                                list.forEach { ubicacion ->
                                    if (ubicacion != null) {
                                        markers.add(
                                            map.addMarker(
                                                createMarker(
                                                    LatLng(
                                                        ubicacion.latitud,
                                                        ubicacion.longitud
                                                    )
                                                )
                                            )
                                        )
                                    }
                                }
                            } else {
                                Toast.makeText(
                                    activity,
                                    "No existen ubicaciones registradas para la fecha indicada",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showStoredLocations() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                ubicacionDao.getAll()?.forEach { ubicacion ->
                    if (ubicacion != null) {
                        withContext(Dispatchers.Main) {
                            markers.add(
                                map.addMarker(
                                    createMarker(
                                        LatLng(ubicacion.latitud, ubicacion.longitud)
                                    )
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    private fun createMarker(coords: LatLng): MarkerOptions {
        val markOpts = MarkerOptions().position(coords)
        if (isLocationInsidePolygon(coords)) {
            markOpts.icon(
                BitmapDescriptorFactory
                    .defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
            )
        }
        return markOpts
    }

    private fun saveLocation(lat: Double, lng: Double) {
        val entity = Ubicacion(
            id = null,
            latitud = lat,
            longitud = lng,
            fecha = Date(),
            isInPoligon = isLocationInsidePolygon(LatLng(lat, lng)),
            isPointPolygon = false
        )
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                ubicacionDao.insert(entity)
            }
        }
    }


    private fun isLocationInsidePolygon(location: LatLng): Boolean {
        return polygon != null && PolyUtil.containsLocation(location, polygon.points, true)
    }

    private fun createPolygon() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                ubicacionDao.getAllPolygonPoints()?.let { ubicaciones ->
                    withContext(Dispatchers.Main) {
                        val polygonOptions = PolygonOptions()
                        ubicaciones.forEach { ubicacion ->
                            if (ubicacion != null) {
                                val latlng = LatLng(ubicacion.latitud, ubicacion.longitud)
                                polygonOptions.add(latlng)
                            }
                        }
                        polygon = map.addPolygon(polygonOptions)
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            && requestCode == callRequestPermissionCode
        ) {
            makePhoneCall()
        } else {
            // El usuario no concedió el permiso
        }
    }

    fun makePhoneCall() {
        val intent = Intent(Intent.ACTION_CALL)
        if ((activity as MainActivity).telefonoEmergencia != null) {
            val phoneNumber = (activity as MainActivity).telefonoEmergencia
            intent.data = Uri.parse("tel:$phoneNumber")
            startActivity(intent)
        }
    }


    fun makePhoneCallWithPermission() {
        val permission = android.Manifest.permission.CALL_PHONE
        if (ContextCompat.checkSelfPermission(requireContext(), permission)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(requireActivity(),
                arrayOf(permission), 1)
        } else {
            makePhoneCall()
        }

    }
}