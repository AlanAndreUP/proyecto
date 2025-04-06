package com.actividad1.rutasegura.ui.theme.screen


import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.actividad1.rutasegura.R // Necesitarás un layout
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScannerActivity : ComponentActivity() {

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var previewView: PreviewView
    private var barcodeScanner: BarcodeScanner? = null
    private var cameraProvider: ProcessCameraProvider? = null // Para desvincular al final

    companion object {
        const val SCAN_RESULT = "SCAN_RESULT_DATA" // Key para devolver el resultado
    }

    // Lanzador para pedir permiso de cámara
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Log.i("ScannerActivity", "Permiso de cámara concedido")
                startCamera()
            } else {
                Log.e("ScannerActivity", "Permiso de cámara DENEGADO")
                Toast.makeText(this, "Permiso de cámara necesario", Toast.LENGTH_SHORT).show()
                setResult(Activity.RESULT_CANCELED) // Indica cancelación por permiso
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Necesitas un layout simple con un PreviewView, ej. layout/activity_scanner.xml
        setContentView(R.layout.activity_scanner) // Asegúrate que este layout exista
        previewView = findViewById(R.id.previewView) // Asegúrate que el ID coincida

        cameraExecutor = Executors.newSingleThreadExecutor() // Executor para análisis de imagen

        // Solicitar permiso o iniciar cámara
        checkCameraPermission()
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permiso ya concedido
                startCamera()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                // Explicar por qué se necesita el permiso (opcional, podrías mostrar un diálogo)
                Log.w("ScannerActivity", "Se debería mostrar explicación para permiso de cámara")
                requestPermissionLauncher.launch(Manifest.permission.CAMERA) // Pedir permiso
            }
            else -> {
                // Pedir permiso directamente
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }


    private fun startCamera() {
        // Configura el scanner de ML Kit (solo para QR en este caso)
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        barcodeScanner = BarcodeScanning.getClient(options)

        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindPreviewAndAnalysis(cameraProvider!!) // El !! es seguro aquí por el listener
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindPreviewAndAnalysis(cameraProvider: ProcessCameraProvider) {
        try {
            // Desvincular usos anteriores
            cameraProvider.unbindAll()

            // Configurar Preview Usecase
            val preview: Preview = Preview.Builder().build()
            preview.setSurfaceProvider(previewView.surfaceProvider)

            // Configurar ImageAnalysis Usecase
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalyzer.setAnalyzer(cameraExecutor, QRCodeAnalyzer { qrContent ->
                // --- Código QR detectado ---
                Log.i("ScannerActivity", "QR Detectado: $qrContent")
                // Detener análisis y cámara antes de devolver resultado
                cameraProvider.unbindAll() // Desvincula para detener cámara/análisis
                barcodeScanner?.close() // Libera recursos de ML Kit
                cameraExecutor.shutdown() // Apaga el executor

                // Devolver resultado a la Activity que llamó
                val resultIntent = Intent().apply {
                    putExtra(SCAN_RESULT, qrContent)
                }
                setResult(Activity.RESULT_OK, resultIntent)
                finish() // Cierra esta activity
            })

            // Seleccionar cámara trasera
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()

            // Vincular los use cases al ciclo de vida
            cameraProvider.bindToLifecycle(
                this as LifecycleOwner, // Activity implementa LifecycleOwner
                cameraSelector,
                imageAnalyzer,
                preview
            )
            Log.d("ScannerActivity", "CameraX vinculado correctamente.")

        } catch (e: Exception) {
            Log.e("ScannerActivity", "Error al vincular CameraX: ${e.message}", e)
            Toast.makeText(this, "Error al iniciar cámara", Toast.LENGTH_SHORT).show()
            setResult(Activity.RESULT_CANCELED) // Indica error
            finish()
        }
    }

    // Clase interna o externa para analizar la imagen
    private class QRCodeAnalyzer(private val onQrCodeDetected: (String) -> Unit) : ImageAnalysis.Analyzer {
        @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                val scanner = BarcodeScanning.getClient() // O usar el scanner configurado para QR

                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        // Buscar el primer valor no vacío
                        val detectedValue = barcodes.firstNotNullOfOrNull { it.rawValue }

                        if (detectedValue != null) {
                            Log.d("QRCodeAnalyzer", "Valor detectado: $detectedValue")
                            onQrCodeDetected(detectedValue) // Llama al callback con el contenido
                        } else {
                            // Log.v("QRCodeAnalyzer", "No se detectó valor en esta imagen") // Muy verboso
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("QRCodeAnalyzer", "Error en ML Kit Barcode Scanner: ${e.message}", e)
                    }
                    .addOnCompleteListener {
                        // Siempre cerrar imageProxy para que ImageAnalysis reciba la siguiente imagen
                        imageProxy.close()
                    }
            } else {
                // Cerrar si no hay imagen válida
                imageProxy.close()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Asegurarse de liberar recursos
        cameraProvider?.unbindAll()
        barcodeScanner?.close()
        if (!cameraExecutor.isShutdown) {
            cameraExecutor.shutdown()
        }
    }

    // Sobreescribir onBackPressed para devolver RESULT_CANCELED
    override fun onBackPressed() {
        super.onBackPressed()
        setResult(Activity.RESULT_CANCELED)
        // No es necesario finish() aquí, super.onBackPressed() ya lo hace
    }
}