package com.itsovertime.overtimecamera.play.camera

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.CamcorderProfile
import android.media.MediaRecorder
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.*
import android.view.animation.LinearInterpolator
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.itsovertime.overtimecamera.R
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_camera.*
import java.io.File
import java.io.IOException
import java.lang.Long
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"


class CameraFragment : Fragment(), CameraInt, View.OnClickListener {

    @Inject
    lateinit var presenter: CameraPresenter

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
    var clickedStop: Boolean = false
    var previewSize: Size? = null
    var sensorOrientation = 0
    var videoFile: File? = null
    var mediaRecorder: MediaRecorder? = null
    lateinit var txView: TXView
    var captureSession: CameraCaptureSession? = null
    lateinit var previewRequestBuilder: CaptureRequest.Builder


    override fun stopRecording() {
        stopRecordingThread()
        recording = false
        try {
            mediaRecorder?.apply {
                stop()
                reset()
            }
            presenter.startPreview()
        } catch (r: RuntimeException) {
            r.printStackTrace()
        } catch (e: java.lang.IllegalStateException) {
            e.printStackTrace()
        }


    }


    override fun startRecording() {
        println("pre try:::::::")
        recording = true
        if (cameraDevice == null || !txView.isAvailable) {
            println("overtimecamera device: " + cameraDevice)
            return
        }
        try {
            println(" start:::::::")
            closePreviewSession()
            setUpMediaRecorder()
            val texture = txView.surfaceTexture.apply {
                setDefaultBufferSize(previewSize?.width ?: 0, previewSize?.height ?: 0)
            }

            val previewSurface = Surface(texture)
            val recorderSurface = mediaRecorder?.surface
            val surfaces = ArrayList<Surface>().apply {
                add(previewSurface)
                recorderSurface?.let { add(it) }
            }
            previewRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
                this?.addTarget(previewSurface)
                recorderSurface?.let {
                    this?.addTarget(it)
                }

            } ?: return

            println("overtimecamera device... $cameraDevice")
            cameraDevice?.createCaptureSession(
                surfaces, object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                        println("create capture....")
                        captureSession = cameraCaptureSession
                        previewRequestBuilder.set(
                            CaptureRequest.CONTROL_MODE,
                            CaptureRequest.CONTROL_SCENE_MODE_ACTION
                        )
                        try {
                            setUpCaptureRequestBuilder(previewRequestBuilder)
                            HandlerThread("CameraPreview").start()
                            println("pre rec thread")
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
                        activity?.run {
                            println("started media recorder.....")
                            mediaRecorder?.start()
                        }
                    }

                    override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                        showToast("Failed")
                    }
                }, null
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }


    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    var countDown: CountDownTimer? = null
    private fun startProgressAnimation() {
        countDown = object : CountDownTimer(18000, 1000) {
            override fun onFinish() {
                progressBar.progress = 100
            }

            override fun onTick(millisUntilFinished: kotlin.Long) {
                val time = millisUntilFinished / 1000
                ObjectAnimator.ofInt(progressBar, "progress", time.toInt()).apply {
                    duration = time
                    interpolator = LinearInterpolator()
                    start()
                }
                //progressBar.setProgress(time.toInt(), true)
            }
        }
        (countDown as CountDownTimer).start()
    }


    override fun startPreview() {
        try {
            closePreviewSession()
            val texture = txView.surfaceTexture
            texture.setDefaultBufferSize(previewSize?.width ?: 0, previewSize?.height ?: 0)
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
                        //  showToast(getString(com.overtimetechnical.R.string.camera_fail))
                    }
                }, backgroundHandler
            )

            if (!clickedStop) {
                playVideo()
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

    }

    override fun closePreviewSession() {
        captureSession?.close()
        captureSession = null
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


    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.tapToSave ->
                if (recording) {
                    clickedStop = true
                    stopVideo()
                } else {
                    playVideo()
                }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_camera, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        txView = cameraView
        progressBar.setOnClickListener(this)
        tapToSave.setOnClickListener(this)
    }

    val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(p0: SurfaceTexture?, p1: Int, p2: Int) {
            openCamera(p1, p2)
        }

        override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {}
        override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture) = true
        override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) = Unit
    }

    override fun onResume() {
        super.onResume()
        startBackgroundThread()
        if (txView.isAvailable) {
            openCamera(txView.width, txView.height)
        } else {
            txView.surfaceTextureListener = surfaceTextureListener
        }

    }

    override fun onPause() {
        closeCamera()
        stopBackgroundThread()
        if (recording) {
            clickedStop = true
            stopVideo()
        }
        super.onPause()
    }

    fun playVideo() {
        showToast("STARTED!!")
        //startRecording()
        //   startProgressAnimation()
    }

    fun stopVideo() {
        showToast("STOPPED!")
        // stopRecording()
//        (countDown as CountDownTimer).cancel()
    }

    fun showToast(message: String) {
        Toast.makeText(
            context,
            message,
            Toast.LENGTH_LONG
        ).show()
    }

    @SuppressLint("MissingPermission")
    override fun openCamera(width: Int, height: Int) {
        val manager = context?.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw RuntimeException("Time out waiting to lock overtimecamera opening.")
            }
            val cameraId = manager.cameraIdList[0]

            val characteristics = manager.getCameraCharacteristics(cameraId)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                ?: throw RuntimeException("Cannot get available preview/video sizes")
            sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
            videoSize = chooseVideoSize(map.getOutputSizes(MediaRecorder::class.java))
            videoSize?.let {
                previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture::class.java), width, height, it)
            }

            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                txView.setAspectRatio(previewSize?.width ?: 0, previewSize?.height ?: 0)
            } else {
                txView.setAspectRatio(previewSize?.height ?: 0, previewSize?.width ?: 0)
            }
            mediaRecorder = MediaRecorder()
            txView.setTransform(Matrix())
            println("manager open start:::::::")
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
    private fun setUpMediaRecorder() {
        val time = System.currentTimeMillis().toString()
        videoFile = presenter.getVideoFilePath(time)
        val rotation = activity?.windowManager?.defaultDisplay?.rotation ?: return
        when (sensorOrientation) {
            SENSOR_ORIENTATION_DEFAULT_DEGREES ->
                mediaRecorder?.setOrientationHint(DEFAULT_ORIENTATIONS.get(rotation))
            SENSOR_ORIENTATION_INVERSE_DEGREES ->
                mediaRecorder?.setOrientationHint(INVERSE_ORIENTATIONS.get(rotation))
        }

        val profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH_SPEED_1080P)
        mediaRecorder?.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFile(videoFile?.absolutePath)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setVideoEncodingBitRate(profile.videoBitRate)
            setVideoFrameRate(profile.videoFrameRate)
            setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight)
            setAudioEncodingBitRate(profile.audioBitRate)
            setAudioEncoder(MediaRecorder.AudioEncoder.HE_AAC)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setCaptureRate(120.0)
            prepare()
        }
    }

    private val cameraStateCallBack = object : CameraDevice.StateCallback() {
        override fun onOpened(cameraDevice: CameraDevice) {
            cameraOpenCloseLock.release()
            println("overtimecamera open not null")
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


}

class Compare : Comparator<Size> {
    override fun compare(lhs: Size, rhs: Size) =
        Long.signum(lhs.width.toLong() * lhs.height - rhs.width.toLong() * rhs.height)
}
