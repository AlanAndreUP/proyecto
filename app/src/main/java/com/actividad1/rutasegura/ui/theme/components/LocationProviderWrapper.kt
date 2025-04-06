package com.actividad1.rutasegura.ui.theme.components


import android.content.Context
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

// Wrapper simple para obtener el FusedLocationProviderClient
class LocationProviderWrapper(context: Context) {
    val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
}