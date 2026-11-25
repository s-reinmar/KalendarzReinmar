package com.example.ui

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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.ui.theme.AppIcons
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

sealed interface DeviceLocationResult {
    data class Success(val lat: Double, val lng: Double) : DeviceLocationResult
    data object PermissionDenied : DeviceLocationResult
    data object LocationDisabled : DeviceLocationResult
    data object Unavailable : DeviceLocationResult
}

object DeviceLocationHelper {
    private const val LOCATION_TIMEOUT_MS = 8_000L

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    suspend fun fetchCurrentLocation(context: Context): DeviceLocationResult {
        if (!hasLocationPermission(context)) {
            return DeviceLocationResult.PermissionDenied
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
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
            return if (bestLastKnown != null && isValidLocation(bestLastKnown)) {
                DeviceLocationResult.Success(bestLastKnown.latitude, bestLastKnown.longitude)
            } else {
                DeviceLocationResult.LocationDisabled
            }
        }

        val fresh = withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
            requestSingleFix(context, locationManager, candidateProviders.first())
        }

        val chosen = when {
            fresh != null && isValidLocation(fresh) -> fresh
            bestLastKnown != null && isValidLocation(bestLastKnown) -> bestLastKnown
            else -> null
        }

        return if (chosen != null) {
            DeviceLocationResult.Success(chosen.latitude, chosen.longitude)
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
            if (!isValidLocation(loc)) continue
            if (best == null || loc.time > best.time) {
                best = loc
            }
        }
        return best
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestSingleFix(
        context: Context,
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
                    ContextCompat.getMainExecutor(context),
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

    private fun isValidLocation(location: Location): Boolean {
        val lat = location.latitude
        val lng = location.longitude
        return lat.isFinite() && lng.isFinite() &&
            lat in -90.0..90.0 && lng in -180.0..180.0
    }
}

@Composable
fun MyLocationMapButton(
    onLocationFound: (lat: Double, lng: Double) -> Unit,
    onFeedbackMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "map_my_location_button",
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLocating by remember { mutableStateOf(false) }

    val deniedMsg = stringResource(R.string.map_my_location_permission_denied)
    val disabledMsg = stringResource(R.string.map_my_location_disabled)
    val unavailableMsg = stringResource(R.string.map_my_location_unavailable)

    fun triggerLocate() {
        if (isLocating) return
        isLocating = true
        scope.launch {
            val result = DeviceLocationHelper.fetchCurrentLocation(context)
            isLocating = false
            when (result) {
                is DeviceLocationResult.Success -> onLocationFound(result.lat, result.lng)
                DeviceLocationResult.PermissionDenied -> onFeedbackMessage(deniedMsg)
                DeviceLocationResult.LocationDisabled -> onFeedbackMessage(disabledMsg)
                DeviceLocationResult.Unavailable -> onFeedbackMessage(unavailableMsg)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            triggerLocate()
        } else {
            onFeedbackMessage(deniedMsg)
        }
    }

    Surface(
        onClick = {
            if (DeviceLocationHelper.hasLocationPermission(context)) {
                triggerLocate()
            } else {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    )
                )
            }
        },
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        contentColor = MaterialTheme.colorScheme.primary,
        tonalElevation = 4.dp,
        shadowElevation = 4.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(48.dp)
            .testTag(testTag),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isLocating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                Icon(
                    painter = painterResource(AppIcons.myLocation),
                    contentDescription = stringResource(R.string.cd_my_location),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}
