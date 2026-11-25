package com.example.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class DeviceLocationFix(
    val lat: Double,
    val lng: Double,
    val bearing: Double? = null
)

object LiveLocationTracker {

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    fun observeDeviceLocation(
        context: Context,
        intervalMs: Long = 5_000L
    ): Flow<DeviceLocationFix> = callbackFlow {
        if (!hasLocationPermission(context)) {
            close()
            return@callbackFlow
        }

        val client = LocationServices.getFusedLocationProviderClient(context.applicationContext)

        fun emitLocation(loc: Location?) {
            if (loc != null && isValidCoordinate(loc.latitude, loc.longitude)) {
                val bearing = if (loc.hasBearing()) {
                    loc.bearing.toDouble().coerceIn(0.0, 360.0)
                } else {
                    null
                }
                trySend(
                    DeviceLocationFix(
                        lat = loc.latitude,
                        lng = loc.longitude,
                        bearing = bearing
                    )
                )
            }
        }

        try {
            client.lastLocation.addOnSuccessListener { lastLoc ->
                emitLocation(lastLoc)
            }
        } catch (_: SecurityException) {
            close()
            return@callbackFlow
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(3_000L)
            .setMinUpdateDistanceMeters(5f)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                emitLocation(result.lastLocation)
            }
        }

        try {
            client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        } catch (_: SecurityException) {
            close()
            return@callbackFlow
        }

        awaitClose {
            client.removeLocationUpdates(callback)
        }
    }
}
