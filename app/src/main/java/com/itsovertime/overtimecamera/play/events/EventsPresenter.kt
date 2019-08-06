package com.itsovertime.overtimecamera.play.events

import com.itsovertime.overtimecamera.play.model.Event

class EventsPresenter(val view: EventsFragment) {

    fun onCreate(param1: List<Event>?) {
        if (!param1.isNullOrEmpty()) {
            view.updateAdapter(param1)
        }

    }
}