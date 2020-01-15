package com.itsovertime.overtimecamera.play.filemanager

interface FileManager {

    fun onDeleteFile(path:String)
    fun onDoesFileExist(path: String): Boolean
    fun onDeleteOldVideoFiles()

}