package com.actividad1.rutasegura.data.model

data class SimulatedBusState(
    val busId: String,
    val currentLocation: UserLocation,
    val nextStopIndex: Int,
    val status: String,
    val routeId: String,
    val speed: String
)