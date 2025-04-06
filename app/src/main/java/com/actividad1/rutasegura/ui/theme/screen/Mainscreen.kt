package com.actividad1.rutasegura.ui.theme.screen

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList // Import para mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color // Para Polyline de Compose (aunque usamos Android Color para osmdroid)
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextOverflow // Para TextOverflow.Ellipsis
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView // Para usar Vistas Android
import androidx.core.content.ContextCompat // Para obtener drawables
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle // ¡Importante!
import com.actividad1.rutasegura.R // Asegúrate que exista R y los drawables referenciados
import com.actividad1.rutasegura.data.model.Route
import com.actividad1.rutasegura.data.model.ScanResult
import com.actividad1.rutasegura.data.model.SimulatedBusState
import com.actividad1.rutasegura.data.model.UserLocation

// Imports específicos de osmdroid
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@Composable
fun MainScreen(viewModel: MainViewModel, onNavigateToLogin: () -> Unit,onScanQrClicked: () -> Unit ) {
    val userLocation by viewModel.userLocation.collectAsState()
    val availableRoutes by viewModel.availableRoutes.collectAsState()
    val simulatedBuses by viewModel.simulatedBuses.collectAsState()
    val isSimulationRunning by viewModel.isSimulationRunning.collectAsState()
    val nearestBusEta by viewModel.nearestBusEta.collectAsState()
    val scanResult by viewModel.scanResult.collectAsState()
    val isLoadingScan by viewModel.isLoadingScan.collectAsState()
    val isLoadingRoutes by viewModel.isLoadingRoutes.collectAsState()

    var selectedRouteId by remember { mutableStateOf<String?>(null) }
    val selectedRoute = remember(selectedRouteId, availableRoutes) {
        availableRoutes.find { it.id == selectedRouteId }
    }
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
                onShowRouteSelectorChange = { showRouteSelector = it } ,
                onNavigateToLogin = onNavigateToLogin,
                onScanQrClicked = onScanQrClicked
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            OsmMapView(
                modifier = Modifier.fillMaxSize(),
                userLocation = userLocation,
                simulatedBuses = simulatedBuses,
                selectedRoute = selectedRoute,
                onMapReady = { mapView ->
                    // Centrar mapa si es necesario
                    if (mapView.mapCenter == GeoPoint(0.0, 0.0) || userLocation == null) {
                        mapView.controller.setCenter(GeoPoint(16.7531, -93.1156)) // Centro aproximado
                        mapView.controller.setZoom(12.0)
                    }
                }
            )

            // Mostrar indicador de ETA
            nearestBusEta?.let { eta ->
                Card(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp),
                    elevation = CardDefaults.cardElevation(4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Próximo bus: $eta",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Diálogo de resultado de escaneo
            if (showScanResultDialog) {
                ScanResultDialog(
                    scanResult = scanResult,
                    onDismiss = {
                        showScanResultDialog = false
                        viewModel.clearScanResult() // Limpiar resultado
                    }
                )
            }
        }
    }
}

// Composable para el mapa de osmdroid
@Composable
fun OsmMapView(
    modifier: Modifier = Modifier,
    userLocation: UserLocation?,
    simulatedBuses: List<SimulatedBusState>,
    selectedRoute: Route?,
    onMapReady: (MapView) -> Unit = {} // Callback opcional
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember { MapView(context) }

    // Efecto para manejar el ciclo de vida del MapView
    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_DESTROY -> mapView.onDetach() // Limpieza final
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    AndroidView(
        factory = { ctx ->
            mapView.apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(12.0)
                controller.setCenter(GeoPoint(16.7531, -93.1156)) // Centro inicial

                val locationProvider = GpsMyLocationProvider(ctx)
                val locationOverlay = MyLocationNewOverlay(locationProvider, this).apply {
                    enableMyLocation()
                }
                overlays.add(locationOverlay)
            }
            mapView
        },
        update = { view ->
            userLocation?.let { loc ->
                val userGeoPoint = GeoPoint(loc.latitude, loc.longitude)
                view.controller.animateTo(userGeoPoint, 15.0, 1000L)
            }

            // Limpiar overlays antiguos
            view.overlays.clear()

            // Agregar marcadores para buses simulados
            simulatedBuses.forEach { bus ->
                val busGeoPoint = GeoPoint(bus.currentLocation.latitude, bus.currentLocation.longitude)
                val busMarker = Marker(view).apply {
                    position = busGeoPoint
                    title = "Bus ${bus.busId} (${bus.routeId})"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                view.overlays.add(busMarker)
            }

            // Agregar polyline para la ruta seleccionada
            selectedRoute?.points?.let { points ->
                if (points.size >= 2) {
                    val geoPoints = points.map { GeoPoint(it.latitude, it.longitude) }
                    val poly = Polyline().apply {
                        setPoints(geoPoints)
                        outlinePaint.color = android.graphics.Color.BLUE
                        outlinePaint.strokeWidth = 10f
                    }
                    view.overlays.add(poly)
                }
            }

            view.invalidate()
        },
        modifier = modifier
    )
}

// --- ScanResultDialog ---
@Composable
fun ScanResultDialog(
    scanResult: ScanResult?,
    onDismiss: () -> Unit
) {
    if (scanResult == null) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Resultado del Escaneo", style = MaterialTheme.typography.titleLarge) },
        text = {
            val message = when (scanResult) {
                is ScanResult.Success -> "Colectivo identificado:\n${scanResult.content}"
                is ScanResult.Error -> "❌ Error al escanear el código."
                is ScanResult.Cancelled -> "⚠️ Escaneo cancelado por el usuario."
                // El caso null ya está cubierto por la guarda al inicio
            }
            Text(text = message, style = MaterialTheme.typography.bodyMedium)
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Text("OK")
            }
        },
        modifier = Modifier.padding(16.dp)
    )
}


// --- AppBottomBar ---
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
    onShowRouteSelectorChange: (Boolean) -> Unit,
    onNavigateToLogin: ()-> Unit,
    onScanQrClicked: () -> Unit
) {
    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp) // Ajustar padding vertical
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Selector de Ruta
            Box(modifier = Modifier.weight(1.5f)) {
                OutlinedButton(
                    onClick = { onShowRouteSelectorChange(true) },
                    enabled = !isSimulationRunning && !isLoadingRoutes,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp) // Ajustar padding interno
                ) {
                    Text(
                        text = availableRoutes.find { it.id == selectedRouteId }?.name ?: "Seleccionar Ruta",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
                        DropdownMenuItem(text = { Text("No hay rutas") }, onClick = {}, enabled = false)
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
                        Divider(modifier = Modifier.padding(vertical = 4.dp))
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

            // Botón Iniciar/Detener
            Button(
                onClick = {
                    if (isSimulationRunning) viewModel.stopSimulation()
                    else selectedRouteId?.let { viewModel.startSimulation(it) }
                },
                enabled = (selectedRouteId != null && !isSimulationRunning) || isSimulationRunning,
                modifier = Modifier.weight(1f)
            ) {
                Text(if (isSimulationRunning) "Detener" else "Iniciar")
            }

            Button(
                onClick = { onScanQrClicked() },
                enabled = !isLoadingScan,
                modifier = Modifier.weight(1f)
            ) {
                if (isLoadingScan) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = LocalContentColor.current, strokeWidth = 2.dp)
                } else {
                    Text("Escanear")
                }
            }
            Button(

                onClick = { onNavigateToLogin() },
                enabled = true,
                modifier = Modifier.weight(1f)
            ) {
                Text("Iniciar Sesión") // Texto corregido
            }
        }
    }
}



fun Double.format(digits: Int): String {

    return String.format("%.${digits}f", this)
}

