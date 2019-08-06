package com.itsovertime.overtimecamera.play.events

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.itsovertime.overtimecamera.play.baseviewholder.BaseViewHolder

class EventsAdapter(
        val listener: EventsClickListener
) : RecyclerView.Adapter<BaseViewHolder>() {

    var data: EventsViewData? = null
        set(value) {
            field = value
            field?.diffResult?.dispatchUpdatesTo(this)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return BaseViewHolder(EventsView(parent.context).apply {

            layoutParams = ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            )
        })
    }
    override fun getItemCount(): Int {
        return data?.list?.get(0)?.eventsList?.size ?: 0
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        ((holder.itemView) as EventsView).bind(data?.list?.get(0)?.eventsList?.get(position)
                ?: return
        )
        (holder.itemView).listener = listener

    }
}