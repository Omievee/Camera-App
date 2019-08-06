package com.itsovertime.overtimecamera.play.events

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager

import com.itsovertime.overtimecamera.play.R
import com.itsovertime.overtimecamera.play.itemsame.BasicDiffCallback
import com.itsovertime.overtimecamera.play.itemsame.ItemSame
import com.itsovertime.overtimecamera.play.model.Event
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_events.*
import javax.inject.Inject


private const val LIST = "param1"

class EventsFragment : Fragment(), EventsImpl {

    override fun updateAdapter(eventsList: List<Event>) {
        val old = adapter.data?.list ?: emptyList()
        val newD = mutableListOf<EventsPresentation>()
        newD.add(EventsPresentation(eventsList = param1 as List<Event>))
        adapter.data = EventsViewData(newD, DiffUtil.calculateDiff(BasicDiffCallback(old, newD)))
    }

    private var param1: List<Event>? = null

    @Inject
    lateinit var presenter: EventsPresenter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getParcelableArrayList(LIST)
        }
    }

    val listener: EventsClickListener = object : EventsClickListener {
        override fun onEventSelected(event: Event) {
            println("event is : ${event.name}")
        }
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_events, container, false)
    }


    private var adapter: EventsAdapter = EventsAdapter(listener = listener)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        println("Size? ${param1?.size}")
        presenter.onCreate(param1)
        eventsRecycler.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        eventsRecycler.adapter = adapter
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)

    }

    companion object {
        @JvmStatic
        fun newInstance(eventsList: List<Event>) =
                EventsFragment().apply {
                    arguments = Bundle().apply {
                        putParcelableArrayList(LIST, eventsList as java.util.ArrayList<out Parcelable>)
                    }
                }
    }
}

class EventsViewData(
        val list: List<EventsPresentation>,
        val diffResult: DiffUtil.DiffResult
)

data class EventsPresentation(
        val eventsList: List<Event>
) : ItemSame<EventsPresentation> {

    override fun sameAs(same: EventsPresentation): Boolean {
        return equals(same)
    }

    override fun contentsSameAs(same: EventsPresentation): Boolean {
        return hashCode() == same.hashCode()
    }
}
