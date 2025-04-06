package com.actividad1.rutasegura.data.simulation // O el paquete que prefieras

import com.actividad1.rutasegura.data.model.SimulatedBusState
import com.actividad1.rutasegura.data.model.UserLocation // Asegúrate que exista
import com.actividad1.rutasegura.data.repository.RouteRepository // Dependencia necesaria
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.random.Random

// Placeholder MUY BÁSICO de SimulationEngine
class SimulationEngine(
    private val routeRepository: RouteRepository, // Necesita acceso a las rutas
    private val defaultDispatcher: CoroutineDispatcher // Para lanzar corutinas
) {
    private val _busStates = MutableStateFlow<List<SimulatedBusState>>(emptyList())
    val busStates: StateFlow<List<SimulatedBusState>> = _busStates.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private var simulationJob: Job? = null
    private val simulationScope = CoroutineScope(SupervisorJob() + defaultDispatcher)

    suspend fun start(routeId: String) {
        stop() // Detiene simulación anterior si existe
        val route = routeRepository.getRouteById(routeId) ?: return // Obtiene la ruta

        _isRunning.value = true
        simulationJob = simulationScope.launch {
            if (route.points.isEmpty()) {
                _isRunning.value = false
                return@launch
            }
            var currentIndex = 0
            val busIdSim = "SIM-BUS-${Random.nextInt(100, 999)}"
            while (isActive && _isRunning.value) { // Verifica isActive y la bandera
                val currentPoint = route.points[currentIndex % route.points.size]
                val nextIndex = (currentIndex + 1) % route.points.size
                _busStates.value = listOf(
                    SimulatedBusState( // Asegúrate que exista esta data class
                        busId = busIdSim,
                        currentLocation = currentPoint,
                        nextStopIndex = nextIndex, // O la lógica que necesites
                        status = "En Ruta (Simulado)",
                        routeId = routeId,
                        speed = "5"
                    )
                )
                delay(3000) // Mueve el bus cada 3 segundos (ejemplo)
                currentIndex++
            }
        }
    }

    fun pause() {
        // Pausa simple deteniendo el job, podría necesitar más lógica
        _isRunning.value = false
        simulationJob?.cancel()
        simulationJob = null
        // Decide si quieres mantener el último estado en _busStates o limpiarlo
    }

    suspend fun stop() {
        _isRunning.value = false
        simulationJob?.cancel()
        simulationJob = null
        _busStates.value = emptyList() // Limpia los buses al detener
    }

    // Asegúrate de tener la data class necesaria
    // data class SimulatedBusState(
    //     val busId: String,
    //     val currentLocation: UserLocation,
    //     val nextStopIndex: Int,
    //     val status: String,
    //     val routeId: String
    // )
}