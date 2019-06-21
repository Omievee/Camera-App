package com.itsovertime.overtimecamera.play.camera

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.CamcorderProfile
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.*
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.itsovertime.overtimecamera.play.R
import dagger.android.support.AndroidSupportInjection
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_camera.*
import java.io.File
import java.io.IOException
import java.lang.Long
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class CameraFragment : Fragment(), CameraInt, View.OnClickListener, View.OnTouchListener {

    private val SENSOR_ORIENTATION_DEFAULT_DEGREES = 90
    private val SENSOR_ORIENTATION_INVERSE_DEGREES = 270


    private val DEFAULT_ORIENTATIONS = SparseIntArray().apply {
        append(Surface.ROTATION_0, 90)
        append(Surface.ROTATION_90, 0)
        append(Surface.ROTATION_180, 270)
        append(Surface.ROTATION_270, 180)
    }
    private val INVERSE_ORIENTATIONS = SparseIntArray().apply {
        append(Surface.ROTATION_0, 270)
        append(Surface.ROTATION_90, 180)
        append(Surface.ROTATION_180, 90)
        append(Surface.ROTATION_270, 0)
    }
    var backgroundThread: HandlerThread? = null
    var recordThread: HandlerThread? = null
    var backgroundHandler: Handler? = null
    val cameraOpenCloseLock = Semaphore(1)
    var cameraDevice: CameraDevice? = null
    var videoSize: Size? = null
    var recording: Boolean = false
    var previewSize: Size? = null
    var sensorOrientation = 0
    var videoFile: File? = null
    var mediaRecorder: MediaRecorder? = null
    var txView: TXView? = null
    var captureSession: CameraCaptureSession? = null
    lateinit var previewRequestBuilder: CaptureRequest.Builder


    @Inject
    lateinit var presenter: CameraPresenter


    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.tapToSave -> {
                progress.visibility = View.VISIBLE
                if (recording) {
                    stopLiveView(false)
                } else {
                    startLiveView()
                }
            }
            R.id.favoriteIcon -> {
                presenter.updateFavoriteField()
                favoriteIcon.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.fave, null))
            }
            R.id.selfie -> presenter.cameraSwitch()

            R.id.pauseButton -> {
                paused = true
                releaseCamera()
                pausedView.visibility = View.VISIBLE
            }
            R.id.pausedView -> {
                paused = false
                pausedView.visibility = View.GONE
                engageCamera()

            }
            R.id.hahaIcon -> {
                presenter.updateFunnyField()
                hahaIcon.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.funny_active, null))
            }
            R.id.uploadButton -> {
                paused = true
                releaseCamera()
                callback?.onUploadsButtonClicked()
            }
        }
    }

    private var callback: UploadsButtonClick? = null
    fun setUploadsClickListener(listener: UploadsButtonClick) {
        this.callback = listener
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun setUpClicks() {
        progressBar.setOnClickListener(this)
        tapToSave.setOnClickListener(this)
        favoriteIcon.setOnClickListener(this)
        selfie.setOnClickListener(this)
        pauseButton.setOnClickListener(this)
        txView?.setOnTouchListener(this)
        pausedView.setOnClickListener(this)
        hahaIcon.setOnClickListener(this)
        uploadButton.setOnClickListener(this)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        return gestureDetector.onTouchEvent(event)
    }

    private var gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent?): Boolean {
            presenter.cameraSwitch()
            return true
        }
    }
    )

    override fun switchCameras() {
        CAMERA = if (CAMERA == 0) {
            1
        } else {
            0
        }
        releaseCamera()
        engageCamera()

        when (CAMERA) {
            1 -> {
                favoriteIcon.visibility = View.INVISIBLE
                hahaIcon.visibility = View.INVISIBLE
                progress.visibility = View.GONE
            }
            0 -> {
                favoriteIcon.visibility = View.VISIBLE
                hahaIcon.visibility = View.VISIBLE
                progress.visibility = View.GONE

            }
        }
    }

    override fun closeCamera() {
        try {
            cameraOpenCloseLock.acquire()
            closePreviewSession()
            cameraDevice?.close()
            cameraDevice = null
            mediaRecorder?.release()
            mediaRecorder = null
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock overtimecamera closing.", e)
        } finally {
            cameraOpenCloseLock.release()
        }
    }

    @SuppressLint("CheckResult")
    override fun startRecording() {
        recording = true
        if (cameraDevice == null || !txView?.isAvailable!!) {
            return
        }
        try {
            closePreviewSession()
            println("preCallable")
            Single.fromCallable {
                setUpMediaRecorder()
            }.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError {
                    println("Error: ${it.printStackTrace()}")
                }
                .doFinally {
                    progress.visibility = View.GONE
                }
                .subscribe({
                    println("inSubScribe")
                    it?.prepare()
                    val texture = txView?.surfaceTexture.apply {
                        this?.setDefaultBufferSize(
                            previewSize?.width ?: 0, previewSize?.height
                                ?: 0
                        )
                    }

                    val previewSurface = Surface(texture)
                    val recorderSurface = it?.surface
                    val surfaces = ArrayList<Surface>().apply {
                        add(previewSurface)
                        recorderSurface?.let { s ->
                            add(s)
                        }
                    }
                    previewRequestBuilder =
                        cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG).apply {
                            this?.addTarget(previewSurface)
                            recorderSurface?.let { s ->
                                this?.addTarget(s)
                            }
                        }!!

                    cameraDevice?.createCaptureSession(
                        surfaces, object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                                captureSession = cameraCaptureSession
                                previewRequestBuilder.set(
                                    CaptureRequest.CONTROL_MODE,
                                    CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON
                                )
                                try {
                                    setUpCaptureRequestBuilder(previewRequestBuilder)
                                    HandlerThread("CameraPreview").start()
                                    startRecordingThread()
                                    captureSession?.setRepeatingRequest(
                                        previewRequestBuilder.build(),
                                        null, recordHandler
                                    )
                                } catch (e: CameraAccessException) {
                                    Log.e("CameraMain", e.toString())
                                } catch (ise: IllegalStateException) {
                                    println("state : ${ise.printStackTrace()}")
                                }

                                println("preStart")
                                activity?.run {
                                    println("run")
                                    it?.start()
                                }
                            }

                            override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                                showToast("Failed")
                            }
                        }, null
                    )
                }, {
                    it.printStackTrace()
                })
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @SuppressLint("CheckResult")
    override fun stopRecording(isPaused: Boolean) {
        stopRecordingThread()
        recording = false
        try {
            Observable.fromCallable {
                mediaRecorder?.apply {
                    stop()
                    reset()
                }
            }.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally {
                    println("stopped recording ::: $isPaused")
                    when (isPaused) {
                        false -> {
                            presenter.saveRecordingToDataBase()
//                                startLiveView()
                        }
                        else -> presenter.deletePreviousFile()
                    }

                }
                .subscribe({
                }, {
                    it.printStackTrace()
                })
        } catch (r: RuntimeException) {
            r.printStackTrace()
        } catch (e: java.lang.IllegalStateException) {
            e.printStackTrace()
        }
    }

    private fun hideViews() {
        favoriteIcon.visibility = View.VISIBLE
        hahaIcon.visibility = View.VISIBLE
        val timer = Timer()
        val timerTask = object : TimerTask() {
            override fun run() {
                favoriteIcon?.let {
                    it.visibility = View.INVISIBLE
                }
                hahaIcon?.let {
                    it.visibility = View.INVISIBLE
                }
            }
        }
        timer.schedule(timerTask, 5000)
    }

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun startPreview() {
        try {
            closePreviewSession()
            val texture = txView?.surfaceTexture
            texture?.setDefaultBufferSize(previewSize?.width ?: 0, previewSize?.height ?: 0)
            cameraDevice?.let {
                previewRequestBuilder = it.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            }

            val previewSurface = Surface(texture)
            previewRequestBuilder.addTarget(previewSurface)
            cameraDevice?.createCaptureSession(
                listOf(previewSurface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        updatePreview()
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                    }
                }, backgroundHandler
            )

            startLiveView()
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    override fun closePreviewSession() {
        try {
            captureSession?.abortCaptures()
            captureSession?.close()
            captureSession = null
        } catch (i: java.lang.IllegalStateException) {
            i.printStackTrace()
            println("close preview::: ${i.message}")
        }

    }

    override fun updatePreview() {
        if (cameraDevice == null) return
        try {
            setUpCaptureRequestBuilder(previewRequestBuilder)
            HandlerThread("CameraPreview").start()
            println("pre rec thread")
            captureSession?.setRepeatingRequest(
                previewRequestBuilder.build(),
                null, backgroundHandler
            )
        } catch (e: CameraAccessException) {
            Log.e("CameraMain", e.toString())
        } catch (ise: IllegalStateException) {
            println("state : ${ise.printStackTrace()}")
        }
    }

    fun setUpCaptureRequestBuilder(builder: CaptureRequest.Builder?) {
        builder?.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_camera, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        txView = cameraView
        presenter.setUpClicks()
    }


    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(p0: SurfaceTexture?, p1: Int, p2: Int) {
            openCamera(p1, p2, CAMERA)
        }

        override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {}
        override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture) = true
        override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) = Unit
    }

    private var CAMERA: Int = 0


    fun engageCamera() {
        startBackgroundThread()
        txView?.let {
            if (it.isAvailable) {
                openCamera(it.width, it.height, CAMERA)
            } else {
                it.surfaceTextureListener = surfaceTextureListener
            }
        }

    }

    override fun onPause() {
        releaseCamera()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        if (pausedView.visibility == View.GONE) {
            println("onResume")
            paused = false
            progress.visibility = View.VISIBLE
            engageCamera()

        }
    }

    var paused: Boolean = false
    private fun releaseCamera() {
        closeCamera()
        stopBackgroundThread()
        if (recording) {
            paused = true
            stopLiveView(paused)
        }
    }

    @SuppressLint("CheckResult")
    private fun startLiveView() {
        //presenter.animateProgressBar(progressBar)
        startRecording()
    }


    private fun stopLiveView(isPaused: Boolean) {
        progressBar.clearAnimation()
        stopRecording(isPaused)
        hideViews()
        hahaIcon.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.haha, null))
        favoriteIcon.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.favebutton, null))
    }

    fun showToast(message: String) {
        Toast.makeText(
            context,
            message,
            Toast.LENGTH_LONG
        ).show()
    }


    @SuppressLint("MissingPermission")
    override fun openCamera(width: Int, height: Int, camera: Int) {
        val manager = context?.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw RuntimeException("Time out waiting to lock overtimecamera opening.")
            }

            val cameraId = when (camera) {
                0 -> manager.cameraIdList[0]
                else -> manager.cameraIdList[1]

            }
            val characteristics = manager.getCameraCharacteristics(cameraId)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                ?: throw RuntimeException("Cannot get available preview/video sizes")
            sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
            videoSize = chooseVideoSize(map.getOutputSizes(MediaRecorder::class.java))
            videoSize?.let {
                previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture::class.java), width, height, it)
            }

            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                txView?.setAspectRatio(previewSize?.width ?: 0, previewSize?.height ?: 0)
            } else {
                txView?.setAspectRatio(previewSize?.height ?: 0, previewSize?.width ?: 0)
            }

            mediaRecorder = MediaRecorder()
            txView?.setTransform(Matrix())
            manager.openCamera(cameraId, cameraStateCallBack, backgroundHandler)

        } catch (e: CameraAccessException) {
            activity?.finishAffinity()
        } catch (e: NullPointerException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock overtimecamera opening.")
        }
    }

    fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground")
        backgroundThread?.start()
        backgroundHandler = Handler(backgroundThread?.looper)
    }

    fun startRecordingThread() {
        recordThread = HandlerThread("Recording")
        recordThread?.start()
        recordHandler = Handler(recordThread?.looper)
    }

    var recordHandler: Handler? = null
    fun stopRecordingThread() {
        recordThread?.quitSafely()
        try {
            recordThread?.join()
            recordThread = null
            recordHandler = null
        } catch (e: InterruptedException) {
            Log.e("CameraMain", e.toString())
        }
    }

    fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            Log.e("CameraMain", e.toString())
        }
    }

    @Throws(IOException::class)
    private fun setUpMediaRecorder(): MediaRecorder? {
        val videoTimeStamp = System.currentTimeMillis().toString()
        videoFile = presenter.getVideoFilePath(videoTimeStamp)
        val rotation = activity?.windowManager?.defaultDisplay?.rotation
        when (sensorOrientation) {
            SENSOR_ORIENTATION_DEFAULT_DEGREES ->
                mediaRecorder?.setOrientationHint(DEFAULT_ORIENTATIONS.get(rotation ?: 0))
            SENSOR_ORIENTATION_INVERSE_DEGREES ->
                mediaRecorder?.setOrientationHint(INVERSE_ORIENTATIONS.get(rotation ?: 0))
        }

        val profile: CamcorderProfile = when (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_HIGH_SPEED_1080P)) {
            true -> CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH_SPEED_1080P)
            else -> CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH)
        }
        println("preSetUpMediaRecorder")

        mediaRecorder?.apply {

            setAudioSource(MediaRecorder.AudioSource.MIC)
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setVideoEncodingBitRate(profile.videoBitRate)
            setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight)
            setVideoFrameRate(profile.videoFrameRate)
            setAudioEncoder(MediaRecorder.AudioEncoder.HE_AAC)
            setAudioEncodingBitRate(profile.audioBitRate)
            setOutputFile(videoFile?.absolutePath)

        }
        println("recorder set up.. returning.....")
        return mediaRecorder
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (isVisibleToUser && view != null) {
            engageCamera()
        }
    }

    private val cameraStateCallBack = object : CameraDevice.StateCallback() {
        override fun onOpened(cameraDevice: CameraDevice) {
            cameraOpenCloseLock.release()
            this@CameraFragment.cameraDevice = cameraDevice
            startPreview()
        }

        override fun onDisconnected(cameraDevice: CameraDevice) {
            cameraOpenCloseLock.release()
            cameraDevice.close()
            println("overtimecamera open disconnect")
            this@CameraFragment.cameraDevice = null
        }

        override fun onError(cameraDevice: CameraDevice, error: Int) {
            cameraOpenCloseLock.release()
            cameraDevice.close()
            this@CameraFragment.cameraDevice = null
            println("call back error $error")
            activity?.finishAffinity()
        }
    }

    private fun chooseVideoSize(choices: Array<Size>) = choices.firstOrNull {
        it.width == it.height * 4 / 3 && it.width <= 1080
    } ?: choices[choices.size - 1]


    private fun chooseOptimalSize(
        choices: Array<Size>,
        width: Int,
        height: Int,
        aspectRatio: Size
    ): Size {

        val w = aspectRatio.width
        val h = aspectRatio.height
        val bigEnough = choices.filter {
            it.height == it.width * h / w && it.width >= width && it.height >= height
        }

        return if (bigEnough.isNotEmpty()) {
            Collections.min(bigEnough, Compare())
        } else {
            choices[0]
        }
    }

    interface UploadsButtonClick {
        fun onUploadsButtonClicked()
    }
}

class Compare : Comparator<Size> {
    override fun compare(lhs: Size, rhs: Size) =
        Long.signum(lhs.width.toLong() * lhs.height - rhs.width.toLong() * rhs.height)
}

