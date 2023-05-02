package cr.ac.una.gps


import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory


import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import cr.ac.una.gps.databinding.FragmentAreaRestringidaBinding
import cr.ac.una.gps.entity.PolygonPoint
import cr.ac.una.gps.utils.Util
import cr.ac.una.roomdb.UbicacionDao
import cr.ac.una.roomdb.db.AppDatabase
import cr.ac.una.roomdb.entity.Ubicacion
import kotlinx.coroutines.*

class AreaRestringidaFragment : FragmentWithLocationPermissions(), GoogleMap.OnMarkerDragListener,
    GoogleMap.OnMarkerClickListener {
    private lateinit var ubicacionDao: UbicacionDao
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var map: GoogleMap
    private lateinit var points: ArrayList<PolygonPoint>
    private var _binding: FragmentAreaRestringidaBinding? = null
    private val binding get() = _binding!!
    private var isEditing: Boolean = false
    private var polygon: Polygon? = null

    private companion object CONSTANTS {
        val DEFAULT_MAP_ZOOM: Float = 10f
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
        _binding = FragmentAreaRestringidaBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ubicacionDao = AppDatabase.getInstance(activity as AppCompatActivity).ubicacionDao()
        points = ArrayList()
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this::mapReadyCallback)
        binding.addMarkerBtn.setOnClickListener { this.addMarker() }
        binding.saveChangesBtn.setOnClickListener { this.savePoints() }
        binding.editPointsBtn.setOnClickListener { this.setEditing(!isEditing) }
    }

    override fun onResume() {
        super.onResume()
    }

    private fun mapReadyCallback(googleMap: GoogleMap) {
        map = googleMap
        map.uiSettings.isZoomControlsEnabled = true
        map.setOnMarkerDragListener(this)
        map.setOnMarkerClickListener(this)
        if (Util.checkLocationPermissions(requireContext())) {
            loadSavedPolygon()
        }
    }

    @SuppressLint("MissingPermission")
    fun getCurrentLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val currentLatLng = LatLng(location.latitude, location.longitude)
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, DEFAULT_MAP_ZOOM))
            }
        }
    }

    override fun onLocationPermissionsGranted() {
        getCurrentLocation()
    }

    fun loadSavedPolygon() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                ubicacionDao.getAllPolygonPoints()?.let { drawSavedPolygonMarkers(it) }
            }
        }
    }

    suspend fun drawSavedPolygonMarkers(ubicaciones: List<Ubicacion?>?) {
        withContext(Dispatchers.Main) {
            points.clear()
            ubicaciones?.forEach { ubicacion ->
                if (ubicacion != null) {
                    val latlng = LatLng(ubicacion.latitud, ubicacion.longitud)
                    map.addMarker(
                        MarkerOptions()
                            .position(latlng)
                            .icon(
                                BitmapDescriptorFactory
                                    .defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                            )
                    )?.let { marker ->
                        points.add(PolygonPoint(marker, ubicacion))
                    }
                }
            }
            if (ubicaciones?.isNotEmpty() == true) {
                points[0].marker?.let {
                    CameraUpdateFactory.newLatLngZoom(
                        it.position,
                        DEFAULT_MAP_ZOOM
                    )
                }?.let { map.moveCamera(it) }
                drawPolygon()
            } else if (checkPermissions()) {
                getCurrentLocation()
            } else {
                askForLocationPermissions()
            }

        }
    }

    suspend fun drawPolygon() {
        withContext(Dispatchers.Main) {
            val polygonOptions = PolygonOptions()
            polygon?.remove()
            points.forEach { polygonPoint ->
                polygonOptions.add(
                    LatLng(
                        polygonPoint.ubicacion.latitud,
                        polygonPoint.ubicacion.longitud
                    )
                )
            }
            if (polygonOptions.points.size > 0) {
                points[0].marker?.let {
                    CameraUpdateFactory.newLatLngZoom(
                        it.position,
                        DEFAULT_MAP_ZOOM
                    )
                }?.let { map.moveCamera(it) }
                polygon = map.addPolygon(polygonOptions)
            }
        }
    }

    fun setEditing(condition: Boolean) {
        isEditing = condition
        points.forEach { polygonPoint ->
            polygonPoint.marker?.isDraggable = isEditing
            val icon = if (isEditing) BitmapDescriptorFactory.defaultMarker() else
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
            polygonPoint.marker?.setIcon(icon)
        }

    }

    /** MARKERS FUNCTIONALITY */
    fun addMarker() {
        this.setEditing(false)
        map.addMarker(
            MarkerOptions()
                .draggable(true)
                .position(map.cameraPosition.target)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
        )?.let { marker -> points.add(PolygonPoint(marker)) }
    }

    fun savePoints() {
        this.setEditing(false)
        var pointsToDelete = ArrayList<Int>()
        points.forEachIndexed { index, point ->
            if (point.marker?.isVisible == true) {
                val lastUbicacion = point.ubicacion
                point.ubicacion = point.marker?.position?.let {
                    Ubicacion(
                        id = lastUbicacion.id,
                        latitud = it.latitude,
                        longitud = it.longitude,
                        fecha = lastUbicacion.fecha,
                        isInPoligon = true,
                        isPointPolygon = true
                    )
                }!!
            } else {
                pointsToDelete.add(index)
            }
        }
        pointsToDelete.forEach { position -> points.removeAt(position) }
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                points.forEach { point ->
                    ubicacionDao.upsert(point.ubicacion)
                }
                drawPolygon()
            }
        }
    }

    override fun onMarkerDrag(p0: Marker) {
        //TODO("Not yet implemented")
    }

    override fun onMarkerDragEnd(marker: Marker) {
        points.forEach { polygonPoint ->
            if (polygonPoint.marker?.id == marker.id) {
                polygonPoint.marker?.position = marker.position
            }
        }
    }

    override fun onMarkerDragStart(p0: Marker) {
        //TODO("Not yet implemented")
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        if (isEditing) {
            var pointToDelete: PolygonPoint? = null
            points.forEach { polygonPoint ->
                if (polygonPoint.marker?.id == marker.id) {
                    pointToDelete = polygonPoint
                    if (polygonPoint.ubicacion.id != null) {
                        lifecycleScope.launch {
                            withContext(Dispatchers.IO) {
                                ubicacionDao.delete(polygonPoint.ubicacion)
                            }
                        }
                    }
                    polygonPoint.marker?.remove()
                }
            }
            if (pointToDelete != null) {
                points.remove(pointToDelete)
            }
            lifecycleScope.launch { drawPolygon() }
        }
        return true
    }

    /** END MARKERS FUNCTIONALITY */

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}