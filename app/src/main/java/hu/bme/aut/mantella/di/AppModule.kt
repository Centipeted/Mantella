package hu.bme.aut.mantella.di

import MainScreenViewModel
import hu.bme.aut.mantella.data.NextcloudRepo
import hu.bme.aut.mantella.data.SharedPrefsCredentialStore
import hu.bme.aut.mantella.screens.loginScreen.AuthInterceptor
import hu.bme.aut.mantella.screens.loginScreen.CredentialStore
import hu.bme.aut.mantella.screens.loginScreen.LoginScreenViewModel
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

val dataModule = module {
    single<CredentialStore> { SharedPrefsCredentialStore(androidContext()) }
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
    viewModel { MainScreenViewModel(get()) }
}