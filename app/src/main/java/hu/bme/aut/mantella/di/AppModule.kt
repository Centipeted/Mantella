package hu.bme.aut.mantella.di

import hu.bme.aut.mantella.data.AuthInterceptor
import hu.bme.aut.mantella.data.CredentialStore
import hu.bme.aut.mantella.data.NextcloudRepo
import hu.bme.aut.mantella.data.SecureCredentialStore
import hu.bme.aut.mantella.screens.loginScreen.LoginScreenViewModel
import hu.bme.aut.mantella.screens.mainScreen.MainScreenViewModel
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

val dataModule = module {
    single<CredentialStore> { SecureCredentialStore(androidContext()) }
}

val networkModule = module {
    single {
        OkHttpClient.Builder()
            .callTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(get()))
            .build()
    }
    single { NextcloudRepo(get(), get()) }
}

val viewModelModule = module {
    viewModel { LoginScreenViewModel(get()) }
    viewModel { MainScreenViewModel(get(), get()) }
}