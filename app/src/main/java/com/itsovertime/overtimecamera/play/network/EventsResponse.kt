package com.itsovertime.overtimecamera.play.network

import com.itsovertime.overtimecamera.play.model.Event

data class EventsResponse(val events: List<Event> = emptyList())
