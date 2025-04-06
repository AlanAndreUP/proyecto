package com.actividad1.rutasegura.data.repository

import com.actividad1.rutasegura.data.model.UserLocation // Asegúrate que la ruta sea correcta
import com.actividad1.rutasegura.data.model.Route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

// Interfaz
interface RouteRepository {
    suspend fun getAvailableRoutes(): List<Route>
    suspend fun getRouteById(id: String): Route?
}

// Implementación
@Singleton // Esta implementación puede ser Singleton si las rutas no cambian
class RouteRepositoryImpl @Inject constructor() : RouteRepository {
    // Rutas hardcoded para el ejemplo
    private val routes = listOf(
        Route("R1", "Ruta Centro", listOf(
            UserLocation(16.7531, -93.1156), // Tuxtla Centro
            UserLocation(16.7580, -93.1180),
            UserLocation(16.7600, -93.1200), // Punto intermedio
            UserLocation(16.7650, -93.1250)  // Destino ejemplo
        )),
        Route("R2", "Ruta Poniente", listOf(
            UserLocation(16.7531, -93.1156), // Tuxtla Centro
            UserLocation(16.7500, -93.1250),
            UserLocation(16.7480, -93.1350), // Punto intermedio Poniente
            UserLocation(16.7450, -93.1450)  // Destino ejemplo Poniente
        ))
        // Añadir más rutas...
    )

    override suspend fun getAvailableRoutes(): List<Route> = withContext(Dispatchers.IO) {
        delay(50) // Simula carga
        routes
    }

    override suspend fun getRouteById(id: String): Route? = withContext(Dispatchers.IO) {
        delay(20) // Simula carga
        routes.find { it.id == id }
    }
}