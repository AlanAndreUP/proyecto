package com.actividad1.rutasegura.ui.theme.screen

import android.util.Log
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.actividad1.rutasegura.R
import com.actividad1.rutasegura.data.model.Route // Asegúrate que la importación sea correcta
// Importar la nueva definición de ScanResult
import com.actividad1.rutasegura.data.model.ScanResult
import com.actividad1.rutasegura.data.model.SimulatedBusState
import com.actividad1.rutasegura.data.model.UserLocation

// Imports específicos de osmdroid
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import androidx.compose.material3.DropdownMenuItem as DropdownMenuItem1

// Configuración de osmdroid
fun initializeOsmdroid(context: android.content.Context) {
    Configuration.getInstance().load(context, androidx.preference.PreferenceManager.getDefaultSharedPreferences(context))
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel, onNavigateToLogin: () -> Unit, onScanQrClicked: () -> Unit) {

    // --- Inicialización de osmdroid ---
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        initializeOsmdroid(context.applicationContext)
    }

    // --- Estados del ViewModel ---
    val userLocation by viewModel.userLocation.collectAsState()
    val availableRoutes by viewModel.availableRoutes.collectAsState()
    val simulatedBuses by viewModel.simulatedBuses.collectAsState()
    val isSimulationRunning by viewModel.isSimulationRunning.collectAsState()
    val nearestBusEta by viewModel.nearestBusEta.collectAsState()
    // Usar la nueva definición de ScanResult
    val scanResultState by viewModel.scanResult.collectAsState()
    val isLoadingScan by viewModel.isLoadingScan.collectAsState()
    val isLoadingRoutes by viewModel.isLoadingRoutes.collectAsState()

    // --- Estados locales de UI ---
    var selectedRouteId by remember { mutableStateOf<String?>(null) }
    val selectedRoute = remember(selectedRouteId, availableRoutes) {
        availableRoutes.find { it.id == selectedRouteId }
    }
    var showRouteSelector by remember { mutableStateOf(false) }
    var showScanResultDialog by remember { mutableStateOf(false) }

    // --- Efecto para mostrar/ocultar el diálogo ---
    LaunchedEffect(scanResultState) {
        // El diálogo se muestra si el estado NO es Idle
        showScanResultDialog = scanResultState != ScanResult.Idle

        // Opcional: Puedes hacer logging aquí para ver los cambios de estado
        if (scanResultState != ScanResult.Idle) {
            Log.d("MainScreen", "ScanResult state changed: $scanResultState, showing dialog.")
        } else {
            Log.d("MainScreen", "ScanResult state is Idle, hiding dialog.")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ruta Segura", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = MaterialTheme.colorScheme.onPrimary),
                actions = {
                    IconButton(onClick = onNavigateToLogin) {
                        Icon(Icons.Filled.Person, contentDescription = "Login", tint = MaterialTheme.colorScheme.onPrimary)
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
                simulatedBuses = simulatedBuses.filter { selectedRouteId == null || it.routeId == selectedRouteId },
                selectedRoute = selectedRoute,
                onMapReady = { mapView ->
                    if (userLocation == null && mapView.mapCenter.latitude == 0.0 && mapView.mapCenter.longitude == 0.0) {
                        mapView.controller.setCenter(GeoPoint(16.7531, -93.1156)) // Tuxtla
                        mapView.controller.setZoom(13.0)
                    }
                }
            )

            // Tarjeta de información flotante
            InfoCard(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp, start = 16.dp, end = 16.dp),
                nearestBusEta = nearestBusEta,
                selectedRoute = selectedRoute,
                isSimulationRunning = isSimulationRunning
            )

            // --- Diálogo de Resultado del Escaneo (Adaptado) ---
            scanResultState?.let {
                ScanResultDisplayDialog(
                    showDialog = showScanResultDialog,
                    scanResult = it, // Pasamos el estado completo
                    onDismiss = {
                        Log.d("MainScreen", "Diálogo cerrado (onDismiss)")
                        showScanResultDialog = false // Ocultar
                        viewModel.clearScanResult() // Volver a Idle
                    },
                    onAccept = {
                        Log.d("MainScreen", "Botón 'Aceptar' presionado")
                        showScanResultDialog = false // Ocultar
                        viewModel.clearScanResult() // Volver a Idle
                    }
                )
            }

        } // Fin Box principal
    } // Fin Scaffold
} // Fin MainScreen


// --- Diálogo de Resultado del Escaneo (Adaptado a la nueva definición de ScanResult) ---
@Composable
fun ScanResultDisplayDialog(
    showDialog: Boolean,
    scanResult: ScanResult, // Recibe el estado completo con la nueva definición
    onDismiss: () -> Unit,
    onAccept: () -> Unit
) {
    // Solo mostrar si showDialog es true y el estado no es Idle
    if (showDialog && scanResult != ScanResult.Idle) {

        val title: String
        val message: String
        val icon: Int
        val iconTint: Color

        when (scanResult) {
            is ScanResult.Success -> {
                title = "Código Escaneado"
                // Acceder al contenido usando scanResult.content
                message = "Contenido:\n${scanResult.content}"
                icon = R.drawable.qr_code_scanner_24px // Icono de éxito
                iconTint = MaterialTheme.colorScheme.primary
            }
            ScanResult.Cancelled -> { // Usar el objeto directamente
                title = "Escaneo Cancelado"
                message = "La operación de escaneo fue cancelada por el usuario."
                icon = R.drawable.cancel_24px // Icono de cancelación
                iconTint = MaterialTheme.colorScheme.secondary
            }
            ScanResult.Error -> { // Usar el objeto directamente
                title = "Error de Escaneo"
                // Mensaje genérico porque ScanResult.Error ya no tiene detalles
                message = "Ocurrió un error durante el escaneo."
                icon = R.drawable.error_24px // Icono de error
                iconTint = MaterialTheme.colorScheme.error
            }
            // El caso Idle no debería mostrar el diálogo basado en la lógica de MainScreen
            ScanResult.Idle -> return // Salir si por alguna razón llega aquí con Idle
        }

        AlertDialog(
            onDismissRequest = onDismiss,
            icon = { Icon(painterResource(id = icon), contentDescription = null, tint = iconTint) },
            title = { Text(title) },
            text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(onClick = onAccept) {
                    Text("Aceptar")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}


// --- InfoCard, OsmMapView, AppBottomBar (Sin cambios respecto a la versión anterior) ---

@Composable
fun InfoCard(
    modifier: Modifier = Modifier,
    nearestBusEta: String?,
    selectedRoute: Route?, // Usar el tipo Route importado
    isSimulationRunning: Boolean
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f) // Un poco más opaco
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), // Ajustar padding
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Información de ruta seleccionada
            if (selectedRoute != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.route_24px), // Cambiado a ruta
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp) // Ligeramente más pequeño
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = selectedRoute.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold // Un poco menos fuerte que Bold
                    )
                }
                Divider(modifier = Modifier.padding(vertical = 4.dp)) // Separador
            }

            // Información de ETA o estado
            when {
                // ETA disponible y simulación corriendo
                nearestBusEta != null && isSimulationRunning && selectedRoute != null -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.access_time_filled),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Próximo bus: $nearestBusEta",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                // Simulación corriendo pero sin ETA (o sin ruta seleccionada)
                isSimulationRunning && selectedRoute != null -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary // Usar color primario
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Calculando tiempo de llegada...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant // Color más tenue
                        )
                    }
                }
                // Simulación detenida pero hay ruta seleccionada
                selectedRoute != null && !isSimulationRunning -> {
                    Text(
                        text = "Presiona 'Iniciar' para ver tiempos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary // Indicar acción
                    )
                }
                // No hay ruta seleccionada
                selectedRoute == null -> {
                    Text(
                        text = "Selecciona una ruta abajo.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun OsmMapView(
    modifier: Modifier = Modifier,
    userLocation: UserLocation?,
    simulatedBuses: List<SimulatedBusState>,
    selectedRoute: Route?, // Usar el tipo importado
    onMapReady: (MapView) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember { MapView(context) }

    // --- Marcador de Usuario ---
    val userMarker = remember { Marker(mapView).apply {
        // icon = ContextCompat.getDrawable(context, R.drawable.ic_user_location) // Ejemplo
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        title = "Tú estás aquí"
    }}

    // --- Marcadores de Buses ---
    val busMarkers = remember { mutableStateMapOf<String, Marker>() }

    // --- Polyline de Ruta ---
    val routePolyline = remember { Polyline().apply {
        outlinePaint.color = android.graphics.Color.BLUE
        outlinePaint.strokeWidth = 10f
    }}

    // Efecto para manejar el ciclo de vida del MapView
    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            Log.d("OsmMapView", "Disposing MapView")
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    AndroidView(
        factory = { ctx ->
            Log.d("OsmMapView", "Factory ejecutándose")
            mapView.apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
            }
            mapView.overlays.add(routePolyline)
            mapView.overlays.add(userMarker)
            onMapReady(mapView)
            mapView
        },
        update = { view ->
            Log.d("OsmMapView", "Update ejecutándose")

            // --- Actualizar Marcador de Usuario ---
            userLocation?.let { loc ->
                val userGeoPoint = GeoPoint(loc.latitude, loc.longitude)
                userMarker.position = userGeoPoint
                userMarker.setVisible(true)
            } ?: run {
                userMarker.setVisible(false)
            }

            // --- Actualizar Polyline de Ruta ---
            selectedRoute?.points?.let { points ->
                if (points.size >= 2) {
                    val geoPoints = points.map { GeoPoint(it.latitude, it.longitude) }
                    routePolyline.setPoints(geoPoints)
                    routePolyline.setVisible(true)
                } else {
                    routePolyline.setVisible(false)
                }
            } ?: run {
                routePolyline.setVisible(false)
                routePolyline.setPoints(emptyList())
            }


            // --- Actualizar Marcadores de Buses ---
            val currentBusIds = simulatedBuses.map { it.busId }.toSet()
            val markersToRemove = busMarkers.keys.filter { it !in currentBusIds }
            markersToRemove.forEach { busId ->
                view.overlays.remove(busMarkers[busId])
                busMarkers.remove(busId)
            }
            simulatedBuses.forEach { bus ->
                val busGeoPoint = GeoPoint(bus.currentLocation.latitude, bus.currentLocation.longitude)
                val marker = busMarkers.getOrPut(bus.busId) {
                    Marker(view).apply {
                        icon = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.directions_bus_24px)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        view.overlays.add(this)
                    }
                }
                marker.position = busGeoPoint
                marker.title = "Bus ${bus.busId} (Ruta ${bus.routeId})"
            }

            view.invalidate()
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppBottomBar(
    viewModel: MainViewModel,
    availableRoutes: List<Route>, // Usar el tipo importado
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
            Divider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 1.dp
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // --- Selector de Ruta ---
                Box(modifier = Modifier.weight(1.5f)) {
                    OutlinedButton(
                        onClick = { onShowRouteSelectorChange(true) },
                        enabled = !isSimulationRunning && !isLoadingRoutes,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (!isSimulationRunning && !isLoadingRoutes) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
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
                                text = availableRoutes.find { it.id == selectedRouteId }?.name ?: "Seleccionar Ruta",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelLarge
                            )
                            if (isLoadingRoutes) {
                                Spacer(Modifier.width(8.dp))
                                CircularProgressIndicator(
                                    Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // --- Menú Desplegable de Rutas ---
                    DropdownMenu(
                        expanded = showRouteSelector,
                        onDismissRequest = { onShowRouteSelectorChange(false) },
                        modifier = Modifier.widthIn(min = 200.dp)
                    ) {
                        if (availableRoutes.isEmpty() && !isLoadingRoutes) {
                            DropdownMenuItem1(
                                text = { Text("No hay rutas disponibles", style = MaterialTheme.typography.bodyMedium) },
                                onClick = {},
                                enabled = false
                            )
                        }
                        availableRoutes.forEach { route ->
                            DropdownMenuItem1(
                                text = { Text(route.name, style = MaterialTheme.typography.bodyMedium) },
                                onClick = {
                                    onRouteSelected(route.id)
                                    onShowRouteSelectorChange(false)
                                },
                                leadingIcon = {
                                    if (route.id == selectedRouteId) {
                                        Icon(Icons.Filled.Check, contentDescription = "Seleccionado", tint = MaterialTheme.colorScheme.primary)
                                    } else {
                                        Icon(painterResource(R.drawable.directions_bus_24px), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            )
                        }
                        if (selectedRouteId != null) {
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                            DropdownMenuItem1(
                                text = { Text("Limpiar selección", style = MaterialTheme.typography.bodyMedium) },
                                onClick = {
                                    onRouteSelected(null)
                                    onShowRouteSelectorChange(false)
                                },
                                leadingIcon = { Icon(Icons.Filled.Clear, contentDescription = "Limpiar", tint = MaterialTheme.colorScheme.error) }
                            )
                        }
                    }
                }

                // --- Botón Iniciar/Detener Simulación ---
                Button(
                    onClick = {
                        if (isSimulationRunning) viewModel.stopSimulation()
                        else selectedRouteId?.let { viewModel.startSimulation(it) }
                    },
                    enabled = (selectedRouteId != null && !isSimulationRunning) || isSimulationRunning,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSimulationRunning) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primary,
                        contentColor = if (isSimulationRunning) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            painter = if (isSimulationRunning) painterResource(id = R.drawable.stop_24px) else painterResource(id = R.drawable._23_24px),
                            contentDescription = null,
                            modifier = Modifier.size(if (isSimulationRunning) 16.dp else 18.dp)
                        )
                        Text(
                            if (isSimulationRunning) "Detener" else "Iniciar",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                // --- Botón Escanear ---
                Button(
                    onClick = onScanQrClicked,
                    enabled = !isLoadingScan,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (isLoadingScan) {
                            CircularProgressIndicator(Modifier.size(18.dp), color = LocalContentColor.current, strokeWidth = 2.dp)
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.qr_code_scanner_24px),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text("Scan", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}