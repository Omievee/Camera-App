package com.itsovertime.overtimecamera.play.events

import android.content.Context
import android.icu.text.SimpleDateFormat
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import com.itsovertime.overtimecamera.play.R
import com.itsovertime.overtimecamera.play.model.Event
import kotlinx.android.synthetic.main.events_item_view.view.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*


class EventsView(context: Context, attributeSet: AttributeSet? = null) :
    ConstraintLayout(context, attributeSet),
    View.OnClickListener {

    var listener: EventsClickListener? = null
    var event: Event? = null

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.view -> listener?.onEventSelected(event ?: return)
        }
    }

    init {
        View.inflate(context, R.layout.events_item_view, this)
        view.setOnClickListener(this)
        layoutParams = MarginLayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    fun bind(event: Event) {
        this.event = event
        name.text = event.name
        location.text = "${event.address}"

        val eventDate = event.starts_at
        val sdf = SimpleDateFormat("MM-dd")
        val d = (SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")).parse(
            eventDate?.replace(
                "Z$",
                "+0000"
            )
        )
        sdf.format(d)
        date.text = d.toString()
        date.apply {
            alpha = .6F
        }
        location.apply {
            alpha = .6F
        }
    }
}