package com.actividad1.rutasegura.ui.theme.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow // Para TextOverflow.Ellipsis
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView // Para usar Vistas Android
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.actividad1.rutasegura.R // Asegúrate que exista R y los drawables referenciados
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
import androidx.compose.material3.DropdownMenuItem as DropdownMenuItem1

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel, onNavigateToLogin: () -> Unit, onScanQrClicked: () -> Unit) {
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
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Bus Tracker",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = onNavigateToLogin) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = "Login",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            )
        },
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
                onShowRouteSelectorChange = { showRouteSelector = it },
                onScanQrClicked = onScanQrClicked
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Mapa principal
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

            // Tarjeta de información flotante
            InfoCard(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .padding(horizontal = 16.dp),
                nearestBusEta = nearestBusEta,
                selectedRoute = selectedRoute,
                isSimulationRunning = isSimulationRunning
            )

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

@Composable
fun InfoCard(
    modifier: Modifier = Modifier,
    nearestBusEta: String?,
    selectedRoute: com.actividad1.rutasegura.data.model.Route?,
    isSimulationRunning: Boolean
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Información de ruta seleccionada
            if (selectedRoute != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.directions_bus_24px),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = selectedRoute.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Información de ETA
            if (nearestBusEta != null && isSimulationRunning) {
                Divider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.access_time_filled),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Próximo bus: $nearestBusEta",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else if (isSimulationRunning) {
                // Estado de carga cuando la simulación está corriendo pero aún no hay ETA
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Calculando tiempo de llegada...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Mensaje cuando no hay simulación
            if (selectedRoute != null && !isSimulationRunning) {
                Text(
                    text = "Pulse 'Iniciar' para comenzar simulación",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Composable para el mapa de osmdroid (sin cambios)
@Composable
fun OsmMapView(
    modifier: Modifier = Modifier,
    userLocation: UserLocation?,
    simulatedBuses: List<SimulatedBusState>,
    selectedRoute: com.actividad1.rutasegura.data.model.Route?,
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

// Diálogo de resultado de escaneo mejorado
@Composable
fun ScanResultDialog(
    scanResult: ScanResult?,
    onDismiss: () -> Unit
) {
    if (scanResult == null) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = when (scanResult) {
                        is ScanResult.Success -> painterResource(R.drawable.qr_code_scanner_24px)
                        is ScanResult.Cancelled -> painterResource(R.drawable.cancel_24px)
                        is ScanResult.Error -> painterResource(R.drawable.error_24px)
                    },
                    contentDescription = null,
                    tint = when (scanResult) {
                        is ScanResult.Success -> MaterialTheme.colorScheme.primary
                        is ScanResult.Cancelled -> MaterialTheme.colorScheme.secondary
                        is ScanResult.Error -> MaterialTheme.colorScheme.error
                    }
                )
                Text(
                    "Resultado del Escaneo",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        text = {
            val message = when (scanResult) {
                is ScanResult.Success -> "Colectivo identificado:\n${scanResult.content}"
                is ScanResult.Error -> "❌ Error al escanear el código."
                is ScanResult.Cancelled -> "⚠️ Escaneo cancelado por el usuario."
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Aceptar")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.padding(16.dp)
    )
}

// AppBottomBar mejorado
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppBottomBar(
    viewModel: MainViewModel,
    availableRoutes: List<com.actividad1.rutasegura.data.model.Route>,
    isLoadingRoutes: Boolean,
    isSimulationRunning: Boolean,
    isLoadingScan: Boolean,
    selectedRouteId: String?,
    onRouteSelected: (String?) -> Unit,
    showRouteSelector: Boolean,
    onShowRouteSelectorChange: (Boolean) -> Unit,
    onScanQrClicked: () -> Unit
) {
    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Separador superior con el color del tema
            Divider(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                thickness = 1.dp
            )

            // Contenido de la barra inferior
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Selector de Ruta
                Box(modifier = Modifier.weight(1.5f)) {
                    OutlinedButton(
                        onClick = { onShowRouteSelectorChange(true) },
                        enabled = !isSimulationRunning && !isLoadingRoutes,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (!isSimulationRunning && !isLoadingRoutes)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.route_24px),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = availableRoutes.find { it.id == selectedRouteId }?.name
                                    ?: "Seleccionar Ruta",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        if (isLoadingRoutes) {
                            Spacer(Modifier.width(8.dp))
                            CircularProgressIndicator(
                                Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showRouteSelector,
                        onDismissRequest = { onShowRouteSelectorChange(false) },
                        modifier = Modifier.widthIn(min = 200.dp)
                    ) {
                        if (availableRoutes.isEmpty() && !isLoadingRoutes) {
                            DropdownMenuItem1(
                                text = {
                                    Text(
                                        "No hay rutas disponibles",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                onClick = {},
                                enabled = false
                            )
                        }

                        availableRoutes.forEach { route ->
                            DropdownMenuItem1(
                                text = {
                                    Text(
                                        route.name,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                onClick = {
                                    onRouteSelected(route.id)
                                    onShowRouteSelectorChange(false)
                                },
                                leadingIcon = {
                                    if (route.id == selectedRouteId) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        Icon(
                                            painter = painterResource(R.drawable.directions_bus_24px),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            )
                        }

                        if (selectedRouteId != null) {
                            Divider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                            DropdownMenuItem1(
                                text = {
                                    Text(
                                        "Limpiar selección",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                onClick = {
                                    onRouteSelected(null)
                                    onShowRouteSelectorChange(false)
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Clear,
                                        contentDescription = "Clear",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            )
                        }
                    }
                }

                // Botones de acción
                Button(
                    onClick = {
                        if (isSimulationRunning) viewModel.stopSimulation()
                        else selectedRouteId?.let { viewModel.startSimulation(it) }
                    },
                    enabled = (selectedRouteId != null && !isSimulationRunning) || isSimulationRunning,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSimulationRunning)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isSimulationRunning) {
                            Icon(
                                painter = painterResource(id = R.drawable.stop_24px),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            if (isSimulationRunning) "Detener" else "Iniciar",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                // Botón Escanear
                Button(
                    onClick = { onScanQrClicked() },
                    enabled = !isLoadingScan,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isLoadingScan) {
                            CircularProgressIndicator(
                                Modifier.size(18.dp),
                                color = LocalContentColor.current,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.stop_24px),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                "Scan",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }
}

fun Double.format(digits: Int): String {
    return String.format("%.${digits}f", this)
}