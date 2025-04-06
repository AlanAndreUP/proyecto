package com.actividad1.rutasegura.di



import android.content.Context
import com.actividad1.rutasegura.data.repository.*
import com.actividad1.rutasegura.data.sensors.LocationProviderWrapper
import com.actividad1.rutasegura.data.simulation.SimulationEngine
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFusedLocationProviderClient(@ApplicationContext context: Context): FusedLocationProviderClient {
        return LocationServices.getFusedLocationProviderClient(context)
    }

    @Provides
    @Singleton
    fun provideLocationProviderWrapper(fusedLocationProviderClient: FusedLocationProviderClient): LocationProviderWrapper {
        // Aquí encapsularías la lógica real de FusedLocationProviderClient
        return LocationProviderWrapper(fusedLocationProviderClient)
    }

    @Provides
    @Singleton
    fun provideSimulationEngine(routeRepository: RouteRepository): SimulationEngine {
        // Proporciona el motor de simulación (podría necesitar Dispatchers.Default)
        return SimulationEngine(routeRepository, Dispatchers.Default)
    }

    // Repositories
    @Provides
    @Singleton
    fun provideLocationRepository(wrapper: LocationProviderWrapper): LocationRepository {
        return LocationRepositoryImpl(wrapper, Dispatchers.IO)
    }

    @Provides
    @Singleton
    fun provideRouteRepository(): RouteRepository {
        // Implementación que carga rutas (hardcoded, desde assets, etc.)
        return RouteRepositoryImpl()
    }

    @Provides
    @Singleton
    fun provideSimulationRepository(engine: SimulationEngine): SimulationRepository {
        return SimulationRepositoryImpl(engine)
    }

    @Provides
    @Singleton
    fun provideCameraRepository(@ApplicationContext context: Context): CameraRepository {
        // Implementación placeholder - requiere CameraX/MLKit reales
        return CameraRepositoryImpl(context)
    }
}

// Podrías tener un ViewModelModule específico si es necesario,
// pero Hilt maneja la inyección en ViewModels automáticamente con @HiltViewModel