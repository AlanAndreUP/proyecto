package com.actividad1.rutasegura.ui.theme.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.actividad1.rutasegura.data.model.ScanResult
import com.actividad1.rutasegura.data.model.SimulatedBusState
import com.actividad1.rutasegura.data.model.UserLocation
import com.actividad1.rutasegura.data.model.Route

@Composable
fun MainScreen(
    viewModel: MainViewModel
) {
    val userLocation by viewModel.userLocation.collectAsState()
    val availableRoutes by viewModel.availableRoutes.collectAsState()
    val simulatedBuses by viewModel.simulatedBuses.collectAsState()
    val isSimulationRunning by viewModel.isSimulationRunning.collectAsState()
    val nearestBusEta by viewModel.nearestBusEta.collectAsState()
    val scanResult by viewModel.scanResult.collectAsState()
    val isLoadingScan by viewModel.isLoadingScan.collectAsState()
    val isLoadingRoutes by viewModel.isLoadingRoutes.collectAsState()

    var selectedRouteId by remember { mutableStateOf<String?>(null) }
    var showRouteSelector by remember { mutableStateOf(false) }
    var showScanResultDialog by remember { mutableStateOf(false) }

    LaunchedEffect(scanResult) {
        showScanResultDialog = scanResult != null
    }

    Scaffold(
        bottomBar = {
            AppBottomBar(
                viewModel = viewModel,
                availableRoutes = availableRoutes,
                isLoadingRoutes = isLoadingRoutes,
                isSimulationRunning = isSimulationRunning,
                isLoadingScan = isLoadingScan,
                selectedRouteId = selectedRouteId,
                onRouteSelected = { selectedRouteId = it },
                showRouteSelector = showRouteSelector,
                onShowRouteSelectorChange = { showRouteSelector = it }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            MapContent(
                userLocation = userLocation,
                simulatedBuses = simulatedBuses,
                nearestBusEta = nearestBusEta
            )

            if (showScanResultDialog) {
                ScanResultDialog(
                    scanResult = scanResult,
                    onDismiss = {
                        showScanResultDialog = false
                        viewModel.clearScanResult()
                    }
                )
            }
        }
    }
}

@Composable
fun ScanResultDialog(
    scanResult: ScanResult?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Resultado del Escaneo",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            val message = when (scanResult) {
                is ScanResult.Success -> "Colectivo identificado:\n${scanResult.content}"
                is ScanResult.Error -> "❌ Error al escanear el código."
                is ScanResult.Cancelled -> "⚠️ Escaneo cancelado por el usuario."
                null -> "..."
            }
            Text(text = message, style = MaterialTheme.typography.bodyMedium)
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text("OK")
            }
        },
        modifier = Modifier.padding(16.dp)
    )
}

@Composable
private fun MapContent(
    userLocation: UserLocation?,
    simulatedBuses: List<SimulatedBusState>,
    nearestBusEta: String?
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(24.dp)
                .wrapContentSize(),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🗺️ Vista del Mapa", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    "📍 Usuario: ${userLocation?.latitude?.format(4)}, ${
                        userLocation?.longitude?.format(
                            4
                        )
                    }"
                )
                Text("🚌 Buses Simulados: ${simulatedBuses.size}")
                Text("⏱️ ETA: ${nearestBusEta ?: "N/A"}")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppBottomBar(
    viewModel: MainViewModel,
    availableRoutes: List<Route>,
    isLoadingRoutes: Boolean,
    isSimulationRunning: Boolean,
    isLoadingScan: Boolean,
    selectedRouteId: String?,
    onRouteSelected: (String?) -> Unit,
    showRouteSelector: Boolean,
    onShowRouteSelectorChange: (Boolean) -> Unit
) {
    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box {
                OutlinedButton(
                    onClick = { onShowRouteSelectorChange(true) },
                    enabled = !isSimulationRunning && !isLoadingRoutes,
                ) {
                    Text(
                        availableRoutes.find { it.id == selectedRouteId }?.name
                            ?: "Seleccionar Ruta"
                    )
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
                        DropdownMenuItem(
                            text = { Text("No hay rutas") },
                            onClick = {},
                            enabled = false
                        )
                    }
                    availableRoutes.forEach { route ->
                        DropdownMenuItem(
                            text = { Text(route.name) },
                            onClick = {
                                onRouteSelected(route.id)
                                onShowRouteSelectorChange(false)
                            }
                        )
                    }
                    if (selectedRouteId != null) {
                        Divider()
                        DropdownMenuItem(
                            text = { Text("Limpiar selección") },
                            onClick = {
                                onRouteSelected(null)
                                onShowRouteSelectorChange(false)
                            }
                        )
                    }
                }
            }

            Button(
                onClick = {
                    if (isSimulationRunning) viewModel.stopSimulation()
                    else selectedRouteId?.let { viewModel.startSimulation(it) }
                },
                enabled = selectedRouteId != null || isSimulationRunning,
                modifier = Modifier.weight(1f)
            ) {
                Text(if (isSimulationRunning) "Detener" else "Iniciar")
            }

            Button(
                onClick = { viewModel.performScan() },
                enabled = !isLoadingScan,
                modifier = Modifier.weight(1f)
            ) {
                Text("Escanear QR")
            }
        }
    }
}

// Extension function para formatear decimales
fun Double.format(digits: Int) = "%.${digits}f".format(this)
