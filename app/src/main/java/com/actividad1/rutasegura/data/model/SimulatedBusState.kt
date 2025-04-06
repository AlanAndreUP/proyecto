package com.actividad1.rutasegura.data.model

data class SimulatedBusState(
    val id: String,
    val location: UserLocation,
    val speedKmh: Double,
    val lastEvent: DrivingEvent? = null
)