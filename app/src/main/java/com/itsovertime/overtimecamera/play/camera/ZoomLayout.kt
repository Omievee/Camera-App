package com.itsovertime.overtimecamera.play.camera

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.OnScaleGestureListener
import android.view.View
import android.widget.FrameLayout


class ZoomLayout : FrameLayout, OnScaleGestureListener {
    private enum class Mode {
        NONE, DRAG, ZOOM
    }

    private var mode = Mode.NONE
    private var scale = 1.0f
    private var lastScaleFactor = 0f
    // Where the finger first  touches the screen
    private var startX = 0f
    private var startY = 0f
    // How much to translate the canvas
    private var dx = 0f
    private var dy = 0f
    private var prevDx = 0f
    private var prevDy = 0f

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init(context)
    }

    private fun init(context: Context) {
        val scaleDetector = ScaleGestureDetector(context, this)
        setOnTouchListener { view, motionEvent ->
            if (motionEvent.pointerCount == 2) {
                when (motionEvent.action and MotionEvent.ACTION_MASK) {
                    MotionEvent.ACTION_DOWN -> {
                        Log.i(TAG, "DOWN")
                        if (scale > MIN_ZOOM) {
                            mode = Mode.DRAG
                            startX = motionEvent.x - prevDx
                            startY = motionEvent.y - prevDy
                        }
                    }
                    MotionEvent.ACTION_MOVE -> if (mode == Mode.DRAG && motionEvent.pointerCount ==2) {
                        dx = motionEvent.x - startX
                        dy = motionEvent.y - startY
                    }
                    MotionEvent.ACTION_POINTER_DOWN -> mode = Mode.ZOOM
                    MotionEvent.ACTION_POINTER_UP -> mode = Mode.NONE
                    MotionEvent.ACTION_UP -> {
                        Log.i(TAG, "UP")
                        mode = Mode.DRAG
                        prevDx = dx
                        prevDy = dy
                    }
                }
            }
            scaleDetector.onTouchEvent(motionEvent)
            if (mode == Mode.DRAG && scale >= MIN_ZOOM || mode == Mode.ZOOM) {
                parent.requestDisallowInterceptTouchEvent(true)
                val maxDx: Float =
                    (child().width - child().width / scale) / 2 * scale
                val maxDy: Float =
                    (child().height - child().height / scale) / 2 * scale
                dx = dx.coerceAtLeast(-maxDx).coerceAtMost(maxDx)
                dy = dy.coerceAtLeast(-maxDy).coerceAtMost(maxDy)
                Log.i(
                    TAG,
                    "Width: " + child().getWidth().toString() + ", scale " + scale.toString() + ", dx " + dx
                        .toString() + ", max " + maxDx
                )
                applyScaleAndTranslation()
            }
            true
        }
    }

    // ScaleGestureDetector
    override fun onScaleBegin(scaleDetector: ScaleGestureDetector): Boolean {
        Log.i(TAG, "onScaleBegin")
        return true
    }

    override fun onScale(scaleDetector: ScaleGestureDetector): Boolean {
        val scaleFactor = scaleDetector.scaleFactor
        Log.i(TAG, "onScale$scaleFactor")
        if (lastScaleFactor == 0f || Math.signum(scaleFactor) == Math.signum(
                lastScaleFactor
            )
        ) {
            scale *= scaleFactor
            scale = Math.max(
                MIN_ZOOM,
                Math.min(scale, MAX_ZOOM)
            )
            lastScaleFactor = scaleFactor
        } else {
            lastScaleFactor = 0f
        }
        return true
    }

    override fun onScaleEnd(scaleDetector: ScaleGestureDetector) {
        Log.i(TAG, "onScaleEnd")
    }

    private fun applyScaleAndTranslation() {
        child().scaleX = scale
        child().scaleY = scale
        child().translationX = dx
        child().translationY = dy
    }

    private fun child(): View {
        return getChildAt(0)
    }

    companion object {
        private const val TAG = "ZoomLayout"
        private const val MIN_ZOOM = 1.0f
        private const val MAX_ZOOM = 4.0f
    }
}