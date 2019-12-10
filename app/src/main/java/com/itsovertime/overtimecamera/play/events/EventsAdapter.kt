package com.itsovertime.overtimecamera.play.events

import android.app.Activity
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.itsovertime.overtimecamera.play.baseviewholder.BaseViewHolder
import com.itsovertime.overtimecamera.play.model.Event

class EventsAdapter(
    val list: List<Event>?,
    val listener: EventsClickListener
) : RecyclerView.Adapter<BaseViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return BaseViewHolder(EventsView(parent.context).apply {
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        })
    }

    override fun getItemCount(): Int {
        return list?.size ?: 0
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        ((holder.itemView) as EventsView).bind(list?.get(position) ?: return)
        (holder.itemView).listener = listener
    }
}