package com.actividad1.rutasegura// Ajusta el paquete si es necesario

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.actividad1.rutasegura.ui.theme.screen.MainScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Lanzador para solicitar permisos múltiples
    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            // Aquí puedes verificar si los permisos fueron concedidos,
            // aunque la lógica principal se basará en el estado observable.
            // Ejemplo: permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Componente que maneja la lógica de permisos
                    PermissionHandler(
                        permissions = listOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.CAMERA
                        ),
                        onPermissionsGranted = {
                            // Cuando los permisos están listos, muestra la pantalla principal
                            MainScreen()
                        },
                        rationaleContent = { showRationale, onRationaleReply ->
                            // Muestra explicación si es necesario (opcional pero recomendado)
                            if (showRationale) {
                                PermissionRationaleDialog(onConfirm = { onRationaleReply(true) })
                            }
                        },
                        onPermissionsDenied = { permanently ->
                            // Muestra mensaje si los permisos son denegados
                            PermissionsDeniedScreen(permanently = permanently)
                        },
                        permissionRequester = { permissionsToRequest ->
                            // Lanza la solicitud de permisos usando el lanzador
                            requestMultiplePermissions.launch(permissionsToRequest.toTypedArray())
                        }
                    )
                }
            }
        }
    }



// --- Composables para manejar permisos (pueden ir a un archivo `PermissionUtils.kt` en `ui` o `util`) ---

@Composable
fun PermissionHandler(
    permissions: List<String>,
    onPermissionsGranted: @Composable () -> Unit, // Contenido a mostrar si se conceden
    rationaleContent: @Composable (showRationale: Boolean, onRationaleReply: (Boolean) -> Unit) -> Unit, // Para mostrar explicación
    onPermissionsDenied: @Composable (permanently: Boolean) -> Unit, // Contenido si se deniegan
    permissionRequester: (List<String>) -> Unit // Función que lanza la solicitud
) {
    var permissionState by remember { mutableStateOf<PermissionStatus>(PermissionStatus.Unknown) }
    var showRationale by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

    // Observador del ciclo de vida para verificar permisos al reanudar la app
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Verificar permisos al volver a la app
                permissionState = checkAppPermissions(permissions, ComponentActivity()) // Necesitas Activity aquí
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Determinar estado inicial
    LaunchedEffect(Unit) {
        permissionState = checkAppPermissions(permissions, ComponentActivity()) // Necesitas Activity aquí
    }

    when (permissionState) {
        PermissionStatus.Granted -> {
            onPermissionsGranted() // Muestra el contenido principal
        }
        PermissionStatus.Denied -> {
            // Muestra contenido indicando que los permisos fueron denegados
            onPermissionsDenied(false)
            // Ofrecer botón para volver a pedir (si no son permanentes)
            Button(onClick = { permissionState = PermissionStatus.ShouldRequest }) {
                Text("Reintentar Permisos")
            }

        }
        PermissionStatus.PermanentlyDenied -> {
            // Muestra contenido indicando que debe ir a ajustes
            onPermissionsDenied(true)
        }
        PermissionStatus.ShouldShowRationale -> {
            // Mostrar explicación antes de pedir permisos
            rationaleContent(true) { confirmed ->
                if (confirmed) {
                    permissionState = PermissionStatus.ShouldRequest // Proceder a pedir si confirma
                } else {
                    permissionState = PermissionStatus.Denied // Marcar como denegado si cancela
                }
            }
        }
        PermissionStatus.ShouldRequest -> {
            // Lanzar la solicitud de permisos
            LaunchedEffect(Unit) {
                permissionRequester(permissions)
                // El estado se actualizará en ON_RESUME o a través del callback del lanzador
            }
            // Mostrar un indicador de carga o pantalla de espera mientras se piden
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }
        PermissionStatus.Unknown -> {
            // Estado inicial o indeterminado, mostrar carga
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }
    }
}

// Necesitas una función para verificar los permisos actuales (requiere Activity o Context)
// Esta es una implementación SIMPLIFICADA. Una real debería usar checkSelfPermission
// y shouldShowRequestPermissionRationale. ¡IMPLANTAR CORRECTAMENTE!
private fun checkAppPermissions(permissions: List<String>, activity: ComponentActivity): PermissionStatus {
    // --- ¡IMPLEMENTACIÓN SIMPLIFICADA! ---
    // Una implementación real necesita usar checkSelfPermission y shouldShowRequestPermissionRationale
    // Esta es solo una placeholder para la lógica del Handler.
    return PermissionStatus.ShouldRequest // Asume que siempre hay que pedir para este ejemplo
}

// Estados posibles de los permisos
enum class PermissionStatus {
    Unknown, Granted, Denied, PermanentlyDenied, ShouldShowRationale, ShouldRequest
}

// Diálogo simple para la explicación (rationale)
@Composable
fun PermissionRationaleDialog(onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = { /* No hacer nada al descartar fuera */ },
        title = { Text("Permisos Necesarios") },
        text = { Text("Esta aplicación necesita permisos de Ubicación y Cámara para funcionar correctamente.") },
        confirmButton = {
            Button(onClick = onConfirm) { Text("Entendido") }
        }
    )
}

// Pantalla simple para permisos denegados
@Composable
fun PermissionsDeniedScreen(permanently: Boolean) {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Text(
            if (permanently) "Los permisos fueron denegados permanentemente. Por favor, actívelos desde los ajustes de la aplicación."
            else "Los permisos son necesarios para usar esta función."
        )
        // Podrías añadir un botón para ir a ajustes si permanently es true
    }
}