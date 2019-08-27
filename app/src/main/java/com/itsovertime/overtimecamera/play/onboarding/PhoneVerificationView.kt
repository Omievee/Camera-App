package com.itsovertime.overtimecamera.play.onboarding

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.itsovertime.overtimecamera.play.R
import java.util.jar.Attributes

class PhoneVerificationView(context: Context, attributes: AttributeSet? = null) :
    ConstraintLayout(context, attributes){


    init {
        View.inflate(context, R.layout.phone_verification, this)
    }

}