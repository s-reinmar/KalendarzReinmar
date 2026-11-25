package com.example.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Looper
import androidx.core.content.ContextCompat
import com.example.domain.model.DeviceLocationResult
import com.example.domain.model.GeoPoint
import com.example.domain.port.DeviceLocationProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

class AndroidLocationProvider(
    private val appContext: Context,
) : DeviceLocationProvider {

    override fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    override suspend fun fetchCurrentLocation(): DeviceLocationResult {
        if (!hasLocationPermission()) {
            return DeviceLocationResult.PermissionDenied
        }

        val locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return DeviceLocationResult.Unavailable

        val candidateProviders = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                locationManager.allProviders.contains(LocationManager.FUSED_PROVIDER)
            ) {
                add(LocationManager.FUSED_PROVIDER)
            }
            add(LocationManager.GPS_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
        }.filter { provider ->
            runCatching { locationManager.isProviderEnabled(provider) }.getOrDefault(false)
        }

        val bestLastKnown = findBestLastKnownLocation(locationManager)

        if (candidateProviders.isEmpty()) {
            val fallbackPoint = bestLastKnown?.toGeoPoint()
            return if (fallbackPoint != null) {
                DeviceLocationResult.Success(fallbackPoint)
            } else {
                DeviceLocationResult.LocationDisabled
            }
        }

        val fresh = withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
            requestSingleFix(locationManager, candidateProviders.first())
        }

        val chosenPoint = fresh?.toGeoPoint() ?: bestLastKnown?.toGeoPoint()
        return if (chosenPoint != null) {
            DeviceLocationResult.Success(chosenPoint)
        } else {
            DeviceLocationResult.Unavailable
        }
    }

    @SuppressLint("MissingPermission")
    private fun findBestLastKnownLocation(locationManager: LocationManager): Location? {
        val providers = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(LocationManager.FUSED_PROVIDER)
            }
            add(LocationManager.GPS_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
            add(LocationManager.PASSIVE_PROVIDER)
        }
        var best: Location? = null
        for (provider in providers) {
            val loc = runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
                ?: continue
            if (loc.toGeoPoint() == null) continue
            if (best == null || loc.time > best.time) {
                best = loc
            }
        }
        return best
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestSingleFix(
        locationManager: LocationManager,
        provider: String,
    ): Location? = suspendCancellableCoroutine { cont ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val signal = CancellationSignal()
            cont.invokeOnCancellation { signal.cancel() }
            try {
                locationManager.getCurrentLocation(
                    provider,
                    signal,
                    ContextCompat.getMainExecutor(appContext),
                ) { location ->
                    if (cont.isActive) {
                        cont.resume(location)
                    }
                }
            } catch (_: Exception) {
                if (cont.isActive) {
                    cont.resume(null)
                }
            }
        } else {
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    locationManager.removeUpdates(this)
                    if (cont.isActive) {
                        cont.resume(location)
                    }
                }

                @Deprecated("Deprecated in Android API")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
                override fun onProviderEnabled(provider: String) = Unit
                override fun onProviderDisabled(provider: String) = Unit
            }
            cont.invokeOnCancellation {
                runCatching { locationManager.removeUpdates(listener) }
            }
            try {
                @Suppress("DEPRECATION")
                locationManager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
            } catch (_: Exception) {
                runCatching { locationManager.removeUpdates(listener) }
                if (cont.isActive) {
                    cont.resume(null)
                }
            }
        }
    }

    private fun Location.toGeoPoint(): GeoPoint? = GeoPoint.createOrNull(latitude, longitude)

    private companion object {
        const val LOCATION_TIMEOUT_MS = 8_000L
    }
}
