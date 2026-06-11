package ru.lesnoy40rt77.weatheradvisor.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
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
    private val fusedLocationClient =
        LocationServices.getFusedLocationProviderClient(context.applicationContext)

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location {
        val freshLocation = withTimeoutOrNull(10_000) {
            getFreshLocation()
        }

        if (freshLocation != null) {
            return freshLocation
        }

        val lastKnownLocation = withTimeoutOrNull(5_000) {
            getLastKnownLocation()
        }

        if (lastKnownLocation != null) {
            return lastKnownLocation
        }

        throw IllegalStateException(
            "Не удалось определить местоположение. Проверь, что геолокация включена, а в эмуляторе задана Location."
        )
    }

    @SuppressLint("MissingPermission")
    private suspend fun getFreshLocation(): Location? {
        return suspendCancellableCoroutine { continuation ->
            val cancellationTokenSource = CancellationTokenSource()

            fusedLocationClient
                .getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
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

            continuation.invokeOnCancellation {
                cancellationTokenSource.cancel()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getLastKnownLocation(): Location? {
        return suspendCancellableCoroutine { continuation ->
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
        }
    }
}