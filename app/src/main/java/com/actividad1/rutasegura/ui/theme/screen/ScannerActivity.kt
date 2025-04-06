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
import com.actividad1.rutasegura.R
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class ScannerActivity : ComponentActivity() {

    companion object {
        const val SCAN_RESULT = "SCAN_RESULT_DATA"
        private const val TAG = "ScannerActivity"
    }

    private lateinit var previewView: PreviewView
    private lateinit var cameraExecutor: ExecutorService
    private var cameraProvider: ProcessCameraProvider? = null

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Log.i(TAG, "Permiso de cámara concedido")
                startCamera()
            } else {
                Log.e(TAG, "Permiso de cámara denegado")
                showToast("Permiso de cámara necesario")
                cancelAndFinish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanner)

        previewView = findViewById(R.id.previewView)
        cameraExecutor = Executors.newSingleThreadExecutor()

        checkCameraPermission()
    }

    fun vibrateOnScan() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(VibratorManager::class.java)
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(200)
            }
        }
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                startCamera()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                showToast("Se requiere permiso para escanear códigos QR")
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            else -> requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        val barcodeScanner = BarcodeScanning.getClient(options)

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases(barcodeScanner)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases(barcodeScanner: com.google.mlkit.vision.barcode.BarcodeScanner) {
        try {
            cameraProvider?.unbindAll()

            val preview = Preview.Builder().build().apply {
                setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .apply {
                    setAnalyzer(cameraExecutor, QRCodeAnalyzer(this@ScannerActivity, barcodeScanner) { qrContent ->
                        handleQrResult(qrContent)
                    })
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            cameraProvider?.bindToLifecycle(
                this as LifecycleOwner,
                cameraSelector,
                preview,
                imageAnalyzer
            )

            Log.d(TAG, "Camera vinculada correctamente")
        } catch (e: Exception) {
            Log.e(TAG, "Error al vincular CameraX", e)
            showToast("Error al iniciar la cámara")
            cancelAndFinish()
        }
    }

    private fun handleQrResult(content: String) {
        Log.i(TAG, "QR Detectado: $content")
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()

        val resultIntent = Intent().apply {
            putExtra(SCAN_RESULT, content)
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    private class QRCodeAnalyzer(
        private val context: ScannerActivity,
        private val scanner: com.google.mlkit.vision.barcode.BarcodeScanner,
        private val onQrCodeDetected: (String) -> Unit
    ) : ImageAnalysis.Analyzer {

        @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image ?: run {
                imageProxy.close()
                return
            }

            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    barcodes.firstOrNull { it.format == Barcode.FORMAT_QR_CODE && !it.rawValue.isNullOrBlank() }
                        ?.rawValue?.let { qrValue ->
                            context.vibrateOnScan() // ✅ vibración al escanear
                            onQrCodeDetected(qrValue)
                        }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error en ML Kit Barcode Scanner", e)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun cancelAndFinish() {
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraProvider?.unbindAll()
        cameraExecutor.takeIf { !it.isShutdown }?.shutdown()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        cancelAndFinish()
    }
}
