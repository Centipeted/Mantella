package hu.bme.aut.mantella

import android.app.Application
import hu.bme.aut.mantella.di.dataModule
import hu.bme.aut.mantella.di.networkModule
import hu.bme.aut.mantella.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@App)
            modules(
                dataModule,
                networkModule,
                viewModelModule
            )
        }
    }
}
