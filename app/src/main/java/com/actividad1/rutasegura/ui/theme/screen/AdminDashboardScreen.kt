package com.actividad1.rutasegura.ui.theme.screen


import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp // Icono de logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.actividad1.rutasegura.data.model.Driver
import com.actividad1.rutasegura.data.model.VehicleUnit

@OptIn(ExperimentalMaterial3Api::class) // Para TopAppBar
@Composable
fun AdminDashboardScreen(
    viewModel: AdminViewModel,
    onLogout: () -> Unit // Callback para navegar al hacer logout
) {
    val driver by viewModel.driverInfo.collectAsState()
    val vehicle by viewModel.vehicleInfo.collectAsState()

    // Si por alguna razón los datos son null (ej. después de matar proceso),
    // fuerza el logout para volver al login.
    LaunchedEffect(driver, vehicle) {
        if (driver == null || vehicle == null) {
            println("AdminDashboard: Datos no disponibles, forzando logout.")
            // Descomentar si quieres forzar logout automático si los datos desaparecen
            // onLogout()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Panel del Conductor") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary, // Color primario
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = {
                        viewModel.logout() // Limpia estado en VM
                        onLogout()        // Navega fuera
                    }) {
                        Icon(
                            imageVector = Icons.Filled.ExitToApp,
                            contentDescription = "Cerrar Sesión"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        // Comprueba si los datos están disponibles antes de mostrarlos
        if (driver != null && vehicle != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues) // Padding del Scaffold
                    .padding(16.dp), // Padding adicional para contenido
                verticalArrangement = Arrangement.spacedBy(16.dp), // Espacio entre elementos
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Bienvenido, ${driver!!.name}", // !! es seguro por la comprobación if
                    style = MaterialTheme.typography.headlineSmall
                )

                DriverInfoCard(driver = driver!!)
                VehicleInfoCard(vehicle = vehicle!!)

                // Aquí puedes agregar más elementos para la pantalla de admin en el futuro
                // Ej: Botón para iniciar/detener seguimiento, ver logs, etc.

            }
        } else {
            // Muestra un indicador de carga o mensaje si los datos aún no están listos
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Cargando datos...")
                }
            }
        }
    }
}

// Composable para mostrar la info del conductor
@Composable
private fun DriverInfoCard(driver: Driver) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp) // Sombra sutil
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp) // Espacio interno
        ) {
            Text("Datos del Conductor", style = MaterialTheme.typography.titleMedium)
            Divider() // Separador
            Text("ID: ${driver.id}")
            Text("Nombre: ${driver.name}")
            Text("Licencia: ${driver.licenseNumber}")
        }
    }
}

// Composable para mostrar la info del vehículo
@Composable
private fun VehicleInfoCard(vehicle: VehicleUnit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Datos de la Unidad", style = MaterialTheme.typography.titleMedium)
            Divider()
            Text("Placa: ${vehicle.plate}")
            Text("Modelo: ${vehicle.model}")
            Text("Capacidad: ${vehicle.capacity} pasajeros")
        }
    }
}