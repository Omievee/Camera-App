package com.itsovertime.overtimecamera.play.uploads

import android.annotation.SuppressLint
import com.itsovertime.overtimecamera.play.db.AppDatabase
import com.itsovertime.overtimecamera.play.model.SavedVideo
import com.itsovertime.overtimecamera.play.network.EncryptedResponse
import com.itsovertime.overtimecamera.play.network.TokenResponse
import com.itsovertime.overtimecamera.play.network.VideoInstanceResponse
import com.itsovertime.overtimecamera.play.uploadsmanager.UploadsManager
import com.itsovertime.overtimecamera.play.videomanager.VideosManager
import com.itsovertime.overtimecamera.play.wifimanager.NETWORK_TYPE
import com.itsovertime.overtimecamera.play.wifimanager.WifiManager
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.io.File
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class UploadsPresenter(
    val view: UploadsFragment,
    val manager: VideosManager,
    private val wifiManager: WifiManager,
    private val uploadManager: UploadsManager
) {


    fun onCreate() {
        manager.loadFromDB()
        subscribeToNetworkUpdates()

    }

    fun onResume() {
        view.swipe2RefreshIsTrue()
        subscribeToVideosFromGallery()
        subscribeToCurrentVideoBeingUploaded()

    }


    var clientIdDisposable: Disposable? = null
    var currentVideoQue: List<SavedVideo>? = null
    private fun subscribeToCurrentVideoBeingUploaded() {
        clientIdDisposable?.dispose()
        clientIdDisposable =
            uploadManager
                .onUpdatedQue()
                .subscribe({
                    currentVideoQue = it
                    println("+++++++QueSize ${currentVideoQue?.size}+++++++++")
                    currentVideoQue?.forEach {
                        getVideoInstance(it)
                        println("QUE CHECK ::::::: ${it.is_favorite} && ${it.clientId}")
                    }
                }, {
                })
    }

    private var managerDisposable: Disposable? = null
    private fun subscribeToVideosFromGallery() {
        managerDisposable?.dispose()
        managerDisposable = manager
            .subscribeToVideoGallery()
            .subscribe({
                view.updateAdapter(it)
                view.swipe2RefreshIsFalse()
            }, {
                println("throwable: ${it.printStackTrace()}")
            })
    }

    private var networkDisposable: Disposable? = null
    private fun subscribeToNetworkUpdates() {
        networkDisposable?.dispose()
        networkDisposable = wifiManager
            .subscribeToNetworkUpdates()
            .subscribe({
                when (it) {
                    NETWORK_TYPE.WIFI -> view.displayWifiReady()
//                    NETWORK_TYPE.MOBILE_LTE -> view.display
//                    NETWORK_TYPE.MOBILE_EDGE -> view.display
                    else -> view.displayNoNetworkConnection()
                }

            }, {
                it.printStackTrace()
            })
    }

    private var instanceDisposable: Disposable? = null

    var currentVideo: SavedVideo? = null
    @Synchronized
    private fun getVideoInstance(it: SavedVideo) {
        currentVideo = it
        instanceDisposable?.dispose()
        instanceDisposable =
            uploadManager
                .getVideoInstance(currentVideo ?: return)
                .doOnError {}
                .map {
                    videoInstanceResponse = it
                }
                .doOnSuccess {
                    manager.updateVideoInstanceId(
                        videoInstanceResponse?.video?.id.toString() ?: "",
                        currentVideo?.clientId.toString() ?: ""

                    )
                    requestTokenForUpload(videoInstanceResponse)
                }
                .subscribe({
                }, {

                })

    }

    var videoInstanceResponse: VideoInstanceResponse? = null
    private var tokenResponse: TokenResponse? = null
    private var tokenDisposable: Disposable? = null
    private fun requestTokenForUpload(response: VideoInstanceResponse?) {
        awsDataDisposable?.dispose()
        awsDataDisposable =
            uploadManager
                .getAWSDataForUpload(response ?: return)
                .doOnError {

                }
                .map {
                    tokenResponse = it
                }
                .doOnSuccess {
                    println("success from aws data....")
                    beginUpload()
                }
                .subscribe({
                }, {

                })
    }


    private var awsDataDisposable: Disposable? = null
    private var encryptionResponse: EncryptedResponse? = null
    private fun beginUpload() {
        println("Begin upload presenter.... $tokenResponse")
        tokenDisposable?.dispose()
        tokenDisposable =
            uploadManager
                .registerWithMD5(tokenResponse ?: return)
                .map {
                    encryptionResponse = it
                    manager.updateVideoMd5(
                        md5 = encryptionResponse?.upload?.md5.toString(),
                        clientId = currentVideo?.clientId.toString()
                    )

                    manager.updateUploadId(
                        uplaodId = encryptionResponse?.upload?.id.toString(),
                        clientId = currentVideo?.clientId.toString()
                    )

                }
                .doOnSuccess {
                    getVideoForUpload(currentVideo ?: return@doOnSuccess)
                }
                .doOnError {
                    println("Begin upload error...... ${it.message}")
                }
                .subscribe({
                }, {
                    println("Final throw:::: ${it.message}")
                })

    }

    var vid: SavedVideo? = null
    @SuppressLint("CheckResult")
    private fun getVideoForUpload(savedVideo: SavedVideo) {
        var db = view?.context?.let { AppDatabase.getAppDataBase(context = it) }
        var videoDao = db?.videoDao()
        Single.fromCallable {
            with(videoDao) {
                this?.getVideoForUpload(savedVideo?.clientId)
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .map {
                vid = it
            }
            .subscribe({
                continueUploadProcess()
                //   continueUploadProcess(true)
            }, {
                it.printStackTrace()
            })
    }

    var uploadChunkIndex = 0
    private var MIN_CHUNK_SIZE = 0.5 * 1024
    private var MAX_CHUNK_SIZE = 2 * 1024 * 1024
    private var chunkSize = 1024
    var chunk: Int = 0
    var offset = 0
    private fun continueUploadProcess() {
        chunk = MAX_CHUNK_SIZE
        val fullBytes = File(vid?.mediumRes).readBytes().size

        val remainder = fullBytes - offset
        println("Remainder is $remainder Offset is: $offset Chunk is: $chunk")
        var endRange = offset + chunk - 1
        if (remainder < chunk) {
            println("inside iff..")
            endRange = offset + remainder - 1
        }
        val sliceFromBytes = File(vid?.mediumRes).readBytes().sliceArray(
            IntRange(
                offset,
                endRange
            )
        )
        println("Full bytes?? $fullBytes")
        println("slice size:: ${sliceFromBytes.size}")
        println("offset is ::: $offset")
        println("Index :: $uploadChunkIndex")
        if (offset <= fullBytes) {
            synchronized(this) {
                upload(
                    chunkIndex = uploadChunkIndex,
                    bytes = sliceFromBytes
                )
            }
        } else {
            checkForComplete()
        }
    }

    @SuppressLint("CheckResult")
    var up: Disposable? = null

    private fun upload(chunkIndex: Int, bytes: ByteArray) {
        synchronized(this) {
            up = uploadManager
                .uploadVideoToServer(
                    upload = encryptionResponse?.upload ?: return@synchronized,
                    array = bytes,
                    chunk = chunkIndex
                )
                .doOnSuccess {
                    if (it.success) {
                        uploadChunkIndex++
                        offset += chunk
                        continueUploadProcess()
                    }
                }
                .doOnError {
                    uploadChunkIndex = 0
                }
                .subscribe({
                }, {
                    it.printStackTrace()
                })
        }
    }


    var complete: Disposable? = null
    private fun checkForComplete() {
        complete = uploadManager
            .onCompleteUpload(encryptionResponse?.upload?.id ?: "")
            .subscribe({
                println("status is :::: ${it.status}")
            }, {
                it.printStackTrace()
            })
    }

    private fun breakFileIntoChunks(file: File, size: Int): Array<ByteArray> {
        return divideArray(
            file.readBytes(),
            size
        )
    }

    @Synchronized
    private fun divideArray(source: ByteArray, chunksize: Int): Array<ByteArray> {
        val ret = Array(ceil(source.size / chunksize.toDouble()).toInt()) { ByteArray(chunksize) }
        var start = 0
        for (i in ret.indices) {
            if (start + chunksize > source.size) {
                System.arraycopy(source, start, ret[i], 0, source.size - start)
            } else {
                System.arraycopy(source, start, ret[i], 0, chunksize)
            }
            start += chunksize
        }
        return ret
    }

    fun onDestroy() {
        managerDisposable?.dispose()
        awsDataDisposable?.dispose()
        networkDisposable?.dispose()
        instanceDisposable?.dispose()
        tokenDisposable?.dispose()
        clientIdDisposable?.dispose()
    }

    fun displayBottomSheetSettings() {
        view.displaySettings()
    }
}