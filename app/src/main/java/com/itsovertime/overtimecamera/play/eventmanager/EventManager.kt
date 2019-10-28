package com.itsovertime.overtimecamera.play.eventmanager

import com.itsovertime.overtimecamera.play.model.Event
import com.itsovertime.overtimecamera.play.network.EventsResponse
import io.reactivex.Single

interface EventManager {
    fun getEvents(): Single<EventsResponse?>
    fun saveEventsToDB(events:List<Event>)

}