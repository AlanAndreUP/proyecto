package com.actividad1.rutasegura.ui.theme.screen



import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.actividad1.rutasegura.data.model.*
import com.actividad1.rutasegura.data.repository.CameraRepository
import com.actividad1.rutasegura.data.repository.LocationRepository
import com.actividad1.rutasegura.data.repository.RouteRepository
import com.actividad1.rutasegura.data.repository.SimulationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.*

@HiltViewModel
class MainViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val routeRepository: RouteRepository,
    private val simulationRepository: SimulationRepository,
    private val cameraRepository: CameraRepository
) : ViewModel() {

    private val _userLocation = MutableStateFlow<UserLocation?>(null)
    val userLocation: StateFlow<UserLocation?> = _userLocation.asStateFlow()

    private val _availableRoutes = MutableStateFlow<List<Route>>(emptyList())
    val availableRoutes: StateFlow<List<Route>> = _availableRoutes.asStateFlow()

    // Usa stateIn para convertir el Flow del repo en un StateFlow manejado por el ViewModelScope
    val simulatedBuses: StateFlow<List<SimulatedBusState>> = simulationRepository.getBusStateUpdates()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L), // Comparte mientras haya subscriptores (con 5s de gracia)
            initialValue = emptyList()
        )

    val isSimulationRunning: StateFlow<Boolean> = simulationRepository.isSimulationRunning()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), false)

    // Combina ubicación de usuario y buses para calcular ETA
    val nearestBusEta: StateFlow<String?> = combine(userLocation, simulatedBuses) { userLoc, buses ->
        calculateNearestBusEta(userLoc, buses) // Llama a la función de cálculo
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null) // Inicia como null


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
                // Es importante manejar errores en la recolección del flujo
                .catch { e ->
                    System.err.println("Error obteniendo ubicación: ${e.message}")
                    // Podrías emitir un estado de error aquí si la UI necesita saberlo

                }
                .collect { location ->
                    _userLocation.value = location
                    System.out.println("Ubicación Recibida: $location") // Log para depurar
                }
        }
    }


    private fun loadRoutes() {
        viewModelScope.launch {
            _isLoadingRoutes.value = true
            try {
                // Obtiene las rutas del repositorio
                _availableRoutes.value = routeRepository.getAvailableRoutes()
            } catch (e: Exception) {
                System.err.println("Error cargando rutas: ${e.message}")
                _availableRoutes.value = emptyList() // Estado vacío en caso de error
                // Podrías exponer un estado de error a la UI
            } finally {
                _isLoadingRoutes.value = false
            }
        }
    }

    fun startSimulation(routeId: String) {
        viewModelScope.launch {
            // Podrías añadir un try-catch si startSimulation puede fallar
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
        // Si ya está escaneando, no hacer nada
        if (_isLoadingScan.value) return

        viewModelScope.launch {
            _isLoadingScan.value = true
            _scanResult.value = null // Limpia resultado anterior al iniciar nuevo escaneo
            try {
                _scanResult.value = cameraRepository.scanQRCode()
            } catch (e: Exception) {
                System.err.println("Error durante el escaneo QR: ${e.message}")
                _scanResult.value = ScanResult.Error // Establece estado de error
            } finally {
                _isLoadingScan.value = false // Asegura que el loading se quite
            }
        }
    }

    // Permite a la UI limpiar el resultado del escaneo (ej. al cerrar el diálogo)
    fun clearScanResult() {
        _scanResult.value = null
    }


    // --- Lógica de Cálculo ETA (Simplificada) ---
    private fun calculateNearestBusEta(userLoc: UserLocation?, buses: List<SimulatedBusState>): String? {
        // Si no tenemos ubicación del usuario o no hay buses, no podemos calcular
        if (userLoc == null || buses.isEmpty()) return null

        var minDistance = Double.MAX_VALUE
        var nearestBus: SimulatedBusState? = null

        // Encuentra el bus simulado más cercano
        for (bus in buses) {
            val distance = haversineDistance(userLoc, bus.location)
            if (distance < minDistance) {
                minDistance = distance
                nearestBus = bus
            }
        }

        // Si no se encontró un bus (no debería pasar si la lista no está vacía, pero por si acaso)
        if (nearestBus == null) return null

        // Si el bus está muy cerca (ej. menos de 50m), considera que está llegando
        if (minDistance < 0.05) return "Llegando"

        // Si el bus está parado o muy lento, indica la distancia
        if (nearestBus.speedKmh < 5) {
            return "Detenido cerca (${"%.1f".format(minDistance * 1000)} m)" // Muestra en metros
        }

        // Calcula ETA simple: tiempo = distancia / velocidad
        // Asegúrate que las unidades sean consistentes (km y km/h -> horas)
        val etaHours = minDistance / nearestBus.speedKmh
        val etaMinutes = (etaHours * 60).roundToInt() // Convierte a minutos enteros

        // Formatea el resultado para mostrarlo
        return when {
            etaMinutes <= 0 -> "Menos de 1 min" // Si es 0 o negativo (por redondeo/precisión)
            etaMinutes < 60 -> "$etaMinutes min" // Menos de una hora
            else -> {
                // Más de una hora
                val hours = etaMinutes / 60
                val minutes = etaMinutes % 60
                "${hours}h ${minutes}m"
            }
        }
    }

    // Función Haversine para calcular distancia entre dos puntos Lat/Lon en km
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