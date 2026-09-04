package com.tommasoberlose.anotherwidget.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class LocationRepository(context: Context) {
    private val appContext = context.applicationContext
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(appContext)
    private val locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    suspend fun getCurrentLocation(): Location? {
        check(hasLocationPermission()) { "Location permission is not granted" }

        val cachedLocation = getCachedLocation()
        if (cachedLocation != null && isRecent(cachedLocation)) {
            return cachedLocation
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemCurrentLocation()?.let { return it }
        }

        getFusedCurrentLocation()?.let { return it }

        // A valid cached fix is preferable to showing a false failure when the
        // vendor location provider cannot produce a new fix immediately.
        cachedLocation?.let { return it }

        return getFusedLocationUpdate() ?: getCachedLocation()
    }

    private suspend fun getSystemCurrentLocation(): Location? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null

        return withTimeoutOrNull(LIVE_LOCATION_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val cancellation = CancellationSignal()
                try {
                    locationManager?.getCurrentLocation(
                        LocationManager.FUSED_PROVIDER,
                        cancellation,
                        ContextCompat.getMainExecutor(appContext)
                    ) { location ->
                        if (continuation.isActive) continuation.resume(location)
                    } ?: continuation.resume(null)
                } catch (ignored: Exception) {
                    if (continuation.isActive) continuation.resume(null)
                }
                continuation.invokeOnCancellation { cancellation.cancel() }
            }
        }
    }

    private suspend fun getFusedCurrentLocation(): Location? {
        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
            .setDurationMillis(LIVE_LOCATION_TIMEOUT_MS)
            .setMaxUpdateAgeMillis(MAX_CACHED_LOCATION_AGE_MS)
            .build()
        val cancellation = CancellationTokenSource()

        return try {
            suspendCancellableCoroutine { continuation ->
                fusedLocationClient.getCurrentLocation(request, cancellation.token)
                    .addOnSuccessListener { location ->
                        if (continuation.isActive) continuation.resume(location)
                    }
                    .addOnFailureListener { exception ->
                        if (continuation.isActive) continuation.resumeWithException(exception)
                    }
                continuation.invokeOnCancellation { cancellation.cancel() }
            }
        } catch (ignored: Exception) {
            null
        }
    }

    private suspend fun getFusedLocationUpdate(): Location? {
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            LOCATION_UPDATE_INTERVAL_MS
        )
            .setMinUpdateIntervalMillis(MIN_LOCATION_UPDATE_INTERVAL_MS)
            .setMaxUpdates(1)
            .setDurationMillis(LIVE_LOCATION_TIMEOUT_MS)
            .setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
            .build()

        return withTimeoutOrNull(LIVE_LOCATION_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val callback = object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        val location = result.lastLocation ?: return
                        if (continuation.isActive) {
                            fusedLocationClient.removeLocationUpdates(this)
                            continuation.resume(location)
                        }
                    }
                }

                fusedLocationClient.requestLocationUpdates(
                    request,
                    callback,
                    Looper.getMainLooper()
                ).addOnFailureListener {
                    if (continuation.isActive) continuation.resume(null)
                }

                continuation.invokeOnCancellation {
                    fusedLocationClient.removeLocationUpdates(callback)
                }
            }
        }
    }

    private suspend fun getCachedLocation(): Location? {
        val systemLocation = getSystemLastKnownLocation()
        val fusedLocation: Location? = try {
            suspendCancellableCoroutine<Location?> { continuation ->
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location ->
                        if (continuation.isActive) continuation.resume(location)
                    }
                    .addOnFailureListener {
                        if (continuation.isActive) continuation.resume(null)
                    }
            }
        } catch (ignored: Exception) {
            null
        }

        return listOfNotNull(systemLocation, fusedLocation).maxByOrNull { it.time }
    }

    private fun getSystemLastKnownLocation(): Location? {
        val providers = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            providers.add(LocationManager.FUSED_PROVIDER)
        }
        providers.add(LocationManager.NETWORK_PROVIDER)
        providers.add(LocationManager.GPS_PROVIDER)

        return providers.mapNotNull { provider ->
            try {
                locationManager?.getLastKnownLocation(provider)
            } catch (ignored: SecurityException) {
                null
            } catch (ignored: IllegalArgumentException) {
                null
            }
        }.maxByOrNull { it.time }
    }

    private fun isRecent(location: Location): Boolean {
        return System.currentTimeMillis() - location.time <= MAX_CACHED_LOCATION_AGE_MS
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val LIVE_LOCATION_TIMEOUT_MS = 12_000L
        private const val MAX_CACHED_LOCATION_AGE_MS = 30 * 60 * 1000L
        private const val LOCATION_UPDATE_INTERVAL_MS = 1_000L
        private const val MIN_LOCATION_UPDATE_INTERVAL_MS = 500L
    }
}
