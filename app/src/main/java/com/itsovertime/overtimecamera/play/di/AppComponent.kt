package com.itsovertime.overtimecamera.play.di

import com.itsovertime.overtimecamera.play.application.OTApplication
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjectionModule
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import dagger.android.support.DaggerApplication
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AndroidInjectionModule::class,
        AppBindingModule::class,
        AndroidSupportInjectionModule::class,
        AppModule::class]
)
interface AppComponent : AndroidInjector<DaggerApplication> {

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: OTApplication): Builder

        fun build(): AppComponent
    }

    fun inject(app: OTApplication)
}
