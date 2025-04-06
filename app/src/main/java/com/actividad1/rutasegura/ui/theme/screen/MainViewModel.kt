package com.actividad1.rutasegura.ui.theme.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.actividad1.rutasegura.data.model.*
import com.actividad1.rutasegura.data.repository.CameraRepository
import com.actividad1.rutasegura.data.repository.LocationRepository
import com.actividad1.rutasegura.data.repository.RouteRepository
import com.actividad1.rutasegura.data.repository.SimulationRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.*

class MainViewModel(
    private val locationRepository: LocationRepository,
    private val routeRepository: RouteRepository,
    private val simulationRepository: SimulationRepository,
    private val cameraRepository: CameraRepository
) : ViewModel() {

    private val _userLocation = MutableStateFlow<UserLocation?>(null)
    val userLocation: StateFlow<UserLocation?> = _userLocation.asStateFlow()

    private val _availableRoutes = MutableStateFlow<List<Route>>(emptyList())
    val availableRoutes: StateFlow<List<Route>> = _availableRoutes.asStateFlow()

    val simulatedBuses: StateFlow<List<SimulatedBusState>> = simulationRepository.getBusStateUpdates()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val isSimulationRunning: StateFlow<Boolean> = simulationRepository.isSimulationRunning()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), false)

    val nearestBusEta: StateFlow<String?> = combine(userLocation, simulatedBuses) { userLoc, buses ->
        calculateNearestBusEta(userLoc, buses)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    private val _scanResult = MutableStateFlow<ScanResult?>(null)
    val scanResult: StateFlow<ScanResult?> = _scanResult.asStateFlow()

    private val _isLoadingRoutes = MutableStateFlow(false)
    val isLoadingRoutes: StateFlow<Boolean> = _isLoadingRoutes.asStateFlow()

    private val _isLoadingScan = MutableStateFlow(false)
    val isLoadingScan: StateFlow<Boolean> = _isLoadingScan.asStateFlow()

    init {
        startLocationUpdates()
        loadRoutes()
    }

    private fun startLocationUpdates() {
        viewModelScope.launch {
            locationRepository.getUserLocationUpdates()
                .catch { e ->
                    System.err.println("Error obteniendo ubicación: ${e.message}")
                }
                .collect { location ->
                    _userLocation.value = location
                    println("Ubicación Recibida: $location")
                }
        }
    }

    private fun loadRoutes() {
        viewModelScope.launch {
            _isLoadingRoutes.value = true
            try {
                _availableRoutes.value = routeRepository.getAvailableRoutes()
            } catch (e: Exception) {
                System.err.println("Error cargando rutas: ${e.message}")
                _availableRoutes.value = emptyList()
            } finally {
                _isLoadingRoutes.value = false
            }
        }
    }

    fun startSimulation(routeId: String) {
        viewModelScope.launch {
            simulationRepository.startSimulation(routeId)
        }
    }

    fun pauseSimulation() {
        viewModelScope.launch {
            simulationRepository.pauseSimulation()
        }
    }

    fun stopSimulation() {
        viewModelScope.launch {
            simulationRepository.stopSimulation()
        }
    }

    fun performScan() {
        _scanResult.value = null // Limpiar resultado anterior antes de escanear
        println("ViewModel: Solicitud de escaneo iniciada desde UI.")
    }

    fun updateScanResult(result: ScanResult) {
        _scanResult.value = result
        _isLoadingScan.value = false
        println("ViewModel: Resultado de escaneo recibido: $result")

    }


    fun clearScanResult() {
        _scanResult.value = null
        _isLoadingScan.value = false // Asegurarse que no se quede cargando
    }
    private fun calculateNearestBusEta(userLoc: UserLocation?, buses: List<SimulatedBusState>): String? {
        if (userLoc == null || buses.isEmpty()) return null

        var minDistance = Double.MAX_VALUE
        var nearestBus: SimulatedBusState? = null

        for (bus in buses) {
            val distance = haversineDistance(userLoc, bus.currentLocation)
            if (distance < minDistance) {
                minDistance = distance
                nearestBus = bus
            }
        }

        if (nearestBus == null) return null

        if (minDistance < 0.05) return "Llegando"

        if (nearestBus.speed.toInt() < 5) {
            return "Detenido cerca (${"%.1f".format(minDistance * 1000)} m)"
        }

        val etaHours = minDistance / nearestBus.speed.toInt()
        val etaMinutes = (etaHours * 60).roundToInt()

        return when {
            etaMinutes <= 0 -> "Menos de 1 min"
            etaMinutes < 60 -> "$etaMinutes min"
            else -> {
                val hours = etaMinutes / 60
                val minutes = etaMinutes % 60
                "${hours}h ${minutes}m"
            }
        }
    }

    private fun haversineDistance(loc1: UserLocation, loc2: UserLocation): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(loc2.latitude - loc1.latitude)
        val dLon = Math.toRadians(loc2.longitude - loc1.longitude)
        val lat1Rad = Math.toRadians(loc1.latitude)
        val lat2Rad = Math.toRadians(loc2.latitude)

        val a = sin(dLat / 2).pow(2) + sin(dLon / 2).pow(2) * cos(lat1Rad) * cos(lat2Rad)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusKm * c
    }
}
