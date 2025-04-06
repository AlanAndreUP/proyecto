package com.actividad1.rutasegura.ui.theme.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun LoginScreen(
    viewModel: AdminViewModel,
    onLoginSuccess: () -> Unit // Callback para navegar
) {
    val username by viewModel.username.collectAsState()
    val password by viewModel.password.collectAsState()
    val loginState by viewModel.loginState.collectAsState()

    // Observa el estado de login y navega automáticamente si es Success
    LaunchedEffect(loginState) {
        if (loginState == LoginUiState.Success) {
            onLoginSuccess()
            viewModel.consumeLoginSuccessState() // Informa al VM que la navegación se manejó
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 16.dp), // Padding
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center // Centrar verticalmente
    ) {
        Text("Acceso Administrador", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(48.dp))

        // Campo de Usuario
        OutlinedTextField(
            value = username,
            onValueChange = viewModel::updateUsername,
            label = { Text("Usuario") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            enabled = loginState !is LoginUiState.Loading // Deshabilitar mientras carga
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Campo de Contraseña
        OutlinedTextField(
            value = password,
            onValueChange = viewModel::updatePassword,
            label = { Text("Contraseña") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(), // Ocultar contraseña
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            enabled = loginState !is LoginUiState.Loading // Deshabilitar mientras carga
        )
        Spacer(modifier = Modifier.height(8.dp)) // Espacio antes del error

        // Mostrar mensaje de error
        if (loginState is LoginUiState.Error) {
            Text(
                text = (loginState as LoginUiState.Error).message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp).align(Alignment.Start)
            )
        } else {
            // Placeholder para mantener el espacio y evitar saltos
            Spacer(modifier = Modifier.height(MaterialTheme.typography.bodySmall.lineHeight.value.dp + 8.dp))
        }


        Spacer(modifier = Modifier.height(16.dp)) // Espacio antes del botón

        // Botón de Login
        Button(
            onClick = { viewModel.login() }, // Llama a la función del ViewModel
            enabled = loginState !is LoginUiState.Loading, // Deshabilitar mientras carga
            modifier = Modifier.fillMaxWidth().height(48.dp) // Tamaño estándar
        ) {
            if (loginState is LoginUiState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary, // Color del indicador
                    strokeWidth = 2.dp
                )
            } else {
                Text("Iniciar Sesión")
            }
        }
    }
}