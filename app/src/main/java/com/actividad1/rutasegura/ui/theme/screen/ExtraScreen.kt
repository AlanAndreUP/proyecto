package com.actividad1.rutasegura.ui.theme.screen

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun RetryButton(onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.padding(top = 16.dp)) {
        Text("Reintentar permisos")
    }
}

@Composable
fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun PermissionRationaleDialog(onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Permisos requeridos") },
        text = { Text("La app necesita acceso a la cámara y ubicación para funcionar correctamente.") },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Entendido")
            }
        }
    )
}

@Composable
fun PermissionsDeniedScreen(permanently: Boolean) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            if (permanently)
                "Los permisos fueron denegados permanentemente. Por favor, actívalos desde Ajustes."
            else
                "Los permisos son necesarios para usar esta función."
        )
        if (permanently) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }) {
                Text("Ir a Ajustes")
            }
        }
    }
}
