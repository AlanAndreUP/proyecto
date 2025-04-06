package com.actividad1.rutasegura.data.repository


import com.actividad1.rutasegura.data.model.SimulatedBusState
import com.actividad1.rutasegura.data.simulation.SimulationEngine
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

// Interfaz
interface SimulationRepository {
    suspend fun startSimulation(routeId: String)
    suspend fun pauseSimulation()
    suspend fun stopSimulation()
    fun getBusStateUpdates(): Flow<List<SimulatedBusState>>
    fun isSimulationRunning(): Flow<Boolean>
}

// Implementación
@Singleton // El repositorio puede ser Singleton si el engine lo es
class SimulationRepositoryImpl @Inject constructor(
    private val engine: SimulationEngine // Inyecta el engine
) : SimulationRepository {
    override suspend fun startSimulation(routeId: String) = engine.start(routeId)
    override suspend fun pauseSimulation() = engine.pause()
    override suspend fun stopSimulation() = engine.stop()
    override fun getBusStateUpdates(): Flow<List<SimulatedBusState>> = engine.busStates
    override fun isSimulationRunning(): Flow<Boolean> = engine.isRunning
}