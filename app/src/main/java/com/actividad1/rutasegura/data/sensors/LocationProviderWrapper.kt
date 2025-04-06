package com.actividad1.rutasegura.data.sensors


import com.google.android.gms.location.FusedLocationProviderClient
import javax.inject.Inject
import javax.inject.Singleton

// Wrapper simple para facilitar inyección y testing del FusedLocationProviderClient
@Singleton // El cliente en sí suele ser Singleton
class LocationProviderWrapper @Inject constructor(
    val fusedLocationClient: FusedLocationProviderClient
)