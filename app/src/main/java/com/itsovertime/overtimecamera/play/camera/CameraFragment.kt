package com.itsovertime.overtimecamera.play.camera

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
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
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.itsovertime.overtimecamera.play.R
import com.itsovertime.overtimecamera.play.events.EventsAdapter
import com.itsovertime.overtimecamera.play.events.EventsClickListener
import com.itsovertime.overtimecamera.play.model.Event
import dagger.android.support.AndroidSupportInjection
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.event_view.*
import kotlinx.android.synthetic.main.fragment_camera.*
import kotlinx.android.synthetic.main.upload_button_view.*
import java.io.File
import java.io.IOException
import java.lang.Long
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class CameraFragment : Fragment(), CameraInt, View.OnClickListener, View.OnTouchListener {
    override fun updateEventTitle(event: String) {
        eventTitle.text = event
    }

    override fun hideEventsRV() {
        //  presenter.collapse(hiddenEvents)
        hiddenEvents.visibility = View.GONE
//        hiddenEvents.animate()
//            .translationY(0f)
//            .alpha(0.0f)
//            .setListener(object : AnimatorListenerAdapter() {
//                override fun onAnimationEnd(animation: Animator?) {
//                    super.onAnimationEnd(animation)
//
//                }
//            })

    }

    override fun openEvents() {
        //presenter.expand(hiddenEvents)
        hiddenEvents.visibility = View.VISIBLE
//        hiddenEvents.alpha = 0.0f;
//        hiddenEvents.animate()
//            .translationY(eventSpace.height.toFloat())
//            .alpha(1.0f)
//            .setListener(null);
    }

    override fun setUpEventViewData(eventList: List<Event>?) {
        if (eventList.isNullOrEmpty()) {
            eventSpace.visibility = View.GONE
        }
        selectedEvent = eventList?.get(0)

        evAdapter = EventsAdapter(eventList, listener)
        hiddenEvents.adapter = evAdapter

    }

    override fun showOrHideViewsForCamera() {
        when (CAMERA) {
            1 -> {
                selfieCameraEngaged = true
                favoriteIcon.visibility = View.INVISIBLE
                hahaIcon.visibility = View.INVISIBLE
                progressBar.visibility = View.GONE

            }
            0 -> {
                selfieCameraEngaged = false
                favoriteIcon.visibility = View.VISIBLE
                hahaIcon.visibility = View.VISIBLE
                progressBar.visibility = View.VISIBLE
            }
        }
    }

    override fun stopProgressAnimation() {
        progressBar?.let {
            it.clearAnimation()
        }

    }

    override fun updateUploadsIconCount(count: String) {
        counterText.text = count
    }

    private val SENSOR_ORIENTATION_DEFAULT_DEGREES = 90
    private val SENSOR_ORIENTATION_INVERSE_DEGREES = 270


    private val DEFAULT_ORIENTATIONS = SparseIntArray().apply {
        append(Surface.ROTATION_0, 0)
        append(Surface.ROTATION_90, 90)
        append(Surface.ROTATION_180, 180)
        append(Surface.ROTATION_270, 270)
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
            R.id.eventSpace -> presenter.displayHiddenView()
            R.id.tapToSave -> {
                progress.visibility = View.VISIBLE
                favoriteIcon.visibility = View.VISIBLE
                hahaIcon.visibility = View.VISIBLE
                println("")
                if (recording) {
                    releaseCamera(tapToSave = true)
                    if (CAMERA == 0) resetIcons()
                } else {
                    startLiveView()
                }
            }
            R.id.favoriteIcon -> {
                presenter.updateFavoriteField()
                favoriteIcon.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        resources,
                        R.drawable.fave,
                        null
                    )
                )
            }
            R.id.selfieButton -> {
                presenter.cameraSwitch()
                presenter.determineViewsForCameraId()
                deleteUnsavedFile()
            }
            R.id.pauseButton -> {
                pausedView.visibility = View.VISIBLE
                paused = true
                deleteUnsavedFile()
                releaseCamera(tapToSave = false)
            }
            R.id.pausedView -> {
                paused = false
                pausedView.visibility = View.GONE
                progress.visibility = View.VISIBLE
                engageCamera()
            }
            R.id.hahaIcon -> {
                presenter.updateFunnyField()
                hahaIcon.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        resources,
                        R.drawable.funny_active,
                        null
                    )
                )
            }
            R.id.uploadButton -> {
                paused = true
                progress.visibility = View.VISIBLE
                deleteUnsavedFile()
                callback?.onUploadsButtonClicked()
                releaseCamera(tapToSave = false)
            }
        }
    }


    private var callback: UploadsButtonClick? = null
    fun setUploadsClickListener(listener: UploadsButtonClick) {
        this.callback = listener
    }

    private fun deleteUnsavedFile() {
        presenter.deletePreviousFile()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun setUpClicks() {

        progressBar.setOnClickListener(this)
        tapToSave.setOnClickListener(this)
        favoriteIcon.setOnClickListener(this)
        selfieButton.setOnClickListener(this)
        pauseButton.setOnClickListener(this)
        pausedView.setOnClickListener(this)
        hahaIcon.setOnClickListener(this)
        eventSpace.setOnClickListener(this)
        uploadButton.setOnClickListener(this)

    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        return gestureDetector.onTouchEvent(event)
    }

    private var gestureDetector =
        GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent?): Boolean {
                return true
            }

        })

    private var CAMERA: Int = 0
    @SuppressLint("CheckResult")
    @Synchronized
    override fun switchCameras() {
//        activity?.runOnUiThread {
//            selfieButton.isEnabled = false
//        }

        println("pre sync..")
        CAMERA = if (CAMERA == 0) 1 else 0

        synchronized(lock = this) {
            Single.fromCallable {
                println("Here")
                releaseCamera(tapToSave = false)
            }.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally {
                    engageCamera()
                }
                .subscribe({
                }, {
                })
        }
        presenter.determineViewsForCameraId()
    }

    var cameraIsClosed: Boolean = false
    override fun closeCamera() {
        cameraIsClosed = true
        try {
            cameraOpenCloseLock.acquire()
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

    @Throws(IOException::class)
    private fun setUpMediaRecorder() {
        val videoTimeStamp = System.currentTimeMillis().toString()
        videoFile = presenter.getVideoFilePath(videoTimeStamp)
        val rotation = activity?.windowManager?.defaultDisplay?.rotation
        val recorder = MediaRecorder()


        val profile =
            when (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_HIGH_SPEED_1080P)) {
                true -> CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH_SPEED_1080P)
                else -> CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH)
            }

        when (sensorOrientation) {
            SENSOR_ORIENTATION_DEFAULT_DEGREES ->
                recorder?.setOrientationHint(DEFAULT_ORIENTATIONS.get(rotation ?: 0))
            SENSOR_ORIENTATION_INVERSE_DEGREES ->
                recorder?.setOrientationHint(INVERSE_ORIENTATIONS.get(rotation ?: 0))
        }
        recorder?.apply {
            setAudioSource(MediaRecorder.AudioSource.CAMCORDER)
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.HE_AAC)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setOutputFile(videoFile?.absolutePath)
            setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight)
            setVideoEncodingBitRate(profile.videoBitRate)
            setAudioEncodingBitRate(profile.audioBitRate)
            setVideoFrameRate(profile.videoFrameRate)
        }
        mediaRecorder = recorder
    }

    @SuppressLint("CheckResult")
    override fun startRecording() {
        recording = true
        if (cameraDevice == null || txView?.isAvailable == false) {
            return
        }
        try {
            setUpMediaRecorder()
            mediaRecorder?.prepare()
            val texture = txView?.surfaceTexture.apply {
                this?.setDefaultBufferSize(
                    videoSize?.width ?: 0, videoSize?.height
                        ?: 0
                )
            }
            val previewSurface = Surface(texture)
            val recorderSurface = mediaRecorder?.surface
            val surfaces = ArrayList<Surface>().apply {
                add(previewSurface)
                recorderSurface?.let { s ->
                    add(s)
                }
            }

            if (!cameraIsClosed) {
                synchronized(this) {
                    previewRequestBuilder =
                        cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
                            this?.addTarget(previewSurface)
                            recorderSurface?.let { s ->
                                this?.addTarget(s)
                            }
                        } ?: return
                    cameraDevice?.createCaptureSession(
                        surfaces, object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                                captureSession = cameraCaptureSession
                                previewRequestBuilder.set(
                                    CaptureRequest.CONTROL_MODE,
                                    CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON
                                )
//                                previewRequestBuilder.set(
//                                    CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
//                                    CaptureRequest.CONTROL
//                                )
                                try {
                                    setUpCaptureRequestBuilder(previewRequestBuilder)
                                    HandlerThread("CameraPreview").start()
                                    startRecordingThread()
                                    captureSession?.setRepeatingRequest(
                                        previewRequestBuilder.build(),
                                        null, recordHandler
                                    )
                                    executeStart()

                                } catch (e: CameraAccessException) {
                                    Log.e("CameraMain", e.toString())
                                } catch (ise: IllegalStateException) {
                                }
                            }

                            override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                                showToast("Failed $cameraCaptureSession")
                            }
                        }, null
                    )
                }
            }

        } catch (e: CameraAccessException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun executeStart() {
        val runnable = Runnable {
            activity?.runOnUiThread {
                favoriteIcon?.let {
                    it.visibility = View.INVISIBLE
                }
                hahaIcon?.let {
                    it.visibility = View.INVISIBLE
                }
            }
            presenter.timerRanOut()
        }
        activity?.runOnUiThread {
            progress.visibility = View.GONE
        }
        mediaRecorder?.start()
        Handler().postDelayed(runnable, 5000)
    }

    @SuppressLint("CheckResult")
    override fun stopRecording(isPaused: Boolean) {
        stopRecordingThread()
        recording = false
        try {
            mediaRecorder?.stop()
            mediaRecorder?.reset()
            mediaRecorder = null
            when (isPaused) {
                false -> presenter.saveVideo(selectedEvent)
                else -> deleteUnsavedFile()
            }
        } catch (r: RuntimeException) {
            r.printStackTrace()
        } catch (e: java.lang.IllegalStateException) {
            e.printStackTrace()
        }
    }

    private fun resetIcons() {
        favoriteIcon.setImageDrawable(
            ResourcesCompat.getDrawable(
                resources,
                R.drawable.favebutton,
                null
            )
        )
        hahaIcon.setImageDrawable(
            ResourcesCompat.getDrawable(
                resources,
                R.drawable.haha,
                null
            )
        )

    }

    companion object {
        private const val TIMEOUT = 5000L
    }

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    fun setUpCaptureRequestBuilder(builder: CaptureRequest.Builder?) {
        builder?.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_camera, container, false)
    }

    var selectedEvent: Event? = null
    val listener: EventsClickListener = object : EventsClickListener {
        override fun onEventSelected(event: Event) {
            presenter.changeEvent(event.name ?: "")
            presenter.hideEvents()

            eventSpace.visibility = View.VISIBLE
            eventTitle.text = event.name

            selectedEvent = event
        }
    }

    private var evAdapter: EventsAdapter? = null
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        txView = cameraView
        presenter.setUpClicks()
        presenter.onCreate()
        getEventData()
        hiddenEvents.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)

    }


    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(p0: SurfaceTexture?, p1: Int, p2: Int) {
            openCamera(p1, p2, CAMERA)
        }

        override fun onSurfaceTextureSizeChanged(
            texture: SurfaceTexture,
            width: Int,
            height: Int
        ) {
        }

        override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture) = true
        override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) = Unit
    }


    fun engageCamera() {
        startBackgroundThread()
        println("TXVIEW :$txView")
        txView?.let {
            if (it.isAvailable) {
                cameraIsClosed = false
                println("opening $cameraIsClosed")
                openCamera(it.width, it.height, CAMERA)
            } else {
                it.surfaceTextureListener = surfaceTextureListener
            }
        }
    }

    override fun onPause() {
        releaseCamera(tapToSave = false)
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        if (pausedView.visibility == View.GONE && this.fragmentIsVisibleToUser != false) {
            paused = false
            progress.visibility = View.VISIBLE
            engageCamera()
        }

    }

    var selfieCameraEngaged: Boolean? = false
    private var paused: Boolean = false
    @SuppressLint("CheckResult")
    private fun releaseCamera(tapToSave: Boolean) {
        println("releasing camera..... $recording")
        synchronized(this) {
            Single.fromCallable {
                closeCamera()
                stopBackgroundThread()
            }.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally {
                    if (recording) {
                        saveText.visibility = View.GONE
                        presenter.clearProgressAnimation()
                        paused = !tapToSave
                        stopLiveView(paused, selfieCameraEngaged ?: false)
                    }
                }
                .subscribe({
                }, {
                })
        }

    }

    private fun getEventData() {
        presenter.getEvents()
    }

    private fun startLiveView() {

        if (CAMERA == 0) {
            activity?.runOnUiThread {
                saveText?.let {
                    it.visibility = View.VISIBLE
                }
            }
            presenter.animateProgressBar(
                saveText,
                progressBar,
                selectedEvent?.max_video_length ?: 12
            )
            startRecording()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onDestroy()
    }

    private fun stopLiveView(isPaused: Boolean, isSelfieCamera: Boolean) {
        println("STOPING LIVE VIEW....>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>")
        stopRecording(isPaused)

    }

    fun showToast(message: String) {
        Toast.makeText(
            context,
            message,
            Toast.LENGTH_LONG
        ).show()
    }


    var manager: CameraManager? = null
    @SuppressLint("MissingPermission")
    override fun openCamera(width: Int, height: Int, camera: Int) {
        manager = context?.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw RuntimeException("Time out waiting to lock overtimecamera opening.")
            }
            val cameraId = when (camera) {
                0 -> manager?.cameraIdList?.get(0)
                else -> manager?.cameraIdList?.get(1)
            } ?: "0"

            val characteristics = manager?.getCameraCharacteristics(cameraId)
            val map = characteristics?.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                ?: throw RuntimeException("Cannot get available preview/video sizes")
//            val frame = characteristics[CameraCharacteristics.SENSOR_INFO_MAX_FRAME_DURATION]
            sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
            videoSize = chooseVideoSize(map.getOutputSizes(MediaRecorder::class.java))

            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                txView?.setAspectRatio(videoSize?.width ?: 0, videoSize?.height ?: 0)
            } else {
                txView?.setAspectRatio(videoSize?.height ?: 0, videoSize?.width ?: 0)
            }

            mediaRecorder = MediaRecorder()
            txView?.setTransform(Matrix())
            manager?.openCamera(cameraId, cameraStateCallBack, backgroundHandler)

        } catch (e: CameraAccessException) {
            activity?.finishAffinity()
        } catch (e: NullPointerException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock overtimecamera opening.")
        }
    }

    private fun startBackgroundThread() {
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
    private fun stopRecordingThread() {
        recordThread?.quitSafely()
        try {
            recordThread?.join()
            recordThread = null
            recordHandler = null
        } catch (e: InterruptedException) {
            Log.e("CameraMain", e.toString())
        }
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            Log.e("CameraMain", e.toString())
        }
    }


    var fragmentIsVisibleToUser: Boolean? = false
    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        fragmentIsVisibleToUser = isVisibleToUser
        if (isVisibleToUser && view != null) {
            progress.visibility = View.VISIBLE
            engageCamera()
        }
    }

    private val cameraStateCallBack = object : CameraDevice.StateCallback() {
        override fun onOpened(cameraDevice: CameraDevice) {
            cameraOpenCloseLock.release()
            this@CameraFragment.cameraDevice = cameraDevice
            startLiveView()
        }

        override fun onDisconnected(cameraDevice: CameraDevice) {
            cameraOpenCloseLock.release()
            cameraDevice.close()
            this@CameraFragment.cameraDevice = null
        }

        override fun onError(cameraDevice: CameraDevice, error: Int) {
            cameraOpenCloseLock.release()
            cameraDevice.close()
            this@CameraFragment.cameraDevice = null
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

