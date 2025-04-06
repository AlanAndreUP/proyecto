// file: util/ScanQrCodeContract.kt (o donde prefieras)
package com.actividad1.rutasegura.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.actividad1.rutasegura.ui.theme.screen.ScannerActivity // Importa tu ScannerActivity

class ScanQrCodeContract : ActivityResultContract<Unit?, String?>() { // Input Unit?, Output String?

    override fun createIntent(context: Context, input: Unit?): Intent {
        // Simplemente crea el Intent para iniciar ScannerActivity
        return Intent(context, ScannerActivity::class.java)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): String? {
        // Parsea el resultado devuelto por ScannerActivity
        return if (resultCode == Activity.RESULT_OK) {
            intent?.getStringExtra(ScannerActivity.SCAN_RESULT) // Devuelve el string o null
        } else {
            null // Devuelve null si fue cancelado o hubo error
        }
    }
}