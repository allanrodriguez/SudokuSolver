package com.allanrodriguez.sudokusolver.activities

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.ColorDrawable
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.util.Rational
import android.view.*
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.camera.core.*
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.allanrodriguez.sudokusolver.R
import com.allanrodriguez.sudokusolver.abstractions.ANIMATION_FAST_MILLIS
import com.allanrodriguez.sudokusolver.abstractions.ANIMATION_SLOW_MILLIS
import com.allanrodriguez.sudokusolver.utilities.AutoFitPreviewBuilder
import com.allanrodriguez.sudokusolver.utilities.simulateClick
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.lang.ref.WeakReference

private const val TAG = "CameraActivity"

class CameraActivity : AppCompatActivity() {

    private lateinit var displayManager: DisplayManager
    private lateinit var imagePreview: ImageView
    private lateinit var rootView: CoordinatorLayout
    private lateinit var viewFinder: TextureView

    private var displayId = -1
    private var imageCapture: ImageCapture? = null
    private var preview: Preview? = null

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayChanged(displayId: Int) {
            if (displayId == this@CameraActivity.displayId) {
                preview?.setTargetRotation(rootView.display.rotation)
                imageCapture?.setTargetRotation(rootView.display.rotation)
            }
        }

        override fun onDisplayAdded(displayId: Int) = Unit

        override fun onDisplayRemoved(displayId: Int) = Unit
    }

    private val onImageCaptured = object : ImageCapture.OnImageCapturedListener() {
        override fun onCaptureSuccess(image: ImageProxy?, rotationDegrees: Int) {
            image?.let { ip ->
                val buffer = ip.planes[0].buffer
                val imageBytes = ByteArray(buffer.capacity())
                buffer.get(imageBytes)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                val mat = Matrix()
                mat.postRotate(rotationDegrees.toFloat())
                val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, mat, true)
                imagePreview.setImageBitmap(rotatedBitmap)
                imagePreview.visibility = View.VISIBLE
            }
        }

        override fun onError(useCaseError: ImageCapture.UseCaseError?, message: String?, cause: Throwable?) {
            Log.e(TAG, "Photo capture failed: $message")
            cause?.printStackTrace()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_camera)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_close_white_24dp)
        }

        if (resources.getBoolean(R.bool.large_layout)) {
            val metrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(metrics)
            window.setLayout((0.8 * metrics.widthPixels).toInt(), (0.8 * metrics.heightPixels).toInt())
        }

        imagePreview = findViewById(R.id.image_preview)
        rootView = findViewById(R.id.root)
        viewFinder = findViewById(R.id.viewfinder)

        displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        displayManager.registerDisplayListener(displayListener, null)

        viewFinder.post {
            displayId = viewFinder.display.displayId
            bindCameraUseCases()
        }
    }

    override fun onDestroy() {
        displayManager.unregisterDisplayListener(displayListener)
        super.onDestroy()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                val shutter = findViewById<FloatingActionButton>(R.id.button_take_picture)
                shutter.simulateClick()
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun onShutterButtonClick(v: View?) {
        imageCapture?.takePicture(onImageCaptured)
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            rootView.postDelayed({
                rootView.foreground = ColorDrawable(Color.WHITE)
                rootView.postDelayed({ rootView.foreground = null }, ANIMATION_FAST_MILLIS)
            }, ANIMATION_SLOW_MILLIS)
        }
    }

    private fun bindCameraUseCases() {
        CameraX.unbindAll()

        val metrics = DisplayMetrics().also { d -> viewFinder.display.getRealMetrics(d) }
        val screenAspectRatio = Rational(metrics.widthPixels, metrics.heightPixels)

        Log.d(TAG, "Metrics: ${metrics.widthPixels} x ${metrics.heightPixels}")

        val viewFinderConfig = PreviewConfig.Builder().apply {
            setLensFacing(CameraX.LensFacing.BACK)
            setTargetAspectRatio(screenAspectRatio)
            setTargetRotation(viewFinder.display.rotation)
        }.build()

        preview = AutoFitPreviewBuilder(viewFinderConfig, WeakReference(viewFinder)).useCase

        val imageCaptureConfig = ImageCaptureConfig.Builder().apply {
            setLensFacing(CameraX.LensFacing.BACK)
            setCaptureMode(ImageCapture.CaptureMode.MAX_QUALITY)
            setTargetAspectRatio(screenAspectRatio)
            setTargetRotation(viewFinder.display.rotation)
        }.build()

        imageCapture = ImageCapture(imageCaptureConfig)

        CameraX.bindToLifecycle(this, preview, imageCapture)
    }
}
