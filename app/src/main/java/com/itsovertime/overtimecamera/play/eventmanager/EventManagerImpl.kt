package com.itsovertime.overtimecamera.play.eventmanager

import android.annotation.SuppressLint
import com.itsovertime.overtimecamera.play.application.OTApplication
import com.itsovertime.overtimecamera.play.db.AppDatabase
import com.itsovertime.overtimecamera.play.model.Event
import com.itsovertime.overtimecamera.play.network.Api
import com.itsovertime.overtimecamera.play.network.EventsResponse
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.*

class EventManagerImpl(val api: Api, val context: OTApplication) : EventManager {

    var db = AppDatabase.getAppDataBase(context = context)
   // private var eventDao = db?.eventDao()
    @SuppressLint("CheckResult")
    override fun saveEventsToDB(events: List<Event>) {
//        Single.fromCallable {
//            with(eventDao) {
//                this?.saveEventData(event = events)
//            }
//        }.subscribeOn(Schedulers.io())
//            .observeOn(AndroidSchedulers.mainThread())
//            .subscribe({
//
//            }, {
//                it.printStackTrace()
//            })
    }

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
            .doOnSuccess {
                saveEventsToDB(it?.events ?: emptyList())
            }
    }
}