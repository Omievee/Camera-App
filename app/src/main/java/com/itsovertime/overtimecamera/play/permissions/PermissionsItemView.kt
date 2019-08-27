package com.itsovertime.overtimecamera.play.permissions

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.itsovertime.overtimecamera.play.R

class PermissionsItemView(context: Context, attributeSet: AttributeSet? = null) :
    ConstraintLayout(context, attributeSet) {


    init {
        View.inflate(context, R.layout.permissions_item_view, this)

    }

    fun bind(pres: PermissionPresentation) {
        println("Permission??? ${pres.permissionTitle}")
    }
}