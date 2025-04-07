package com.actividad1.rutasegura.ui.theme.screen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack // Importar icono correcto
import androidx.compose.material3.* // Usar Material3
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.launch
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
private const val TAG = "ScannerScreen"

@Composable
fun ScannerScreen(
    onScanResult: (String?) -> Unit // Callback para devolver el resultado (String) o cancelación (null)
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope() // Para lanzar tareas asíncronas

    // Estado para manejar el permiso de cámara
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Lanzador para solicitar permiso
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                Log.i(TAG, "Permiso de cámara concedido")
                hasCameraPermission = true
            } else {
                Log.e(TAG, "Permiso de cámara denegado")
                Toast.makeText(context, "Permiso de cámara necesario", Toast.LENGTH_SHORT).show()
                onScanResult(null) // Indicar cancelación si se niega el permiso
            }
        }
    )

    // Solicitar permiso si no se tiene
    LaunchedEffect(key1 = true) { // Se ejecuta una vez al entrar al composable
        if (!hasCameraPermission) {
            Log.d(TAG, "Solicitando permiso de cámara...")
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // --- UI y Lógica de Cámara ---
    Box(modifier = Modifier.fillMaxSize()) {
        if (hasCameraPermission) {
            CameraPreview(
                context = context,
                lifecycleOwner = lifecycleOwner,
                onQrCodeDetected = { qrContent ->
                    // Asegurarse que se ejecuta en el hilo principal para la navegación/callback
                    coroutineScope.launch(Dispatchers.Main) {
                        Log.d(TAG, "Código QR detectado: $qrContent")
                        onScanResult(qrContent)
                    }
                },
                onError = {
                    coroutineScope.launch(Dispatchers.Main) {
                        Log.e(TAG, "Error al iniciar la cámara")
                        Toast.makeText(context, "Error al iniciar la cámara", Toast.LENGTH_SHORT).show()
                        onScanResult(null) // Indicar error/cancelación
                    }
                }
            )

            // --- Elementos de UI superpuestos ---
            ScannerOverlay()

        } else {
            // Opcional: Mostrar un mensaje mientras se espera el permiso
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Solicitando permiso de cámara...")
            }
        }

        // Botón para volver atrás (cancelar)
        IconButton(
            onClick = {
                Log.d(TAG, "Acción de volver atrás (cancelación)")
                onScanResult(null) // Llama al callback con null para indicar cancelación
            },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.3f), CircleShape) // Fondo semi-transparente
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack, // Usar icono AutoMirrored
                contentDescription = "Volver",
                tint = Color.White
            )
        }
    }
}

@Composable
private fun CameraPreview(
    context: Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    onQrCodeDetected: (String) -> Unit,
    onError: () -> Unit
) {
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var cameraProvider: ProcessCameraProvider? by remember { mutableStateOf(null) }
    val previewView = remember { PreviewView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        scaleType = PreviewView.ScaleType.FILL_CENTER // Ajustar según necesidad
    }}
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val vibrator = rememberVibrator(context)

    // --- Efecto para manejar el ciclo de vida de la cámara ---
    LaunchedEffect(lifecycleOwner) {
        try {
            Log.d(TAG, "Esperando a que el CameraProvider esté listo...")
            cameraProvider = cameraProviderFuture.await() // Espera a que el provider esté listo
            Log.d(TAG, "CameraProvider listo, vinculando casos de uso")
            bindCameraUseCases(
                cameraProvider!!, // Sabemos que no es null aquí por el await
                previewView,
                lifecycleOwner,
                cameraExecutor,
                vibrator,
                onQrCodeDetected
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener o vincular CameraProvider", e)
            onError()
        }
    }

    // --- Efecto para limpiar recursos ---
    DisposableEffect(Unit) {
        onDispose {
            Log.d(TAG, "Desvinculando cámara y liberando executor")
            cameraProvider?.unbindAll()
            cameraExecutor.shutdown()
        }
    }

    // --- Vista de Android para mostrar el PreviewView ---
    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    )
}

// --- Función para vincular los casos de uso de CameraX ---
private fun bindCameraUseCases(
    cameraProvider: ProcessCameraProvider,
    previewView: PreviewView,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    cameraExecutor: ExecutorService,
    vibrator: Vibrator?,
    onQrCodeDetected: (String) -> Unit
) {
    val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        .build()
    val barcodeScanner = BarcodeScanning.getClient(options)

    val preview = Preview.Builder()
        .build()
        .also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

    val imageAnalyzer = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()
        .also {
            it.setAnalyzer(cameraExecutor, QRCodeAnalyzer(barcodeScanner, vibrator) { qrContent ->
                // Detener análisis y cámara ANTES de notificar el resultado
                cameraProvider.unbindAll() // Importante para evitar múltiples detecciones
                Log.d(TAG, "Escaneo completado, enviando resultado: $qrContent")
                onQrCodeDetected(qrContent)
            })
        }

    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    try {
        // Desvincular antes de volver a vincular
        cameraProvider.unbindAll()

        // Vincular casos de uso a la cámara
        cameraProvider.bindToLifecycle(
            lifecycleOwner, cameraSelector, preview, imageAnalyzer
        )
        Log.d(TAG, "Casos de uso de CameraX vinculados correctamente.")

    } catch (exc: Exception) {
        Log.e(TAG, "Error al vincular casos de uso", exc)
        // Podrías llamar a onError() aquí también si la vinculación falla
    }
}

// --- Analizador de QR ---
private class QRCodeAnalyzer(
    private val scanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    private val vibrator: Vibrator?,
    private val onQrCodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private var isScanning = true // Flag para evitar múltiples llamadas rápidas

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (!isScanning) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            Log.d(TAG, "Analizando imagen para QR...") // Log para indicar el inicio del análisis
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            Log.d(TAG, "Imagen convertida a InputImage con rotación: ${imageProxy.imageInfo.rotationDegrees} grados.")

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    Log.d(TAG, "Proceso de escaneo de código de barras completado.")
                    val qrCode = barcodes.firstOrNull { it.format == Barcode.FORMAT_QR_CODE && !it.rawValue.isNullOrBlank() }
                    if (qrCode != null && isScanning) {
                        isScanning = false
                        Log.d(TAG, "Código QR detectado.")
                        vibrateDevice(vibrator)
                        qrCode.rawValue?.let {
                            Log.d(TAG, "Código QR detectado: ${qrCode.rawValue}")
                            onQrCodeDetected(it)
                        }
                    } else {
                        Log.d(TAG, "No se detectó un código QR válido.")
                    }
                }
                .addOnFailureListener { e ->
                    if (isScanning) { // Solo loguear si aún estábamos escaneando
                        Log.e(TAG, "Error al procesar el código de barras en ML Kit", e)
                    } else {
                        Log.d(TAG, "Escaneo de código QR cancelado antes de completarse.")
                    }
                }
                .addOnCompleteListener {
                    // Cerrar imageProxy si no se llamó a onQrCodeDetected (éxito)
                    if (isScanning) {
                        Log.d(TAG, "Cerrando imageProxy porque no se detectó ningún código QR.")
                        imageProxy.close()
                    } else {
                        Log.d(TAG, "Escaneo exitoso, no es necesario cerrar imageProxy.")
                    }
                }
        } else {
            // Asegurarse de cerrar si no hay mediaImage
            Log.w(TAG, "No se pudo obtener mediaImage, cerrando imageProxy.")
            imageProxy.close()
        }

    }
}
@Composable
private fun ScannerOverlay() {
    val frameSize = 260.dp
    val scrimColor = Color.Black.copy(alpha = 0.6f) // Color semi-transparente

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        val frameLeft = (screenWidth - frameSize) / 2
        val frameTop = (screenHeight - frameSize) / 2 * 0.9f // Ajustar posición vertical ligeramente
        val frameRight = frameLeft + frameSize
        val frameBottom = frameTop + frameSize

        // Scrims (áreas oscurecidas fuera del marco)
        Box(modifier = Modifier.fillMaxSize().background(scrimColor)) // Fondo completo inicial

        // "Agujero" transparente donde va el marco
        Box(
            modifier = Modifier
                .size(frameSize)
                .align(Alignment.Center)
                .offset(y = (frameTop - (screenHeight / 2)) + frameSize / 2 ) // Ajustar offset basado en frameTop
                .background(Color.Transparent) // Área transparente
                .border(2.dp, Color.White) // Borde del marco
        )

        // Texto de instrucciones
        Text(
            text = "Coloque el código QR dentro del recuadro",
            color = Color.White,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp) // Espacio desde abajo
                .width(frameSize) // Ancho similar al marco
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

// --- Funciones de utilidad para vibración ---
@Composable
private fun rememberVibrator(context: Context): Vibrator? {
    return remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?
        }
    }
}

private fun vibrateDevice(vibrator: Vibrator?) {
    vibrator?.let {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            it.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            it.vibrate(200)
        }
    }
}

// Helper para convertir Future a suspend fun (opcional pero útil)
suspend fun <T> ListenableFuture<T>.await(): T {
    return suspendCoroutine { continuation ->
        addListener({
            try {
                continuation.resume(get())
            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }, Dispatchers.Main.asExecutor()) // Ejecutar listener en Main thread
    }
}
