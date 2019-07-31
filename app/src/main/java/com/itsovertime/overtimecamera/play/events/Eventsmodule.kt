package com.itsovertime.overtimecamera.play.events

import com.itsovertime.overtimecamera.play.di.FragmentScope
import dagger.Module
import dagger.Provides

@Module
class Eventsmodule {

    @Provides
    @FragmentScope
    fun providePresenter(fragment: EventsFragment): EventsPresenter =
            EventsPresenter(fragment)


}
