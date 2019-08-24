package com.itsovertime.overtimecamera.play.baseactivity

interface BaseActivityInt {

    fun displayPermissions()
    fun displayAlert()
    fun displayDeniedPermissionsView()
    fun setUpAdapter()
    fun displayProgress()
    fun hideDisplayProgress()
    fun displayErrorFromResponse()
    fun displayEnterResponseView(number: String)
    fun resetViews()
}


