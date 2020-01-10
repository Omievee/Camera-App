package com.itsovertime.overtimecamera.play.filemanager

import com.itsovertime.overtimecamera.play.application.OTApplication
import java.io.File

class FileManagerImpl(val context: OTApplication) : FileManager {
    override fun onDeleteFile(path: String) {
        if (File(path).exists()) {
            File(path).delete()
        }
    }

    override fun onDoesFileExist(path: String): Boolean {
        return when (File(path).exists()) {
            true -> true
            else -> false
        }
    }

    override fun onDeleteOldVideoFiles() {

    }
}