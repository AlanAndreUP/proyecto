package com.actividad1.rutasegura.data.repository

import android.content.Context // Necesario para el constructor
import com.actividad1.rutasegura.data.model.ScanResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.random.Random

// Interfaz (sin cambios)
interface CameraRepository {
    suspend fun scanQRCode(): ScanResult
}

// Implementación SIN HILT
class CameraRepositoryImpl(
    private val context: Context // Recibe el contexto vía constructor
): CameraRepository {

    // !!! ESTA ES UNA IMPLEMENTACIÓN FALSA / PLACEHOLDER !!!
    override suspend fun scanQRCode(): ScanResult = withContext(Dispatchers.IO) {
        delay(1500) // Simula el tiempo de escaneo
        // Simula un resultado aleatorio
        if (Random.nextBoolean()) {
            ScanResult.Success("BUS-${Random.nextInt(100, 999)}")
        } else {
            if(Random.nextBoolean()) ScanResult.Cancelled else ScanResult.Error
        }
    }
    // Si usas CameraX, necesitarás el 'context' aquí.
}