package ru.lesnoy40rt77.weatheradvisor.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class LocationRepository(
    context: Context
) {
    private val appContext = context.applicationContext

    private val fusedLocationClient =
        LocationServices.getFusedLocationProviderClient(appContext)

    private val locationManager =
        appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    suspend fun getCurrentLocation(): Location {
        if (!hasLocationPermission()) {
            throw IllegalStateException("Нет разрешения на местоположение.")
        }

        val fusedFreshLocation = withTimeoutOrNull(8_000) {
            getFreshFusedLocation()
        }

        if (fusedFreshLocation != null) {
            return fusedFreshLocation
        }

        val fusedLastLocation = withTimeoutOrNull(3_000) {
            getLastFusedLocation()
        }

        if (fusedLastLocation != null) {
            return fusedLastLocation
        }

        val platformLastLocation = getBestPlatformLastKnownLocation()

        if (platformLastLocation != null) {
            return platformLastLocation
        }

        val platformFreshLocation = withTimeoutOrNull(8_000) {
            getSinglePlatformLocation()
        }

        if (platformFreshLocation != null) {
            return platformFreshLocation
        }

        throw IllegalStateException(
            "Не удалось определить местоположение. В эмуляторе открой Extended Controls → Location, задай координаты и нажми Set Location."
        )
    }

    private fun hasLocationPermission(): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineGranted || coarseGranted
    }

    @SuppressLint("MissingPermission")
    private suspend fun getFreshFusedLocation(): Location? {
        return suspendCancellableCoroutine { continuation ->
            val cancellationTokenSource = CancellationTokenSource()

            try {
                fusedLocationClient
                    .getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        cancellationTokenSource.token
                    )
                    .addOnSuccessListener { location ->
                        if (continuation.isActive) {
                            continuation.resume(location)
                        }
                    }
                    .addOnFailureListener { exception ->
                        if (continuation.isActive) {
                            continuation.resumeWithException(exception)
                        }
                    }
            } catch (exception: SecurityException) {
                if (continuation.isActive) {
                    continuation.resumeWithException(exception)
                }
            }

            continuation.invokeOnCancellation {
                cancellationTokenSource.cancel()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getLastFusedLocation(): Location? {
        return suspendCancellableCoroutine { continuation ->
            try {
                fusedLocationClient
                    .lastLocation
                    .addOnSuccessListener { location ->
                        if (continuation.isActive) {
                            continuation.resume(location)
                        }
                    }
                    .addOnFailureListener { exception ->
                        if (continuation.isActive) {
                            continuation.resumeWithException(exception)
                        }
                    }
            } catch (exception: SecurityException) {
                if (continuation.isActive) {
                    continuation.resumeWithException(exception)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getBestPlatformLastKnownLocation(): Location? {
        return try {
            locationManager
                .getProviders(true)
                .mapNotNull { provider ->
                    try {
                        locationManager.getLastKnownLocation(provider)
                    } catch (_: SecurityException) {
                        null
                    }
                }
                .maxByOrNull { it.time }
        } catch (_: Exception) {
            null
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getSinglePlatformLocation(): Location? {
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        ).filter { provider ->
            try {
                locationManager.isProviderEnabled(provider)
            } catch (_: Exception) {
                false
            }
        }

        for (provider in providers) {
            val location = withTimeoutOrNull(5_000) {
                requestSingleUpdate(provider)
            }

            if (location != null) {
                return location
            }
        }

        return null
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestSingleUpdate(provider: String): Location? {
        return suspendCancellableCoroutine { continuation ->
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    locationManager.removeUpdates(this)

                    if (continuation.isActive) {
                        continuation.resume(location)
                    }
                }
            }

            try {
                locationManager.requestSingleUpdate(
                    provider,
                    listener,
                    Looper.getMainLooper()
                )
            } catch (_: Exception) {
                locationManager.removeUpdates(listener)

                if (continuation.isActive) {
                    continuation.resume(null)
                }
            }

            continuation.invokeOnCancellation {
                locationManager.removeUpdates(listener)
            }
        }
    }
}