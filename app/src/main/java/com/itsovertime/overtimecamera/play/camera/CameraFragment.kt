package com.itsovertime.overtimecamera.play.camera

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.graphics.drawable.Drawable
import android.hardware.camera2.*
import android.icu.text.SimpleDateFormat
import android.media.CamcorderProfile
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.util.Log
import android.util.Range
import android.util.Size
import android.util.SparseIntArray
import android.view.*
import android.widget.Chronometer
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.itsovertime.overtimecamera.play.BuildConfig
import com.itsovertime.overtimecamera.play.R
import com.itsovertime.overtimecamera.play.events.EventsAdapter
import com.itsovertime.overtimecamera.play.events.EventsClickListener
import com.itsovertime.overtimecamera.play.itemsame.BasicDiffCallback
import com.itsovertime.overtimecamera.play.model.Event
import com.itsovertime.overtimecamera.play.model.Tagged_Teams
import com.itsovertime.overtimecamera.play.onboarding.OnboardingActivity
import com.itsovertime.overtimecamera.play.uploads.UploadsActivity
import com.itsovertime.overtimecamera.play.userpreference.UserPreference
import dagger.android.support.AndroidSupportInjection
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.event_view.*
import kotlinx.android.synthetic.main.events_recycler_view.*
import kotlinx.android.synthetic.main.fragment_camera.*
import kotlinx.android.synthetic.main.tagged_players_view.*
import kotlinx.android.synthetic.main.upload_button_view.*
import java.io.File
import java.io.IOException
import java.lang.Long
import java.time.Instant
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class CameraFragment : Fragment(), CameraInt, View.OnClickListener, View.OnTouchListener {


    override fun setUpDefaultEvent(event: Event?) {
        eventTitle.text = event?.name ?: "Unknown Event"
        selectedEvent = event
        setUpTaggedUsersView(event)
    }

    override fun hideEventsRV() {
        hiddenEvents.visibility = View.GONE
    }

    override fun openEvents() {
        hiddenEvents.visibility = View.VISIBLE
    }

    var taggedAthletesArray = arrayListOf<String>()
    private val taggedListener: TaggedAthleteClickListener = object : TaggedAthleteClickListener {
        override fun onAtheleteSelected(id: String) {
            taggedAthletesArray.add(id)
        }

    }
    var taggedAdapter = TaggedPlayersAdapter(listener = taggedListener)
    var newData = mutableListOf<TaggedPlayersPresentation>()
    var old = taggedAdapter.data?.data ?: emptyList()
    override fun setUpEventViewData(eventList: MutableList<Event>?) {
        if (eventList.isNullOrEmpty()) {
            eventSpace.visibility = View.GONE
        }
        evAdapter = EventsAdapter(eventList, listener)
        eventsRecycler.adapter = evAdapter
        eventList?.forEach {
            setUpTaggedUsersView(it)
        }
    }

    val listener: EventsClickListener = object : EventsClickListener {
        override fun onEventSelected(event: Event) {
            presenter.changeEvent(event)
            presenter.hideEvents()
            eventSpace.visibility = View.VISIBLE
            selectedEvent = event


            setUpTaggedUsersView(event)
        }
    }

    private fun setUpTaggedUsersView(event: Event?) {
        old = newData
        newData.clear()
        event?.tagged_users?.forEach {
            newData.add(TaggedPlayersPresentation(it))
        }
        if (newData.isEmpty()) {
            taggedView.visibility = View.GONE
        }

        taggedAdapter.data =
            TaggedPlayersData(newData, DiffUtil.calculateDiff(BasicDiffCallback(old, newData)))
        athleteRecycler.adapter = taggedAdapter
    }

    override fun showOrHideViewsForCamera() {
        when (CAMERA) {
            1 -> {
                tapToSave.background = null
                tapToSave.setImageResource(R.drawable.selfie_record_red)
                selfieCameraEngaged = true
                favoriteIcon.visibility = View.INVISIBLE
                hahaIcon.visibility = View.INVISIBLE
                progressBar.visibility = View.GONE
                pauseButton.visibility = View.GONE
                bottomView.visibility = View.INVISIBLE
                topView.visibility = View.INVISIBLE
                eventSpace.visibility = View.GONE
                selfieMsg.visibility = View.VISIBLE
                selfieTimer.visibility = View.VISIBLE
                taggedView.visibility = View.GONE
            }
            0 -> {
                eventSpace.visibility = View.VISIBLE
                taggedView.visibility = View.VISIBLE
                selfieTimer.visibility = View.GONE
                selfieMsg.visibility = View.GONE
                pauseButton.visibility = View.VISIBLE
                bottomView.visibility = View.VISIBLE
                topView.visibility = View.VISIBLE
                tapToSave.setImageResource(R.drawable.tap)
                selfieCameraEngaged = false
                favoriteIcon.visibility = View.VISIBLE
                hahaIcon.visibility = View.VISIBLE
                progressBar.visibility = View.VISIBLE
            }
        }
    }

    override fun stopProgressAnimation() {
        progressBar?.clearAnimation()
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
            R.id.cancel -> hideEventsRV()
            R.id.eventTitle -> presenter.displayHiddenView()
            R.id.tapToSave -> {
                progress.visibility = View.VISIBLE
                when (CAMERA) {
                    1 -> tapToSaveSelfie()
                    else -> tapToSaveRegularRecording()
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
                progress.visibility = View.VISIBLE
                presenter.cameraSwitch()
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
                releaseCamera(tapToSave = false)
                startActivity(Intent(context, UploadsActivity::class.java))
            }
        }
    }

    private fun tapToSaveRegularRecording() {
        println("TAP TO SAVE....")
        favoriteIcon.visibility = View.VISIBLE
        hahaIcon.visibility = View.VISIBLE

        if (!newData.isNullOrEmpty()) {
            taggedAdapter.notifyDataSetChanged()
            taggedView.visibility = View.VISIBLE
        }
        if (recording) {
            releaseCamera(tapToSave = true)
            resetIcons()
        }
    }

    var count: Int = 0
    private fun tapToSaveSelfie() {
        if (!recording) {
            progress.visibility = View.GONE
            recording = true
            selfieTimer.base = SystemClock.elapsedRealtime();
            tapToSave.setImageResource(R.drawable.selfie_record_red_stop)
            selfieMsg.visibility = View.GONE
            mediaRecorder?.start()
            selfieTimer.start()
            selfieTimer.onChronometerTickListener =
                Chronometer.OnChronometerTickListener {
                    count++
                    if (count == 30) {
                        progressBar.visibility = View.VISIBLE
                        tapToSaveSelfie()
                        count = 0
                    }
                }
        } else {
            selfieMsg.visibility = View.VISIBLE
            tapToSave.setImageResource(R.drawable.selfie_record_red)
            selfieTimer.base = SystemClock.elapsedRealtime();
            selfieTimer.stop()
            releaseCamera(tapToSave = true)
        }
    }

    private var callback: UploadsButtonClick? = null
    fun setUploadsClickListener(listener: UploadsButtonClick) {
        this.callback = listener
    }

    private fun deleteUnsavedFile() {
        println("DELETING FILE!")
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
        cancel.setOnClickListener(this)
        hahaIcon.setOnClickListener(this)
        eventTitle.setOnClickListener(this)
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
        println("pre sync..")
        CAMERA = if (CAMERA == 0) 1 else 0
        selfieCameraEngaged = CAMERA != 0

        println("Camera is... $CAMERA")
        synchronized(lock = this) {
            deleteUnsavedFile()
            Single.fromCallable {
                releaseCamera(tapToSave = false)
            }.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    presenter.determineViewsForCameraId()
                    engageCamera()
                }, {
                })
        }

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
                recorder.setOrientationHint(DEFAULT_ORIENTATIONS.get(rotation ?: 0))
            SENSOR_ORIENTATION_INVERSE_DEGREES ->
                recorder.setOrientationHint(INVERSE_ORIENTATIONS.get(rotation ?: 0))
        }
        recorder.apply {
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
    override fun prepareCameraForRecording() {
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
                                previewRequestBuilder.apply {
                                    set(
                                        CaptureRequest.CONTROL_MODE,
                                        CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON
                                    )
                                    set(
                                        CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                                        Range<Int>(60, 60)
                                    )
                                }

                                try {
                                    setUpCaptureRequestBuilder(previewRequestBuilder)
                                    HandlerThread("CameraPreview").start()
                                    startRecordingThread()
                                    captureSession?.setRepeatingRequest(
                                        previewRequestBuilder.build(),
                                        null, recordHandler
                                    )

                                    startMediaRecorder()

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

    var runnable: Runnable? = null
    private fun startMediaRecorder() {
        when (CAMERA) {
            0 -> {
                println("======================== ${taggedAthletesArray.size}")
                favoriteIcon?.let {
                    if (it.visibility == View.VISIBLE) {
                        runnable = Runnable {
                            activity?.runOnUiThread {
                                favoriteIcon?.let {
                                    it.visibility = View.INVISIBLE
                                }
                                hahaIcon?.let {
                                    it.visibility = View.INVISIBLE
                                }
                                taggedView?.let {
                                    it.visibility = View.INVISIBLE
                                }
                                if (taggedAthletesArray.isNotEmpty()) {
                                    presenter.updateTaggedAthletesField(taggedAthletesArray)
                                    taggedAthletesArray.clear()
                                }
                            }
                        }
                    }
                }
                mediaRecorder?.start()
                Handler().postDelayed(runnable, 5000)
            }
            1 -> {
                recording = false
            }
        }

        activity?.runOnUiThread {
            progress.visibility = View.GONE
        }
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


    private var evAdapter: EventsAdapter? = null
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        txView = cameraView
        presenter.setUpClicks()
        presenter.onCreate()
        getEventData()
        eventsRecycler.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        athleteRecycler.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        detectOrientation()

        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        val params = navSpace.layoutParams as ConstraintLayout.LayoutParams
        if (resourceId > 0) {
            params.height = resources.getDimensionPixelSize(resourceId)
        } else {
            params.height = 0
        }

        navSpace.layoutParams = params
    }

    var orientation: OrientationEventListener? = null
    private fun detectOrientation() {
        orientation = object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                when (orientation) {
                    in 0..65 -> {
                        showWarnings()
                    }
                    in 360 downTo 290 -> {
                        showWarnings()
                    }
                    in 65..165 -> {
                        hideWarnings()
                    }
                    in 290 downTo 235 -> {
                        hideWarnings()
                    }
                    else -> {
                        showWarnings()
                    }
                }
            }

        }
        if (!BuildConfig.DEBUG) {
            orientation?.enable()
        }

    }

    fun showWarnings() {
        rotateWarning?.let {
            it.visibility = View.VISIBLE
        }
    }

    fun hideWarnings() {
        rotateWarning?.let {
            it.visibility = View.GONE
        }
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
        println("ON RESUME FROM FRAGMENT")
        if (pausedView.visibility == View.GONE && this.fragmentIsVisibleToUser != false) {
            paused = false
            progress.visibility = View.VISIBLE
            engageCamera()
        }
        orientation?.let {
            if (it.canDetectOrientation()) {
                it.enable()
            }
        }
    }

    var selfieCameraEngaged: Boolean? = false
    private var paused: Boolean = false
    @SuppressLint("CheckResult")
    private fun releaseCamera(tapToSave: Boolean) {
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
                        stopRecording(paused)
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
        }
        prepareCameraForRecording()

    }

    override fun onDestroy() {
        releaseCamera(tapToSave = false)
        deleteUnsavedFile()
        super.onDestroy()
        presenter.onDestroy()
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
                0 -> {
                    manager?.cameraIdList?.get(0)
                }
                else -> manager?.cameraIdList?.get(1)
            } ?: "0"

            println("WE ARE HERE.... $cameraId")
            val characteristics = manager?.getCameraCharacteristics(cameraId)
            val map = characteristics?.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                ?: throw RuntimeException("Cannot get available preview/video sizes")
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
            println("OPENED!! $cameraDevice")
            this@CameraFragment.cameraDevice = cameraDevice
            startLiveView()
        }

        override fun onDisconnected(cameraDevice: CameraDevice) {
            cameraOpenCloseLock.release()
            println("DISCONNECT!! $cameraDevice")
            cameraDevice.close()
            this@CameraFragment.cameraDevice = null
            callback?.onRefreshFragmentFromDisconnect()
        }

        override fun onError(cameraDevice: CameraDevice, error: Int) {
            cameraOpenCloseLock.release()
            println("ERROR FROM CAMERA ::$error")
            cameraDevice.close()
            this@CameraFragment.cameraDevice = null
            activity?.finishAffinity()
        }
    }

    private fun chooseVideoSize(choices: Array<Size>) = choices.firstOrNull {
        it.width == it.height * 4 / 3 && it.width <= 1080
    } ?: choices[choices.size - 1]


    interface UploadsButtonClick {
        fun onUploadsButtonClicked()
        fun onRefreshFragmentFromDisconnect()
    }
}

class Compare : Comparator<Size> {
    override fun compare(lhs: Size, rhs: Size) =
        Long.signum(lhs.width.toLong() * lhs.height - rhs.width.toLong() * rhs.height)
}

