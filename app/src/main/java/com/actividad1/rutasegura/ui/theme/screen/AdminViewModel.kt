package com.actividad1.rutasegura.ui.theme.screen


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.actividad1.rutasegura.data.model.Driver
import com.actividad1.rutasegura.data.model.VehicleUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// Estados posibles de la UI de Login
sealed interface LoginUiState {
    object Idle : LoginUiState        // Estado inicial o después de error/logout
    object Loading : LoginUiState    // Cargando durante el intento de login
    object Success : LoginUiState    // Login exitoso
    data class Error(val message: String) : LoginUiState // Error de login
}

class AdminViewModel : ViewModel() {

    // --- Campos de Entrada ---
    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    // --- Estado de la UI ---
    private val _loginState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val loginState: StateFlow<LoginUiState> = _loginState.asStateFlow()

    // --- Datos del Admin (disponibles después del login) ---
    private val _driverInfo = MutableStateFlow<Driver?>(null)
    val driverInfo: StateFlow<Driver?> = _driverInfo.asStateFlow()

    private val _vehicleInfo = MutableStateFlow<VehicleUnit?>(null)
    val vehicleInfo: StateFlow<VehicleUnit?> = _vehicleInfo.asStateFlow()

    // --- Acciones ---
    fun updateUsername(input: String) {
        _username.value = input
        // Si estaba en estado de error, vuelve a Idle al escribir
        if (_loginState.value is LoginUiState.Error) {
            _loginState.value = LoginUiState.Idle
        }
    }

    fun updatePassword(input: String) {
        _password.value = input
        // Si estaba en estado de error, vuelve a Idle al escribir
        if (_loginState.value is LoginUiState.Error) {
            _loginState.value = LoginUiState.Idle
        }
    }

    fun login() {
        // Evita logins múltiples si ya está cargando
        if (_loginState.value is LoginUiState.Loading) return

        viewModelScope.launch {
            _loginState.value = LoginUiState.Loading
            delay(1000) // Simular llamada a red/verificación

            // --- LÓGICA DE LOGIN "FAKE" ---
            if (_username.value == "admin" && _password.value == "1234") {
                // Éxito
                loadAdminData() // Carga los datos fake
                _loginState.value = LoginUiState.Success
                println("Admin Login: Success for user ${_username.value}")
            } else {
                // Error
                _driverInfo.value = null // Limpia datos si falla
                _vehicleInfo.value = null
                _loginState.value = LoginUiState.Error("Usuario o contraseña incorrectos.")
                println("Admin Login: Failed for user ${_username.value}")
                // Opcional: Resetear a Idle después de mostrar el error por un tiempo
                delay(2500)
                if (_loginState.value is LoginUiState.Error) { // Revisa si aún está en error
                    _loginState.value = LoginUiState.Idle
                }
            }
        }
    }

    // Carga datos fake del conductor y vehículo
    private fun loadAdminData() {
        _driverInfo.value = Driver(id = "CHOF-001", name = "Carlos Pérez", licenseNumber = "TX98765A")
        _vehicleInfo.value = VehicleUnit(plate = "CH2-456-B", model = "Toyota Hiace", capacity = 15)
    }

    fun logout() {
        _username.value = ""
        _password.value = ""
        _driverInfo.value = null
        _vehicleInfo.value = null
        _loginState.value = LoginUiState.Idle
        println("Admin Logout")
    }

    // Función para consumir el estado Success después de navegar
    fun consumeLoginSuccessState() {
        if (_loginState.value == LoginUiState.Success) {
            // Puedes dejarlo en Success o volverlo a Idle si prefieres
            // _loginState.value = LoginUiState.Idle
        }
    }
}