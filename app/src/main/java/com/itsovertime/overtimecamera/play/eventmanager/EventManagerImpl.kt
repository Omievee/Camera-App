package com.itsovertime.overtimecamera.play.eventmanager

import com.itsovertime.overtimecamera.play.network.Api
import com.itsovertime.overtimecamera.play.network.EventsResponse
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.*

class EventManagerImpl(val api: Api) : EventManager {


    private var eventResponse: EventsResponse? = null

    override fun getEvents(): Single<EventsResponse?> {
        return when (eventResponse) {
            null -> {
                api
                        .getEventData(Date(-60 * 60 * 24))
                        .doOnSuccess {
                            eventResponse = it
                        }
                        .doOnError {
                            it.printStackTrace()
                        }
            }
            else -> Single.just(eventResponse)
        }.subscribeOn(Schedulers.single())
                .observeOn(AndroidSchedulers.mainThread())


    }
}