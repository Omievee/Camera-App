package com.itsovertime.overtimecamera.play.camera

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.itsovertime.overtimecamera.play.R
import kotlinx.android.synthetic.main.event_view.view.*

class EventPickVIew(context: Context, attributeSet: AttributeSet? = null) : ConstraintLayout(context, attributeSet), View.OnClickListener {


    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.mainView -> {

            }
        }
    }


    init {
        View.inflate(context, R.layout.event_view, this)
        mainView.setOnClickListener(this)
    }

}