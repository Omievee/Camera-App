package com.itsovertime.overtimecamera.play.uploadsmanager

import com.itsovertime.overtimecamera.play.application.OTApplication
import com.itsovertime.overtimecamera.play.model.SavedVideo
import com.itsovertime.overtimecamera.play.network.*
import com.itsovertime.overtimecamera.play.wifimanager.WifiManager
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import okhttp3.MediaType
import java.io.*
import java.util.*
import okhttp3.RequestBody
import java.nio.file.Files
import kotlin.math.ceil
import java.math.BigInteger
import java.nio.charset.Charset
import java.security.MessageDigest


class UploadsManagerImpl(
    val context: OTApplication,
    val api: Api,
    val manager: WifiManager
) : UploadsManager {


    override fun onCurrentVideoId(): Observable<String> {
        return subject
            .observeOn(Schedulers.io())
            .subscribeOn(AndroidSchedulers.mainThread())
    }

    private var MIN_CHUNK_SIZE = 0.5 * 1024
    private var MAX_CHUNK_SIZE = 2 * 1024 * 1024
    private var chunkSize = 1 * 1024
    private var uploadRate: Double = 0.0
    private var time = System.currentTimeMillis()
    private val subject: BehaviorSubject<String> = BehaviorSubject.create()

    override fun getVideoInstance(): Single<VideoInstanceResponse> {
        subject.onNext(favoriteVideos[0].clientId)
        return api
            .getVideoInstance(
                VideoInstanceRequest(
                    client_id = UUID.fromString(favoriteVideos[0].clientId),
                    is_favorite = favoriteVideos[0].is_favorite,
                    is_selfie = favoriteVideos[0].is_selfie,
                    latitude = favoriteVideos[0].latitude ?: 0.0,
                    longitude = favoriteVideos[0].longitude ?: 0.0
                )
            )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }


    override fun registerUploadForId(data: TokenResponse): Single<EncryptedResponse> {
        val md5 = toHex(File(favoriteVideos[0].trimmedVidPath).absolutePath)
        return api
            .uploadDataForMd5(
                UploadRequest(
                    md5,
                    data.S3Bucket,
                    data.S3Key,
                    data.AccessKeyId,
                    data.SecretAccessKey,
                    data.SessionToken
                )
            )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    override fun getAWSDataForUpload(response: VideoInstanceResponse): Single<TokenResponse> {
        return api
            .uploadToken(response)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }


//    private fun getMD5EncryptedString(encTarget: String): String {
//        var mdEnc: MessageDigest? = null
//        try {
//            mdEnc = MessageDigest.getInstance("MD5")
//        } catch (e: NoSuchAlgorithmException) {
//            println("Exception while encrypting to md5")
//            e.printStackTrace()
//        }
//        // Encryption algorithm
//        mdEnc?.update(encTarget.toByteArray(), 0, encTarget.length)
//        var md5 = BigInteger(1, mdEnc?.digest()).toString(16)
//        while (md5.length < 32) {
//            md5 = "0$md5"
//        }
//        return md5
//    }

    fun toHex(string: String): String {
        val md5 = MessageDigest.getInstance("MD5")
        val hash = BigInteger(
            1,
            md5.digest(string.toByteArray(Charset.defaultCharset()))
        ).toString(16)
        return hash
    }

    private fun toHexString(bytes: ByteArray): String {
        val hexString = StringBuilder()
        for (i in bytes.indices) {
            val hex = Integer.toHexString(0xFF and bytes[i].toInt())
            if (hex.length == 1) {
                hexString.append('0')
            }
            hexString.append(hex)
        }
        return hexString.toString()
    }


    @ExperimentalStdlibApi
    override fun uploadVideo(upload: Upload): Observable<VideoUploadResponse> {
        val array = divideArray(
            Files.readAllBytes(File(favoriteVideos[0].trimmedVidPath).toPath()),
            MIN_CHUNK_SIZE.toInt()
        )

        val md5 = MessageDigest.getInstance("MD5")
        val hash = BigInteger(
            1,
            md5.digest(array[0])
        ).toString(16)

        val request = RequestBody.create(
            MediaType.parse("application/octet-stream"),
            array[0]
        )
        return api
            .uploadSelectedVideo(
                md5Header = hash,
                videoId = upload.id ?: "",
                uploadChunk = 0,
                file = request
            )
            .doOnError {
                println("Throwable... ${it.localizedMessage}")
            }
            .doOnComplete {
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    private var favoriteVideos = mutableListOf<SavedVideo>()
    private var standardVideos = mutableListOf<SavedVideo>()

    private fun divideArray(source: ByteArray, chunksize: Int): Array<ByteArray> {
        val ret =
            Array(ceil(source.size / chunksize.toDouble()).toInt()) { ByteArray(chunksize) }
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


    override fun onReadyVideosForUpload(videoList: MutableList<SavedVideo>) {
        favoriteVideos.clear()
        standardVideos.clear()
        videoList.forEach {
            when (it.is_favorite) {
                true -> {
                    favoriteVideos.add(it)
                }
                else -> standardVideos.add(it)
            }
        }

    }

    override fun onUploadFavoriteMedQualityVideo(): Single<VideoInstanceRequest> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onUploadMediumQualityVideo() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onUploadHighQualityVideo() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}


//val file = splitFile(File(favoriteVideos[0].trimmedVidPath))[0]
////        val file = divideArray(
////            File(favoriteVideos[0].trimmedVidPath).readBytes(),
////            MIN_CHUNK_SIZE.toInt()
////        )[0].clone()
//        val l = splitFile(File(favoriteVideos[0].trimmedVidPath))[0].path
//        val s = divideArray(File(favoriteVideos[0].trimmedVidPath).readBytes(), chunkSize)
//        val `in` = FileInputStream(File(favoriteVideos[0].trimmedVidPath))
//        val buf: ByteArray
//        buf = ByteArray(`in`.available())
//        while (`in`.read(buf) !== -1);
//@Throws(IOException::class)
//fun splitFile(f: File): List<File> {
//    var partCounter = 1
//    val result = arrayListOf<File>()
//    val sizeOfFiles = 1024 * 1024// 1MB
//    val buffer = ByteArray(sizeOfFiles) // create a buffer of bytes sized as the one chunk size
//    val bis = BufferedInputStream(FileInputStream(f))
//    val name = f.name
//    println("byte.. ${(bis.read(buffer)) > 0}")
//    while ((bis.read(buffer)) > 0) {
//        val newFile = File(
//            f.parent,
//            name + "." + String.format("%03d", partCounter++)
//        ) // naming files as <inputFileName>.001, <inputFileName>.002, ...
//        val out = FileOutputStream(newFile)
//        out.write(
//            buffer,
//            0,
//            chunkSize
//        )//tmp is chunk size. Need it for the last chunk, which could be less then 1 mb.
//        result.add(newFile)
//    }
//    return result
//}