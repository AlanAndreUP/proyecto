package com.actividad1.rutasegura.data.simulation


import com.actividad1.rutasegura.data.model.*
import com.actividad1.rutasegura.data.repository.RouteRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*
import kotlin.random.Random

@Singleton // El motor de simulación puede ser Singleton
class SimulationEngine @Inject constructor(
    private val routeRepository: RouteRepository, // Inyecta el repo de rutas
    private val defaultDispatcher: CoroutineDispatcher // Inyecta dispatcher (desde AppModule)
) {
    private val _busStates = MutableStateFlow<List<SimulatedBusState>>(emptyList())
    val busStates: StateFlow<List<SimulatedBusState>> = _busStates.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private var simulationJob: Job? = null
    private var currentRoute: Route? = null
    private var busProgress: MutableMap<String, Int> = mutableMapOf() // Bus ID -> index in route path

    suspend fun start(routeId: String) {
        if (_isRunning.value && currentRoute?.id == routeId) return // Ya corriendo esta ruta
        stop() // Detiene cualquier simulación anterior
        currentRoute = routeRepository.getRouteById(routeId) ?: return // Sale si la ruta no existe
        _isRunning.value = true
        busProgress.clear()
        // Inicializa un bus en el inicio de la ruta
        val startLocation = currentRoute!!.path.first()
        val initialBus = SimulatedBusState(id = "BUS-001", location = startLocation, speedKmh = 0.0)
        busProgress[initialBus.id] = 0
        _busStates.value = listOf(initialBus)

        simulationJob = CoroutineScope(defaultDispatcher + SupervisorJob()).launch {
            while (isActive && _isRunning.value && currentRoute != null) {
                updateBusPositions()
                delay(SIMULATION_TICK_MS) // Actualiza cada segundo
            }
            // Si sale del bucle porque isRunning es false o no hay ruta
            if (!_isRunning.value) {
                _busStates.value = emptyList() // Limpia si se detuvo explícitamente
            }
        }
    }

    fun pause() {
        // Pausar podría simplemente detener el Job sin limpiar el estado,
        // para poder reanudarlo después. Aquí implementamos una pausa simple
        // que cancela el job, similar a stop() pero sin limpiar _busStates.
        // Una implementación más robusta podría manejar un estado 'Paused'.
        _isRunning.value = false
        simulationJob?.cancel()
    }

    suspend fun stop() {
        _isRunning.value = false
        simulationJob?.cancelAndJoin() // Cancela y espera que termine
        simulationJob = null
        currentRoute = null
        // Asegúrate que la limpieza ocurra después de que el job termine
        _busStates.value = emptyList()
        busProgress.clear()

    }

    private fun updateBusPositions() {
        val route = currentRoute ?: return // Sal si no hay ruta seleccionada
        val currentBusStatesList = _busStates.value // Obtiene la lista actual

        // Si no hay buses o la ruta está vacía, no hagas nada
        if (currentBusStatesList.isEmpty() || route.path.isEmpty()) {
            // Podríamos detener la simulación aquí si el bus llega al final y no hay más lógica
            // Opcional: stop()
            return
        }

        val updatedStates = mutableListOf<SimulatedBusState>()

        for (bus in currentBusStatesList) {
            var currentPathIndex = busProgress[bus.id] ?: 0 // Índice actual en la ruta

            // Verifica si ya llegó al final
            if (currentPathIndex >= route.path.size - 1) {
                // El bus llegó al final. Lo dejamos en la última posición con velocidad 0.
                updatedStates.add(bus.copy(
                    speedKmh = 0.0,
                    location = route.path.last(),
                    lastEvent = DrivingEvent.NORMAL // Resetea el evento
                ))
                // Considera detener la simulación o manejar la lógica de fin de ruta aquí
                continue // Procesa el siguiente bus si hubiera más
            }

            // Avanza al siguiente punto de la ruta
            currentPathIndex++
            val nextLocation = route.path[currentPathIndex]
            // val prevLocation = route.path[currentPathIndex - 1] // Podría usarse para calcular ángulo/velocidad

            // Simular velocidad y eventos (muy simplificado)
            val simulatedSpeed = Random.nextDouble(30.0, 50.0)
            // Probabilidad de evento (10% de frenada, 5% de aceleración)
            val randomEvent = Random.nextInt(100)
            val simulatedEvent = when {
                randomEvent < 10 -> DrivingEvent.HARD_BRAKE
                randomEvent < 15 -> DrivingEvent.SUDDEN_ACCELERATION
                else -> DrivingEvent.NORMAL
            }

            val updatedBus = bus.copy(
                location = nextLocation,
                speedKmh = simulatedSpeed,
                lastEvent = simulatedEvent
            )
            updatedStates.add(updatedBus)
            busProgress[bus.id] = currentPathIndex // Actualiza el progreso del bus
        }
        // Emite la nueva lista de estados actualizados
        _busStates.value = updatedStates
    }


    companion object {
        private const val SIMULATION_TICK_MS = 1000L // 1 segundo
    }
}