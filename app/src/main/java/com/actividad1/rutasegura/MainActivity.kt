package com.actividad1.rutasegura

import android.Manifest
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel // Import para viewModel() fácil
import androidx.navigation.NavGraph.Companion.findStartDestination // Corregir import si es necesario
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
// import androidx.navigation.navigation // No es estrictamente necesario aquí
import com.actividad1.rutasegura.data.repository.*
import com.actividad1.rutasegura.data.simulation.SimulationEngine
import com.actividad1.rutasegura.ui.theme.screen.* // Importa todas tus pantallas y VMs
import com.actividad1.rutasegura.util.PermissionHandler // Tu manejador de permisos
import kotlinx.coroutines.Dispatchers
import androidx.activity.compose.rememberLauncherForActivityResult // Importar
import androidx.compose.foundation.isSystemInDarkTheme
import com.actividad1.rutasegura.data.model.ScanResult
import com.actividad1.rutasegura.ui.theme.AppTheme
import com.actividad1.rutasegura.ui.theme.ElectricTheme
import com.actividad1.rutasegura.ui.theme.components.LocationProviderWrapper
import com.actividad1.rutasegura.util.ScanQrCodeContract
// Sealed class para definir las rutas de navegación de forma segura
sealed class AppScreen(val route: String) {
    object Login : AppScreen("login_screen")
    object AdminDashboard : AppScreen("admin_dashboard_screen")
    object MainUser : AppScreen("main_user_screen")
}

class MainActivity : ComponentActivity() {

    // --- Lógica de Permisos ---
    private lateinit var permissionCallback: (Map<String, Boolean>) -> Unit
    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (::permissionCallback.isInitialized) {
                permissionCallback(permissions)
            }
        }

    // --- ViewModel principal ---
    private lateinit var mainViewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- Instanciación Manual de Dependencias para MainViewModel ---
        val appContext: Context = applicationContext
        // Asegúrate que la ruta a LocationProviderWrapper sea correcta
        val locationWrapper = LocationProviderWrapper(appContext)
        val routeRepo: RouteRepository = RouteRepositoryImpl()
        val simulationEngine = SimulationEngine(routeRepo, Dispatchers.Default)
        val locationRepo: LocationRepository = LocationRepositoryImpl(locationWrapper, Dispatchers.IO)
        val simulationRepo: SimulationRepository = SimulationRepositoryImpl(simulationEngine)
        val cameraRepo: CameraRepository = CameraRepositoryImpl(appContext)

        val viewModelFactory = MainViewModelFactory(
            locationRepository = locationRepo,
            routeRepository = routeRepo,
            simulationRepository = simulationRepo,
            cameraRepository = cameraRepo
        )
        mainViewModel = ViewModelProvider(this, viewModelFactory)[MainViewModel::class.java]
        // --- Fin Instanciación Manual ---

        setContent {
            // Aplica el tema aquí - puedes elegir entre AppTheme o ElectricTheme
            ElectricTheme(darkTheme = isSystemInDarkTheme()) {
                // Solo un Surface es necesario aquí
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

    fun requestPermissions(permissions: List<String>, callback: (Map<String, Boolean>) -> Unit) {
        permissionCallback = callback
        requestMultiplePermissions.launch(permissions.toTypedArray())
    }
}

@Composable
fun AppNavigation(
    mainViewModel: MainViewModel,
    activity: MainActivity
) {
    val navController = rememberNavController()
    val adminViewModel: AdminViewModel = viewModel() // VM para admin
    val scanQrLauncher = rememberLauncherForActivityResult(
        contract = ScanQrCodeContract(), // Usa nuestro Contract personalizado
        onResult = { result: String? ->
            // --- Resultado del Escaner ---
            if (result != null) {
                Log.i("AppNavigation", "Resultado del escaneo recibido: $result")
                // Actualiza el ViewModel con el resultado exitoso
                mainViewModel.updateScanResult(ScanResult.Success(result))
            } else {
                Log.w("AppNavigation", "Escaneo cancelado o fallido")

                mainViewModel.updateScanResult(ScanResult.Cancelled)
            }
        }
    )
    NavHost(
        navController = navController,
        startDestination = AppScreen.MainUser.route
    ) {

        // Pantalla de Login
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
            // Aquí podrías añadir un botón para ir a MainUser si no se requiere login admin
            // Button(onClick = { navController.navigate(AppScreen.MainUser.route) }) { Text("Ir al mapa (Usuario)") }
        }

        // Pantalla de Admin Dashboard
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
            // Aquí podrías añadir un botón para ir a MainUser desde el dashboard de admin
            // Button(onClick = { navController.navigate(AppScreen.MainUser.route) }) { Text("Ver Mapa Usuario") }
        }

        // Pantalla Principal del Usuario (Mapa)
        composable(route = AppScreen.MainUser.route) {
            PermissionHandler(
                permissions = listOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.CAMERA // Permiso aún necesario para la función Escanear
                ),
                onPermissionsGranted = {
                    MainScreen(
                        viewModel = mainViewModel,
                        onNavigateToLogin = { // Define la acción para navegar a Login
                            navController.navigate(AppScreen.Login.route) {

                                launchSingleTop = true
                            }
                        },
                        onScanQrClicked  = {

                        scanQrLauncher.launch(null)
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

    } // Fin NavHost
}