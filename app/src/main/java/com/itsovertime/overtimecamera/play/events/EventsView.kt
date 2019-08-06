package com.itsovertime.overtimecamera.play.events

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import com.itsovertime.overtimecamera.play.R
import com.itsovertime.overtimecamera.play.model.Event
import kotlinx.android.synthetic.main.events_item_view.view.*

class EventsView(context: Context, attributeSet: AttributeSet? = null) : ConstraintLayout(context, attributeSet),
        View.OnClickListener {

    var listener: EventsClickListener? = null
    var event: Event? = null

    override fun onClick(v: View?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    init {
        View.inflate(context, R.layout.events_item_view, this)
        layoutParams = MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    fun bind(event: Event) {
        println("EVENT VIEW BIND")
        this.event = event
        name.text = event.name
//        location.text = "${event.city}"


    }
}