package com.itsovertime.overtimecamera.play.tos

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.itsovertime.overtimecamera.play.R
import kotlinx.android.synthetic.main.fragment_tos.*
import kotlinx.android.synthetic.main.fragment_tos.view.*

class TOSView(context: Context, attributeSet: AttributeSet? = null) :
    ConstraintLayout(context, attributeSet), View.OnClickListener {
    override fun onClick(v: View?) {

    }

    init {
        View.inflate(context, R.layout.fragment_tos, this)
        webview.loadUrl(context.getString(R.string.tos_url))
    }
}