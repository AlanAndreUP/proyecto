package com.actividad1.rutasegura.data.repository


import android.annotation.SuppressLint
import android.os.Looper
import com.google.android.gms.location.*
import com.actividad1.rutasegura.data.model.UserLocation
import com.actividad1.rutasegura.data.sensors.LocationProviderWrapper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import javax.inject.Inject

// Interfaz
interface LocationRepository {
    fun getUserLocationUpdates(): Flow<UserLocation>
}

// Implementación
class LocationRepositoryImpl @Inject constructor(
    private val locationWrapper: LocationProviderWrapper,
    private val ioDispatcher: CoroutineDispatcher // Inyecta el dispatcher IO
) : LocationRepository {

    @SuppressLint("MissingPermission") // Asume que los permisos se manejan en la UI
    override fun getUserLocationUpdates(): Flow<UserLocation> = callbackFlow<UserLocation> {
        // Comprobar permisos aquí sería ideal antes de empezar
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L) // cada 5 seg
            .setMinUpdateIntervalMillis(2000L) // Mínimo 2 seg
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let {
                    trySend(UserLocation(it.latitude, it.longitude)).isSuccess // Emite la ubicación
                }
            }
        }

        // Asegúrate de estar en un contexto que pueda llamar a requestLocationUpdates
        // Por ejemplo, dentro de un launch(Dispatchers.Main) si es necesario,
        // aunque el callback usualmente se maneja en el Main Looper por defecto.
        locationWrapper.fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper() // Callback en el hilo principal
        ).addOnFailureListener { e ->
            close(e) // Cierra el flow con error si falla el request
        }

        // Se llama cuando el Flow se cancela/cierra
        awaitClose {
            locationWrapper.fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }.flowOn(ioDispatcher) // Ejecuta la lógica de subscripción en IO
}