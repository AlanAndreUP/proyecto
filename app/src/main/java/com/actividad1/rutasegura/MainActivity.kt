package com.actividad1.rutasegura

import android.Manifest
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
// import androidx.activity.result.contract.ActivityResultContracts // Ya no es necesario aquí para el scanner
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.* // Importar para LaunchedEffect, etc.
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.actividad1.rutasegura.data.repository.*
import com.actividad1.rutasegura.data.simulation.SimulationEngine
import com.actividad1.rutasegura.ui.theme.screen.*
import com.actividad1.rutasegura.util.PermissionHandler
import kotlinx.coroutines.Dispatchers
// import androidx.activity.compose.rememberLauncherForActivityResult // No necesario para el scanner aquí
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.actividad1.rutasegura.data.model.ScanResult
import com.actividad1.rutasegura.ui.theme.ElectricTheme // O AppTheme
import com.actividad1.rutasegura.ui.theme.components.LocationProviderWrapper
// import com.actividad1.rutasegura.util.ScanQrCodeContract // Ya no se usa

// Sealed class para definir las rutas de navegación de forma segura
sealed class AppScreen(val route: String) {
    object Login : AppScreen("login_screen")
    object AdminDashboard : AppScreen("admin_dashboard_screen")
    object MainUser : AppScreen("main_user_screen")
    object Scanner : AppScreen("scanner_screen") // <<<--- AÑADIR RUTA PARA EL SCANNER
}

class MainActivity : ComponentActivity() {

    // --- Lógica de Permisos (se mantiene igual) ---
    private lateinit var permissionCallback: (Map<String, Boolean>) -> Unit
    private val requestMultiplePermissions =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()) { permissions -> // Usar el específico
            if (::permissionCallback.isInitialized) {
                permissionCallback(permissions)
            }
        }

    // --- ViewModel principal (se mantiene igual) ---
    private lateinit var mainViewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- Instanciación Manual de Dependencias (se mantiene igual) ---
        val appContext: Context = applicationContext
        val locationWrapper = LocationProviderWrapper(appContext)
        val routeRepo: RouteRepository = RouteRepositoryImpl()
        val simulationEngine = SimulationEngine(routeRepo, Dispatchers.Default)
        val locationRepo: LocationRepository = LocationRepositoryImpl(locationWrapper, Dispatchers.IO)
        val simulationRepo: SimulationRepository = SimulationRepositoryImpl(simulationEngine)
        val cameraRepo: CameraRepository = CameraRepositoryImpl(appContext) // Mantener si se usa en otro lado

        val viewModelFactory = MainViewModelFactory(
            locationRepository = locationRepo,
            routeRepository = routeRepo,
            simulationRepository = simulationRepo,
            cameraRepository = cameraRepo // Pasar el repo de cámara
        )
        mainViewModel = ViewModelProvider(this, viewModelFactory)[MainViewModel::class.java]
        // --- Fin Instanciación Manual ---

        setContent {
            ElectricTheme(darkTheme = isSystemInDarkTheme()) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        mainViewModel = mainViewModel,
                        activity = this
                    )
                }
            }
        }
    }

    // Método para solicitar permisos (se mantiene igual)
    fun requestPermissions(permissions: List<String>, callback: (Map<String, Boolean>) -> Unit) {
        permissionCallback = callback
        requestMultiplePermissions.launch(permissions.toTypedArray())
    }
}

@Composable
fun AppNavigation(
    mainViewModel: MainViewModel,
    activity: MainActivity // Mantener si PermissionHandler lo necesita
) {
    val navController = rememberNavController()
    val adminViewModel: AdminViewModel = viewModel()

    // --- Ya NO necesitamos el scanQrLauncher aquí ---
    // val scanQrLauncher = rememberLauncherForActivityResult(...) { ... }

    NavHost(
        navController = navController,
        startDestination = AppScreen.MainUser.route // O Login si es el flujo normal
    ) {

        // Pantalla de Login (sin cambios)
        composable(route = AppScreen.Login.route) {
            LoginScreen(
                viewModel = adminViewModel,
                onLoginSuccess = {
                    navController.navigate(AppScreen.AdminDashboard.route) {
                        popUpTo(AppScreen.Login.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        // Pantalla de Admin Dashboard (sin cambios)
        composable(route = AppScreen.AdminDashboard.route) {
            AdminDashboardScreen(
                viewModel = adminViewModel,
                onLogout = {
                    navController.navigate(AppScreen.Login.route) {
                        popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        // Pantalla Principal del Usuario (Mapa)
        composable(route = AppScreen.MainUser.route) { navBackStackEntry -> // Obtener navBackStackEntry

            // --- Lógica para obtener el resultado del Scanner ---
            LaunchedEffect(key1 = navBackStackEntry) {
                val savedStateHandle = navBackStackEntry.savedStateHandle
                val scanResult = savedStateHandle.get<String?>("scan_result") // Usar la clave que definimos

                if (scanResult != null) {
                    Log.i("AppNavigation", "Resultado del escaneo recibido: $scanResult")
                    mainViewModel.updateScanResult(ScanResult.Success(scanResult))
                    // Limpiar el resultado para evitar procesarlo de nuevo
                    savedStateHandle.remove<String?>("scan_result")
                } else {
                    // Verificar si el valor fue explícitamente null (cancelado)
                    if (savedStateHandle.contains("scan_result")) {
                        Log.w("AppNavigation", "Escaneo cancelado o fallido")
                        mainViewModel.updateScanResult(ScanResult.Cancelled)
                        savedStateHandle.remove<String?>("scan_result")
                    }
                }
            }


            PermissionHandler(
                permissions = listOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.CAMERA // Permiso necesario para ScannerScreen
                ),
                onPermissionsGranted = {
                    MainScreen(
                        viewModel = mainViewModel,
                        onNavigateToLogin = {
                            navController.navigate(AppScreen.Login.route) {
                                launchSingleTop = true
                                // Considera popUpTo si es necesario limpiar la pila
                            }
                        },
                        onScanQrClicked = {
                            // --- NAVEGAR A LA PANTALLA DEL SCANNER ---
                            navController.navigate(AppScreen.Scanner.route)
                        }
                    )
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
                    activity.requestPermissions(permissionsToRequest, onResult)
                }
            )
        }

        // --- NUEVA RUTA PARA EL SCANNER ---
        composable(route = AppScreen.Scanner.route) {
            ScannerScreen(
                onScanResult = { result ->
                    // Poner el resultado en el SavedStateHandle de la pantalla ANTERIOR (MainUser)
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("scan_result", result) // Usar una clave consistente
                    // Volver a la pantalla anterior
                    navController.popBackStack()
                }
            )
        }

    } // Fin NavHost
}