package com.overtime.camera.camera

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import com.overtime.camera.R
import dagger.android.AndroidInjection
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


class CameraFragment : Fragment(), CameraInt, View.OnClickListener, View.OnTouchListener {
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        val sensorArraySize = mCameraInfo.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        val y = event?.y?.toInt()?.div(v?.width?.toFloat().times())
//
//
//        //TODO: here I just flip x,y, but this needs to correspond with the sensor orientation (via SENSOR_ORIENTATION)
//        val y = ((((event?.x)?.toInt())?.div(v?.width?.toFloat()!!))(( * sensorArraySize.height()));
//        val x = ((event.y).toInt()) / (float) view . getHeight ()) * (float) sensorArraySize . width ());
//        val halfTouchWidth = 150; //(int)motionEvent.getTouchMajor(); //TODO: this doesn't represent actual touch size in pixel. Values range in [3, 10]...
//        val halfTouchHeight = 150; //(int)motionEvent.getTouchMinor();
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.progressBar -> {

            }
        }
    }

    @Inject
    lateinit var presenter: CameraPresenter

    var backgroundThread: HandlerThread? = null
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

    var captureSession: CameraCaptureSession? = null
    lateinit var previewRequestBuilder: CaptureRequest.Builder
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

    @SuppressLint("MissingPermission")
    override fun openCamera(width: Int, height: Int) {
        val manager = context?.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw RuntimeException("Time out waiting to lock camera opening.")
            }
            val cameraId = manager.cameraIdList[0]

            val characteristics = manager.getCameraCharacteristics(cameraId)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    ?: throw RuntimeException("Cannot get available preview/video sizes")
            sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
            videoSize = presenter.chooseVideoSize(map.getOutputSizes(MediaRecorder::class.java))
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

            backgroundHandler?.let {
                manager.openCamera(cameraId, stateCallback, it)
            }

        } catch (e: CameraAccessException) {
            println("Camera Access Exception:::: ${e.printStackTrace()}")
            activity?.finishAffinity()
        } catch (e: NullPointerException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera opening.")
        }
    }

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

    val stateCallback = object : CameraDevice.StateCallback() {
        override fun onDisconnected(cameraDevice: CameraDevice) {
            cameraOpenCloseLock.release()
            this@CameraFragment.cameraDevice = null
            cameraDevice.close()
        }

        override fun onError(cameraDevice: CameraDevice, error: Int) {
            cameraOpenCloseLock.release()
            cameraDevice.close()
            println("Error $error")
            this@CameraFragment.cameraDevice = null
            activity?.finishAffinity()
        }

        override fun onOpened(cameraDevice: CameraDevice) {
            cameraOpenCloseLock.release()
            this@CameraFragment.cameraDevice = cameraDevice
            startPreview()
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
            throw RuntimeException("Interrupted while trying to lock camera closing.", e)
        } finally {
            cameraOpenCloseLock.release()
        }
    }

    override fun startPreview() {
        try {
            closePreviewSession()
            val texture = txView.surfaceTexture

            texture.setDefaultBufferSize(previewSize?.width ?: 0, previewSize?.height ?: 0)
            previewRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW) ?: return

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
                            //   showToast(getString(com.overtimetechnical.R.string.camera_fail))
                        }
                    }, backgroundHandler
            )
//            if (!clickedStop) {
//                playVideo()
//            }

        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    override fun updatePreview() {
        if (cameraDevice == null) return
        try {
            setUpCaptureRequestBuilder(previewRequestBuilder)
            HandlerThread("CameraPreview").start()
            captureSession?.setRepeatingRequest(
                    previewRequestBuilder.build(),
                    null, backgroundHandler
            )
        } catch (e: CameraAccessException) {
            Log.e("CameraMain", e.toString())
        }
    }

    private fun setUpCaptureRequestBuilder(builder: CaptureRequest.Builder?) {
        builder?.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
    }


    override fun startRecording() {
        println("pre try:::::::")

        if (cameraDevice == null || !txView.isAvailable) {
            println("camera device: " + cameraDevice)
            return
        }
        try {
            println(" start:::::::")
            closePreviewSession()
            setUpMediaRecorder()
            val texture = textureView.surfaceTexture.apply {
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


            cameraDevice?.createCaptureSession(
                    surfaces,
                    object : CameraCaptureSession.StateCallback() {

                        override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                            captureSession = cameraCaptureSession
                            updatePreview()
                            activity?.runOnUiThread {
                                recording = true
                                mediaRecorder?.start()
                            }
                        }

                        override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {}
                    }, backgroundHandler
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun startProgressAnimation() {
        val countDown = object : CountDownTimer(18000, 1000) {
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
        countDown.start()
    }

    override fun stopRecording() {
        recording = false
        mediaRecorder?.apply {
            stop()
            reset()
        }
        //  showToast(getString(R.string.camera_video_saved))
        presenter.startPreview()
    }


    override fun closePreviewSession() {
        captureSession?.close()
        captureSession = null
    }


    private var param1: String? = null
    private var param2: String? = null
    private val SENSOR_ORIENTATION_DEFAULT_DEGREES = 90
    private val SENSOR_ORIENTATION_INVERSE_DEGREES = 270

    lateinit var txView: TXView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_camera, container, false)
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        txView = textureView
        progressBar.setOnClickListener(this)
        startProgressAnimation()
        txView.setOnTouchListener(this)
    }

    val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(p0: SurfaceTexture?, p1: Int, p2: Int) {
            presenter.openCamera(p1, p2)
        }

        override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {}
        override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture) = true
        override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) = Unit
    }

    override fun onResume() {
        super.onResume()
        startBackgroundThread()
        if (txView.isAvailable) {
            presenter.openCamera(txView.width, txView.height)
        } else {

            txView.surfaceTextureListener = surfaceTextureListener
        }
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground")
        backgroundThread?.start()
        backgroundHandler = Handler(backgroundThread?.looper)
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
        videoFile = presenter.getVideoFilePath("new_video")
        val rotation = activity?.windowManager?.defaultDisplay?.rotation
        when (sensorOrientation) {
            SENSOR_ORIENTATION_DEFAULT_DEGREES ->
                rotation?.let { DEFAULT_ORIENTATIONS.get(it) }?.let { mediaRecorder?.setOrientationHint(it) }
            SENSOR_ORIENTATION_INVERSE_DEGREES ->
                rotation?.let { INVERSE_ORIENTATIONS.get(it) }?.let { mediaRecorder?.setOrientationHint(it) }
        }

        mediaRecorder?.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(videoFile?.absolutePath)
            setVideoEncodingBitRate(10000000)
            setVideoFrameRate(60)
            setVideoSize(videoSize?.width ?: 0, videoSize?.height ?: 0)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            prepare()
        }
    }

    override fun onPause() {
        closeCamera()
        stopBackgroundThread()
//        if (recording) {
//            clickedStop = true
//           // stopVideo()
//        }
        super.onPause()
    }


    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
                CameraFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_PARAM1, param1)
                        putString(ARG_PARAM2, param2)
                    }
                }
    }
}

class Compare : Comparator<Size> {
    override fun compare(lhs: Size, rhs: Size) =
            Long.signum(lhs.width.toLong() * lhs.height - rhs.width.toLong() * rhs.height)
}