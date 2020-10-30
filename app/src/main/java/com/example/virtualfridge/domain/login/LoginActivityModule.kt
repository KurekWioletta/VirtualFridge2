package com.example.virtualfridge.domain.login

import com.example.virtualfridge.domain.base.BaseActivity
import com.example.virtualfridge.domain.login.google.GoogleLoginListener
import dagger.Binds
import dagger.Module

@Module
class LoginActivityProvidesModule {

}

@Module
abstract class LoginActivityBindsModule {

    @Binds
    abstract fun provideBaseActivity(loginActivity: LoginActivity): BaseActivity

    @Binds
    abstract fun provideGoogleLoginListener(loginActivity: LoginActivity): GoogleLoginListener

}