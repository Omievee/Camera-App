package com.itsovertime.overtimecamera.play.baseactivity

interface BaseActivityInt {

    fun displayAlert()
    fun setUpAdapter()
    fun displayProgress()
    fun hideDisplayProgress()
    fun displayErrorFromResponse()
    fun displayEnterResponseView(number: String)
    fun resetViews()
    fun displaySignUpPage()

    fun beginPermissionsFlow()
    fun allowAccess()


}


