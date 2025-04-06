package com.actividad1.rutasegura.ui.theme.screen // O donde la tengas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.actividad1.rutasegura.data.repository.CameraRepository
import com.actividad1.rutasegura.data.repository.LocationRepository
import com.actividad1.rutasegura.data.repository.RouteRepository
import com.actividad1.rutasegura.data.repository.SimulationRepository

// Factory para crear MainViewModel con sus dependencias
class MainViewModelFactory(
    private val locationRepository: LocationRepository,
    private val routeRepository: RouteRepository,
    private val simulationRepository: SimulationRepository,
    private val cameraRepository: CameraRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(
                locationRepository,
                routeRepository,
                simulationRepository,
                cameraRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}