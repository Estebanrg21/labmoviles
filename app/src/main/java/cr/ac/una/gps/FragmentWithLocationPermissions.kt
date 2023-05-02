package cr.ac.una.gps

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

abstract class FragmentWithLocationPermissions : Fragment(){

    protected companion object {
        val LOCATION_PERMISSIONS_REQUEST_CODE: Int = 568466
    }

    protected fun checkPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun askForLocationPermissions() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            LOCATION_PERMISSIONS_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == LOCATION_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (checkPermissions()) {
                    onLocationPermissionsGranted()
                } else {
                    onLocationPermissionsNotGranted()
                }
            } else {
                // Permiso denegado, maneja la situación de acuerdo a tus necesidades
                onLocationPermissionsNotGranted()
            }
        }
    }

    protected open fun onLocationPermissionsGranted() {

    }

    protected open fun onLocationPermissionsNotGranted() {

    }
}