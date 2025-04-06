package com.actividad1.rutasegura

import android.Manifest
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.lifecycle.ViewModelProvider
import com.actividad1.rutasegura.ui.theme.screen.MainScreen
import com.actividad1.rutasegura.ui.theme.screen.MainViewModel
import com.actividad1.rutasegura.ui.theme.screen.PermissionRationaleDialog
import com.actividad1.rutasegura.ui.theme.screen.PermissionsDeniedScreen
import com.actividad1.rutasegura.util.PermissionHandler

// --- Importaciones de Repositorios y Factory ---
import com.actividad1.rutasegura.data.repository.*
import com.actividad1.rutasegura.ui.theme.screen.MainViewModelFactory

// --- Importaciones para Instanciación Manual ---
import com.actividad1.rutasegura.data.simulation.SimulationEngine     // Necesario crear esta clase
import kotlinx.coroutines.Dispatchers

class MainActivity : ComponentActivity() {

    private lateinit var permissionCallback: (Map<String, Boolean>) -> Unit

    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissionCallback(permissions)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- Inicio: Instanciación Manual de Dependencias ---

        val appContext: Context = applicationContext

        // 1. Crear dependencias de bajo nivel (si las hay)
        val locationWrapper =
            com.actividad1.rutasegura.ui.theme.components.LocationProviderWrapper(appContext) // Wrapper para FusedLocationProviderClient
        val routeRepo: RouteRepository = RouteRepositoryImpl()    // No tiene dependencias en constructor

        // 2. Crear el SimulationEngine (asumiendo que necesita RouteRepository y un Dispatcher)
        //    Ajusta sus dependencias según tu implementación real
        val simulationEngine = SimulationEngine(routeRepo, Dispatchers.Default)

        // 3. Crear los repositorios que dependen de los anteriores
        val locationRepo: LocationRepository = LocationRepositoryImpl(locationWrapper, Dispatchers.IO)
        val simulationRepo: SimulationRepository = SimulationRepositoryImpl(simulationEngine)
        val cameraRepo: CameraRepository = CameraRepositoryImpl(appContext) // Necesita contexto

        // 4. Crear el ViewModelFactory con las instancias de los repositorios
        val viewModelFactory = MainViewModelFactory(
            locationRepository = locationRepo,
            routeRepository = routeRepo,
            simulationRepository = simulationRepo,
            cameraRepository = cameraRepo
            // Asegúrate de que MainViewModelFactory acepte estas interfaces
        )

        // 5. Obtener el ViewModel usando el Factory
        val mainViewModel = ViewModelProvider(this, viewModelFactory)[MainViewModel::class.java]

        // --- Fin: Instanciación Manual de Dependencias ---


        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                // El resto del código de la UI permanece igual
                PermissionHandler(
                    permissions = listOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.CAMERA
                    ),
                    onPermissionsGranted = {
                        MainScreen(mainViewModel) // Pasa el ViewModel creado manualmente
                    },
                    rationaleContent = { showRationale, onRationaleReply ->
                        if (showRationale) {
                            PermissionRationaleDialog(onConfirm = { onRationaleReply(true) })
                        }
                    },
                    onPermissionsDenied = { permanently ->
                        PermissionsDeniedScreen(permanently = permanently)
                    },
                    permissionRequester = { permissionsToRequest, onResult ->
                        permissionCallback = onResult
                        requestMultiplePermissions.launch(permissionsToRequest.toTypedArray())
                    }
                )
            }
        }
    }
}