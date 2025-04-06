package com.actividad1.rutasegura.data.repository


import android.content.Context
import com.actividad1.rutasegura.data.model.ScanResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

// Interfaz
interface CameraRepository {
    suspend fun scanQRCode(): ScanResult // Simplificado
}

// Implementación (Placeholder)
@Singleton
class CameraRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context // Inyecta contexto si es necesario para CameraX
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
}