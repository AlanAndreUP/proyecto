// En src/main/java/.../TuApplication.kt (o como la llames)
package com.actividad1.rutasegura // Ajusta tu paquete

import android.app.Application
import android.util.Log
import org.osmdroid.config.Configuration
import org.osmdroid.library.BuildConfig // Importa BuildConfig de osmdroid, no el de tu app

class RutaSegura : Application() {
    override fun onCreate() {
        super.onCreate()
        // Configuración importante para osmdroid
        Configuration.getInstance().load(
            applicationContext,
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(applicationContext)
        )
        // Establece un User-Agent único para evitar ser bloqueado por los servidores de tiles
        Configuration.getInstance().userAgentValue = BuildConfig.LIBRARY_PACKAGE_NAME // Usa el de osmdroid
        Log.i("OSM Droid Config", "User Agent: ${Configuration.getInstance().userAgentValue}")
        Log.i("OSM Droid Config", "OSM Cache Dir: ${Configuration.getInstance().osmdroidTileCache}")

    }
}