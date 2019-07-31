package com.itsovertime.overtimecamera.play.events

import com.itsovertime.overtimecamera.play.model.Event

interface EventsClickListener {

    fun onEventSelected(event: Event)
}