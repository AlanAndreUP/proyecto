package com.actividad1.rutasegura.data.repository

import com.actividad1.rutasegura.data.model.SimulatedBusState
import com.actividad1.rutasegura.data.simulation.SimulationEngine // Necesario para el constructor
import kotlinx.coroutines.flow.Flow
// Quita imports de javax.inject

// Interfaz (sin cambios)
interface SimulationRepository {
    suspend fun startSimulation(routeId: String)
    suspend fun pauseSimulation()
    suspend fun stopSimulation()
    fun getBusStateUpdates(): Flow<List<SimulatedBusState>>
    fun isSimulationRunning(): Flow<Boolean>
}

// Implementación SIN HILT
class SimulationRepositoryImpl(
    private val engine: SimulationEngine // Recibe vía constructor
) : SimulationRepository {
    override suspend fun startSimulation(routeId: String) = engine.start(routeId)
    override suspend fun pauseSimulation() = engine.pause() // Asume que engine tiene estos métodos
    override suspend fun stopSimulation() = engine.stop()   // Asume que engine tiene estos métodos
    override fun getBusStateUpdates(): Flow<List<SimulatedBusState>> = engine.busStates // Asume que engine expone esto
    override fun isSimulationRunning(): Flow<Boolean> = engine.isRunning // Asume que engine expone esto
}