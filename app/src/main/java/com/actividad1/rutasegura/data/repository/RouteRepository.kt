package com.actividad1.rutasegura.data.repository

import com.actividad1.rutasegura.data.model.UserLocation
import com.actividad1.rutasegura.data.model.Route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
// Quita imports de javax.inject

// Interfaz (sin cambios)
interface RouteRepository {
    suspend fun getAvailableRoutes(): List<Route>
    suspend fun getRouteById(id: String): Route?
}

// Implementación SIN HILT
class RouteRepositoryImpl : RouteRepository { // Constructor vacío, sin @Inject

    // Rutas hardcoded (sin cambios)
    private val routes = listOf(
        Route(
            "R1", "Ruta Centro", listOf(
                UserLocation(16.7531, -93.1156), UserLocation(16.7580, -93.1180),
                UserLocation(16.7600, -93.1200), UserLocation(16.7650, -93.1250)
            ),

        ),
        Route(
            "R2", "Ruta Poniente", listOf(
                UserLocation(16.7531, -93.1156), UserLocation(16.7500, -93.1250),
                UserLocation(16.7480, -93.1350), UserLocation(16.7450, -93.1450)
            ),

        )
    )

    override suspend fun getAvailableRoutes(): List<Route> = withContext(Dispatchers.IO) {
        delay(50)
        routes
    }

    override suspend fun getRouteById(id: String): Route? = withContext(Dispatchers.IO) {
        delay(20)
        routes.find { it.id == id }
    }
}