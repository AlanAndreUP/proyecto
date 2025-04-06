package com.actividad1.rutasegura.ui.theme.screen


import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.actividad1.rutasegura.data.model.ScanResult
// Importa otros componentes necesarios, como el MapViewComponent real
// import com.tuempresa.transporteapp.ui.components.MapViewComponent // Ejemplo

@OptIn(ExperimentalMaterial3Api::class) // Para DropdownMenu y otros componentes M3
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel() // Obtiene el ViewModel inyectado por Hilt
) {
    // Recolecta los estados del ViewModel de forma segura para el ciclo de vida
    val userLocation by viewModel.userLocation.collectAsStateWithLifecycle()
    val availableRoutes by viewModel.availableRoutes.collectAsStateWithLifecycle()
    val simulatedBuses by viewModel.simulatedBuses.collectAsStateWithLifecycle()
    val isSimulationRunning by viewModel.isSimulationRunning.collectAsStateWithLifecycle()
    val nearestBusEta by viewModel.nearestBusEta.collectAsStateWithLifecycle()
    val scanResult by viewModel.scanResult.collectAsStateWithLifecycle()
    val isLoadingScan by viewModel.isLoadingScan.collectAsStateWithLifecycle()
    val isLoadingRoutes by viewModel.isLoadingRoutes.collectAsStateWithLifecycle()

    // Estado local para la UI
    var selectedRouteId by remember { mutableStateOf<String?>(null) }
    var showRouteSelector by remember { mutableStateOf(false) }
    var showScanResultDialog by remember { mutableStateOf(false) }

    // Observa el resultado del escaneo para decidir si mostrar el diálogo
    // Usamos un Side Effect para evitar lógica compleja directamente en la composición
    LaunchedEffect(scanResult) {
        showScanResultDialog = scanResult != null // Muestra el diálogo si hay un resultado
    }

    // Estructura principal de la pantalla
    Scaffold(
        bottomBar = {
            // Mueve el panel de control a la BottomAppBar para un diseño más estándar
            AppBottomBar(
                viewModel = viewModel,
                availableRoutes = availableRoutes,
                isLoadingRoutes = isLoadingRoutes,
                isSimulationRunning = isSimulationRunning,
                isLoadingScan = isLoadingScan,
                selectedRouteId = selectedRouteId,
                onRouteSelected = { selectedRouteId = it }, // Actualiza el estado local
                showRouteSelector = showRouteSelector,
                onShowRouteSelectorChange = { showRouteSelector = it } // Actualiza estado local
            )
        }
    ) { paddingValues -> // paddingValues proporcionado por Scaffold

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Aplica el padding de Scaffold
        ) {

            // --- Mapa (Placeholder) ---
            // Aquí iría tu componente de mapa real (GoogleMap, MapboxMap, etc.)
            // Pasarle userLocation y simulatedBuses para dibujar marcadores.
            Box(
                modifier = Modifier.fillMaxSize(), // Ocupa todo el espacio disponible
                contentAlignment = Alignment.Center
            ) {
                // Reemplaza esto con tu componente de mapa real
                Text(
                    text = "MAPA\nUsuario: ${userLocation?.latitude?.format(4)}, ${userLocation?.longitude?.format(4)}\n" +
                            "Buses Simulados: ${simulatedBuses.size}\n" +
                            "ETA: ${nearestBusEta ?: "N/A"}",
                    modifier = Modifier.padding(16.dp)
                )
                // Ejemplo de cómo pasarías los datos a un mapa real:
                // MapViewComponent(
                //     userLocation = userLocation,
                //     buses = simulatedBuses,
                //     modifier = Modifier.fillMaxSize()
                // )
            }

            // --- Diálogo de Resultado de Escaneo ---
            if (showScanResultDialog) {
                ScanResultDialog(
                    scanResult = scanResult, // Pasa el resultado actual
                    onDismiss = {
                        showScanResultDialog = false // Oculta el diálogo
                        viewModel.clearScanResult() // Limpia el estado en el ViewModel
                    }
                )
            }
        } // Fin Box Principal (contenido de Scaffold)
    } // Fin Scaffold
} // Fin MainScreen Composable

// --- Componente para la Barra Inferior ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppBottomBar(
    viewModel: MainViewModel, // Pasa el ViewModel para llamar funciones
    availableRoutes: List<com.actividad1.rutasegura.data.model.Route>,
    isLoadingRoutes: Boolean,
    isSimulationRunning: Boolean,
    isLoadingScan: Boolean,
    selectedRouteId: String?,
    onRouteSelected: (String?) -> Unit, // Callback para actualizar la selección
    showRouteSelector: Boolean,
    onShowRouteSelectorChange: (Boolean) -> Unit // Callback para mostrar/ocultar dropdown
) {
    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.surfaceVariant // Un color de fondo
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween // Espacio entre elementos
        ) {
            // Dropdown para seleccionar ruta (con estado manejado externamente)
            Box {
                OutlinedButton( // Un botón diferente para el selector
                    onClick = { onShowRouteSelectorChange(true) },
                    enabled = !isSimulationRunning && !isLoadingRoutes,
                    modifier = Modifier.width(12.dp) // Ocupa más espacio
                ) {
                    Text(availableRoutes.find { it.id == selectedRouteId }?.name ?: "Ruta", maxLines = 1)
                    if (isLoadingRoutes) {
                        Spacer(Modifier.width(8.dp))
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    }
                }
                DropdownMenu(
                    expanded = showRouteSelector,
                    onDismissRequest = { onShowRouteSelectorChange(false) }
                ) {
                    if (availableRoutes.isEmpty() && !isLoadingRoutes) {
                        DropdownMenuItem(text = {Text("No hay rutas")}, onClick = { }, enabled = false)
                    }
                    availableRoutes.forEach { route ->
                        DropdownMenuItem(
                            text = { Text(route.name) },
                            onClick = {
                                onRouteSelected(route.id) // Llama al callback
                                onShowRouteSelectorChange(false) // Cierra el menú
                            }
                        )
                    }
                    // Opción para deseleccionar
                    if (selectedRouteId != null) {
                        Divider()
                        DropdownMenuItem(text = {Text("Limpiar selección")}, onClick = {
                            onRouteSelected(null)
                            onShowRouteSelectorChange(false)
                        })
                    }
                }
            }

            Spacer(Modifier.width(8.dp)) // Espacio entre botones

            // Botón Iniciar/Detener Simulación
            Button(
                modifier = Modifier.weight(1f), // Ocupa espacio
                onClick = {
                    if (isSimulationRunning) {
                        viewModel.stopSimulation()
                    } else {
                        selectedRouteId?.let { viewModel.startSimulation(it) }
                    }
                },
                enabled = selectedRouteId != null || isSimulationRunning
            ) {
                Text(if (isSimulationRunning) "Detener" else "Iniciar", maxLines = 1)
            }

            Spacer(Modifier.width(8.dp)) // Espacio entre botones

            // Botón de Escaneo QR
            Button(
                modifier = Modifier.weight(1f), // Ocupa espacio
                onClick = { viewModel.performScan() },
                enabled = !isLoadingScan
            ) {
                Text("Scan QR", maxLines = 1)
                // Podrías mostrar indicador de carga aquí también si quieres
            }
        }
    }
}


// --- Componente para el Diálogo de Resultado ---
@Composable
private fun ScanResultDialog(
    scanResult: ScanResult?, // Recibe el resultado
    onDismiss: () -> Unit // Callback para cuando se cierra
) {
    AlertDialog(
        onDismissRequest = onDismiss, // Llama al callback al descartar
        title = { Text("Resultado del Escaneo") },
        text = {
            // Muestra el texto según el tipo de resultado
            when (scanResult) {
                is ScanResult.Success -> Text("Colectivo identificado:\n${scanResult.content}")
                is ScanResult.Error -> Text("Error al escanear el código.")
                is ScanResult.Cancelled -> Text("Escaneo cancelado por el usuario.")
                null -> Text("...") // Estado inesperado si el diálogo es visible
            }
        },
        confirmButton = {
            // Botón para cerrar el diálogo
            Button(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}


// Función de extensión simple para formatear coordenadas (puede ir a util)
fun Double.format(digits: Int) = "%.${digits}f".format(this)