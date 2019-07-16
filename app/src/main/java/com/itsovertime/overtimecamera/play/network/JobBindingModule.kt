package com.itsovertime.overtimecamera.play.network

import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import javax.inject.Scope
import javax.inject.Singleton

@Module
abstract class JobBindingModule {
    @ServiceScoped
    @ContributesAndroidInjector
    internal abstract fun provideService(): NetworkSchedulerService
}

@Scope
@MustBeDocumented
@Retention(value = AnnotationRetention.RUNTIME)
annotation class ServiceScoped

