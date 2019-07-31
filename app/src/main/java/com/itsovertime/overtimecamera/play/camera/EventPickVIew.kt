package com.itsovertime.overtimecamera.play.camera

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import com.itsovertime.overtimecamera.play.R
import com.itsovertime.overtimecamera.play.model.Event
import kotlinx.android.synthetic.main.event_view.view.*

class EventPickVIew(context: Context, attributeSet: AttributeSet? = null) : ConstraintLayout(context, attributeSet), View.OnClickListener {
    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.mainView -> {
                displayEventsFragment()
            }
        }
    }

    private fun displayEventsFragment() {

    }


    init {
        View.inflate(context, R.layout.event_view, this)
        mainView.setOnClickListener(this)
    }

    fun bind(event: Event) {


    }
}