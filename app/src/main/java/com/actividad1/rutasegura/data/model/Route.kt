package com.actividad1.rutasegura.data.model

data class Route(
    val id: String,
    val name: String,
    val path: List<UserLocation>
)