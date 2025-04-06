package com.actividad1.rutasegura.data.repository

import android.annotation.SuppressLint
import android.os.Looper
import com.google.android.gms.location.*
import com.actividad1.rutasegura.data.model.UserLocation
import kotlinx.coroutines.CoroutineDispatcher // Necesario para el constructor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
// Quita import javax.inject.Inject

// Interfaz (sin cambios)
interface LocationRepository {
    fun getUserLocationUpdates(): Flow<UserLocation>
}

// Implementación SIN HILT
class LocationRepositoryImpl(
    private val locationWrapper: com.actividad1.rutasegura.ui.theme.components.LocationProviderWrapper, // Recibe vía constructor
    private val ioDispatcher: CoroutineDispatcher        // Recibe vía constructor
) : LocationRepository {

    @SuppressLint("MissingPermission") // Asume que los permisos se manejan en la UI
    override fun getUserLocationUpdates(): Flow<UserLocation> = callbackFlow<UserLocation> {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
            .setMinUpdateIntervalMillis(2000L)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let {
                    trySend(UserLocation(it.latitude, it.longitude)).isSuccess
                }
            }
        }

        locationWrapper.fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        ).addOnFailureListener { e ->
            close(e)
        }

        awaitClose {
            locationWrapper.fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }.flowOn(ioDispatcher) // Usa el dispatcher inyectado
}