package com.itsovertime.overtimecamera.play.permissions

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.itsovertime.overtimecamera.play.R
import kotlinx.android.synthetic.main.permissions_item_view.view.*

class PermissionsItemView(context: Context, attributeSet: AttributeSet? = null) :
    ConstraintLayout(context, attributeSet) {


    init {
        View.inflate(context, R.layout.permissions_item_view, this)
    }

    fun bind(pres: PermissionPresentation) {
        permission.text = pres.permissionTitle
    }
}