package cr.ac.una.gps.utils

import android.Manifest
import android.app.Activity
import android.content.Context

import android.content.pm.PackageManager

import androidx.core.content.ContextCompat

class Util{

    companion object {
        fun checkLocationPermissions(context: Context): Boolean {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }

    }
}