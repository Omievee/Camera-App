package com.itsovertime.overtimecamera.play.settings

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.itsovertime.overtimecamera.play.R
import kotlinx.android.synthetic.main.item_layout_settings.view.*

class SettingsView(context: Context, attributeSet: AttributeSet? = null) : ConstraintLayout(context, attributeSet),
    View.OnClickListener {

    var pres: SettingsPresentation? = null
    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.view -> settingsClickListener?.onClick(pres ?: return)
        }
    }

    var settingsClickListener: SettingsClickListener? = null

    init {
        View.inflate(context, R.layout.item_layout_settings, this)
        view.setOnClickListener(this)
    }

    fun bind(pres: SettingsPresentation?) {
        this.pres = pres
        settingsOptionText.text = pres?.title
    }
}