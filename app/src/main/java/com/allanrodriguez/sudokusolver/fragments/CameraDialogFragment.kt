package com.allanrodriguez.sudokusolver.fragments

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.hardware.camera2.*
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.Image
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.*
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProviders
import com.allanrodriguez.sudokusolver.R
import com.allanrodriguez.sudokusolver.viewmodels.CameraDialogViewModel
import com.allanrodriguez.sudokusolver.views.AutoFitTextureView
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.sign
import kotlinx.android.synthetic.main.fragment_camera_dialog.*

class CameraDialogFragment : DialogFragment() {

    //region Properties
    private val maxPreviewWidth: Int = 1920
    private val maxPreviewHeight: Int = 1080
    private val cameraOpenCloseLock: Semaphore = Semaphore(1)
    private val orientations: SparseIntArray = SparseIntArray()

    private var cameraCaptureSession: CameraCaptureSession? = null
    private var cameraDevice: CameraDevice? = null
    private var cameraId: String? = null
    private var backgroundHandler: Handler? = null
    private var backgroundThread: HandlerThread? = null
    private var imageReader: ImageReader? = null
    private var isFlashSupported: Boolean = false
    private var previewSize: Size? = null
    private var sensorOrientation: Int = 0
    private var state: CameraState = CameraState.PREVIEW
    private var textureView: AutoFitTextureView? = null
    private var isImagePreviewShowing = false

    private lateinit var file: File
    private lateinit var previewRequest: CaptureRequest
    private lateinit var previewRequestBuilder: CaptureRequest.Builder
    private lateinit var viewModel: CameraDialogViewModel
    //endregion

    init {
        orientations.append(Surface.ROTATION_0, 90)
        orientations.append(Surface.ROTATION_90, 0)
        orientations.append(Surface.ROTATION_180, 270)
        orientations.append(Surface.ROTATION_270, 180)
    }

    //region Lifecycle methods
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_camera_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        textureView = camera_preview
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(CameraDialogViewModel::class.java)

        camera_dialog_toolbar.setNavigationOnClickListener { closeDialog() }

        button_take_picture.setOnClickListener {
            button_take_picture.isClickable = false
            takePicture()
        }

        button_accept.setOnClickListener {
            val imageRect = Rect(camera_preview.left, camera_preview.top, camera_preview.right, camera_preview.bottom)
            val squareRect = Rect(camera_window.left, camera_window.top, camera_window.right, camera_window.bottom)

            val fragment: Fragment = ParseOcrFragment.newInstance(file, imageRect, squareRect)

            closeDialog()

            fragmentManager?.beginTransaction()
                    ?.setCustomAnimations(R.anim.fade_in,
                            R.anim.fade_out,
                            R.anim.fade_in,
                            R.anim.fade_out)
                    ?.add(android.R.id.content, fragment, ParseOcrFragment::class.java.simpleName)
                    ?.addToBackStack(null)
                    ?.commit()
        }
        button_retake.setOnClickListener { cancelImagePreview() }

        file = File(context?.cacheDir, "sudoku.jpg")
    }

    override fun onStart() {
        super.onStart()

        if (resources.getBoolean(R.bool.large_layout)) {
            context?.resources?.displayMetrics?.let {
                val width: Int = (it.widthPixels * 0.75).toInt()
                val height: Int = (it.heightPixels * 0.75).toInt()

                dialog.window?.setLayout(width, height)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        startBackgroundThread()

        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).
        if (textureView?.isAvailable == true) {
            openCamera(textureView?.width as Int, textureView?.height as Int)
        } else {
            textureView?.surfaceTextureListener = surfaceTextureListener
        }

        if (isImagePreviewShowing) {
            cancelImagePreview()
        }
    }

    override fun onPause() {
        closeCamera()
        stopBackgroundThread()

        super.onPause()
    }
    //endregion

    private fun brightenScreens() {
        val from: Int = ContextCompat.getColor(context as Context, R.color.black)
        val to: Int = ContextCompat.getColor(context as Context, R.color.transparentBlack)

        val colorTransition: ValueAnimator = ValueAnimator.ofArgb(from, to).apply {
            duration = 250
            addUpdateListener {
                val color = ColorDrawable(it.animatedValue as Int)

                top_screen.background = color
                left_screen.background = color
                right_screen.background = color
                bottom_screen.background = color
            }
        }

        colorTransition.start()
    }

    private fun cancelImagePreview() {
        unlockFocus()
        brightenScreens()
        camera_directions.visibility = VISIBLE
        showTakePictureButton()
        isImagePreviewShowing = false
    }

    private fun captureStillPicture() {
        try {
            val activity: FragmentActivity? = activity
            if (activity == null || cameraDevice == null) {
                return
            }

            // This is the CaptureRequest.Builder that we use to take a picture.
            val captureBuilder: CaptureRequest.Builder
                    = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE) as CaptureRequest.Builder
            captureBuilder.addTarget(imageReader?.surface as Surface)

            // Use the same AE and AF modes as the preview.
            captureBuilder[CaptureRequest.CONTROL_AF_MODE] = CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE

            // Orientation
            val rotation: Int = activity.windowManager.defaultDisplay.rotation
            captureBuilder[CaptureRequest.JPEG_ORIENTATION] = getOrientation(rotation)

            val captureCallback = object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
                    Log.d(tag, "Saved image to file at $file")

                    isImagePreviewShowing = true

                    activity.runOnUiThread {
                        camera_directions.visibility = INVISIBLE
                        darkenScreens()
                        showAcceptRetakeButtons()
                    }
                }
            }

            cameraCaptureSession?.stopRepeating()
            cameraCaptureSession?.abortCaptures()
            cameraCaptureSession?.capture(captureBuilder.build(), captureCallback, null)
        } catch (ex: CameraAccessException) {
            ex.printStackTrace()
        }
    }

    /**
     * Given `choices` of `Size`s supported by a camera, choose the smallest one that
     * is at least as large as the respective texture view size, and that is at most as large as the
     * respective max size, and whose aspect ratio matches with the specified value. If such size
     * doesn't exist, choose the largest one that is at most as large as the respective max size,
     * and whose aspect ratio matches with the specified value.
     *
     * @param choices           The list of sizes that the camera supports for the intended output
     * class
     * @param textureViewWidth  The width of the texture view relative to sensor coordinate
     * @param textureViewHeight The height of the texture view relative to sensor coordinate
     * @param maxWidth          The maximum width that can be chosen
     * @param maxHeight         The maximum height that can be chosen
     * @param aspectRatio       The aspect ratio
     * @return The optimal `Size`, or an arbitrary one if none were big enough
     */
    private fun chooseOptimalSize(choices: Array<Size>,
                                  textureViewWidth: Int,
                                  textureViewHeight: Int,
                                  maxWidth: Int,
                                  maxHeight: Int,
                                  aspectRatio: Size): Size {

        // Collect the supported resolutions that are at least as big as the preview Surface
        val bigEnough: MutableList<Size> = mutableListOf()
        // Collect the supported resolutions that are smaller than the preview Surface
        val notBigEnough: MutableList<Size> = mutableListOf()
        val w: Int = aspectRatio.width
        val h: Int = aspectRatio.height
        for (option: Size in choices) {
            if (option.width <= maxWidth
                    && option.height <= maxHeight
                    && option.height == option.width * h / w) {
                if (option.width >= textureViewWidth && option.height >= textureViewHeight) {
                    bigEnough.add(option)
                } else {
                    notBigEnough.add(option)
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        when {
            bigEnough.isNotEmpty() -> return Collections.min(bigEnough, CompareSizesByArea())
            notBigEnough.isNotEmpty() -> return Collections.max(notBigEnough, CompareSizesByArea())
            else -> Log.e(tag, "Couldn't find any suitable preview size.")
        }

        return choices[0]
    }

    private fun closeCamera() {
        try {
            cameraOpenCloseLock.acquire()

            cameraCaptureSession?.close()
            cameraCaptureSession = null

            cameraDevice?.close()
            cameraDevice = null

            imageReader?.close()
            imageReader = null
        } catch (ex: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera closing.", ex)
        } finally {
            cameraOpenCloseLock.release()
        }
    }

    private fun closeDialog() {
        if (resources.getBoolean(R.bool.large_layout)) {
            dismiss()
        } else {
            fragmentManager?.popBackStack()
        }
    }

    private fun configureTransform(viewWidth: Int, viewHeight: Int) {
        val activity: FragmentActivity? = activity

        if (textureView == null || previewSize == null || activity == null) {
            return
        }

        val rotation: Int = activity.windowManager.defaultDisplay.rotation
        val matrix = Matrix()
        val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
        val bufferRect = RectF(0f, 0f,
                previewSize?.height?.toFloat() as Float, previewSize?.width?.toFloat() as Float)
        val centerX: Float = viewRect.centerX()
        val centerY: Float = viewRect.centerY()

        if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)

            val scale: Float = max(viewHeight.toFloat() / previewSize?.height as Int,
                    viewWidth.toFloat() / previewSize?.width as Int)
            matrix.postScale(scale, scale, centerX, centerY)
            matrix.postRotate(90f * (rotation - 2f), centerX, centerY)
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180f, centerX, centerY)
        }

        textureView?.setTransform(matrix)
    }

    private fun createCameraPreviewSession() {
        try {
            val surfaceTexture: SurfaceTexture = textureView?.surfaceTexture as SurfaceTexture

            // We configure the size of default buffer to be the size of camera preview we want.
            surfaceTexture.setDefaultBufferSize(previewSize?.width as Int, previewSize?.height as Int)

            // This is the output Surface we need to start preview.
            val surface = Surface(surfaceTexture)

            // We set up a CaptureRequest.Builder with the output Surface.
            previewRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW) as CaptureRequest.Builder
            previewRequestBuilder.addTarget(surface)

            // Here, we create a CameraCaptureSession for camera preview.
            cameraDevice?.createCaptureSession(listOf(surface, imageReader?.surface),
                    captureSessionStateCallback,
                    null)
        } catch (ex: CameraAccessException) {
            ex.printStackTrace()
        }
    }

    private fun darkenScreens() {
        val from: Int = ContextCompat.getColor(context as Context, R.color.transparentBlack)
        val to: Int = ContextCompat.getColor(context as Context, R.color.black)

        val colorTransition: ValueAnimator = ValueAnimator.ofArgb(from, to).apply {
            duration = 250
            addUpdateListener {
                val color = ColorDrawable(it.animatedValue as Int)

                top_screen.background = color
                left_screen.background = color
                right_screen.background = color
                bottom_screen.background = color
            }
        }
        colorTransition.start()
    }

    private fun getOrientation(rotation: Int): Int {
        // Sensor orientation is 90 for most devices, or 270 for some devices (e.g. Nexus 5X)
        // We have to take that into account and rotate JPEG properly.
        // For devices with orientation of 90, we simply return our mapping from ORIENTATIONS.
        // For devices with orientation of 270, we need to rotate the JPEG 180 degrees.
        return (orientations[rotation] + sensorOrientation + 270) % 360
    }

    private fun lockFocus() {
        try {
            previewRequestBuilder[CaptureRequest.CONTROL_AF_TRIGGER] = CameraMetadata.CONTROL_AF_TRIGGER_START
            state = CameraState.WAITING_LOCK
            cameraCaptureSession?.capture(previewRequestBuilder.build(), captureSessionCaptureCallback, backgroundHandler)
        } catch (ex: CameraAccessException) {
            ex.printStackTrace()
        }
    }

    private fun openCamera(surfaceWidth: Int, surfaceHeight: Int) {
        setUpCameraOutputs(surfaceWidth, surfaceHeight)
        configureTransform(surfaceWidth, surfaceHeight)

        val cameraManager: CameraManager = context?.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        try {
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw RuntimeException("Time out waiting to lock camera opening.")
            }

            cameraManager.openCamera(cameraId as String, deviceStateCallback, backgroundHandler)
        } catch (ex: CameraAccessException) {
            ex.printStackTrace()
        } catch (ex: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera opening.", ex)
        } catch (ex: SecurityException) {
            throw RuntimeException("Permission was not granted to use the camera.", ex)
        }
    }

    private fun runPrecaptureSequence() {
        try {
            // This is how to tell the camera to trigger.
            previewRequestBuilder[CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER] = CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START

            // Tell #captureSessionCaptureCallback to wait for the precapture sequence to be set.
            state = CameraState.WAITING_PRECAPTURE
            cameraCaptureSession?.capture(previewRequestBuilder.build(), captureSessionCaptureCallback, backgroundHandler)
        } catch (ex: CameraAccessException) {
            ex.printStackTrace()
        }
    }

    private fun setUpCameraOutputs(width: Int, height: Int) {
        val cameraManager: CameraManager = context?.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraList: Array<String> = cameraManager.cameraIdList

        try {
            for (cameraId: String in cameraList) {
                val characteristics: CameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId)
                val lensDirection: Int? = characteristics[CameraCharacteristics.LENS_FACING]

                // No front-facing cameras wanted!
                if (lensDirection == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue
                }

                val map: StreamConfigurationMap? = characteristics[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]
                        ?: continue

                // For still image captures, we use the largest available size.
                val largest: Size = Collections.max(map?.getOutputSizes(ImageFormat.JPEG)?.toList(), CompareSizesByArea())
                imageReader = ImageReader.newInstance(largest.width, largest.height, ImageFormat.JPEG, 2)
                imageReader?.setOnImageAvailableListener(::onImageAvailable, backgroundHandler)

                // Find out if we need to swap dimension to get the preview size relative to sensor coordinates.
                val displayRotation: Int = activity?.windowManager?.defaultDisplay?.rotation as Int
                sensorOrientation = characteristics[CameraCharacteristics.SENSOR_ORIENTATION] as Int
                var swappedDimensions = false

                when (displayRotation) {
                    Surface.ROTATION_0, Surface.ROTATION_180 -> {
                        if (sensorOrientation == 90 || sensorOrientation == 270) {
                            swappedDimensions = true
                        }
                    }
                    Surface.ROTATION_90, Surface.ROTATION_270 -> {
                        if (sensorOrientation == 0 || sensorOrientation == 180) {
                            swappedDimensions = true
                        }
                    }
                    else -> Log.e(tag, "Display rotation is invalid: $displayRotation")
                }

                val displaySize = Point()
                activity?.windowManager?.defaultDisplay?.getSize(displaySize)

                var rotatedPreviewWidth: Int = width
                var rotatedPreviewHeight: Int = height
                var maxPreviewWidth: Int = displaySize.x
                var maxPreviewHeight: Int = displaySize.y

                if (swappedDimensions) {
                    rotatedPreviewWidth = height
                    rotatedPreviewHeight = width
                    maxPreviewWidth = displaySize.y
                    maxPreviewHeight = displaySize.x
                }

                if (maxPreviewWidth > this.maxPreviewWidth) {
                    maxPreviewWidth = this.maxPreviewWidth
                }

                if (maxPreviewHeight > this.maxPreviewHeight) {
                    maxPreviewHeight = this.maxPreviewHeight
                }

                previewSize = chooseOptimalSize(map?.getOutputSizes(SurfaceTexture::class.java) as Array<Size>,
                        rotatedPreviewWidth,
                        rotatedPreviewHeight,
                        maxPreviewWidth,
                        maxPreviewHeight,
                        largest)

                val orientation: Int = resources.configuration.orientation
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    textureView?.setAspectRatio(previewSize?.width as Int, previewSize?.height as Int)
                } else {
                    textureView?.setAspectRatio(previewSize?.height as Int, previewSize?.width as Int)
                }

                val available: Boolean? = characteristics[CameraCharacteristics.FLASH_INFO_AVAILABLE]
                isFlashSupported = available ?: false

                this.cameraId = cameraId

                return
            }
        } catch (ex: CameraAccessException) {
            ex.printStackTrace()
        } catch (ex: NullPointerException) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            // TODO: Show error dialog
        }
    }

    private fun showAcceptRetakeButtons() {
        button_take_picture.hide()
        button_retake.show()
        button_accept.show()
    }

    private fun showTakePictureButton() {
        button_retake.hide()
        button_accept.hide()
        button_take_picture.isClickable = true
        button_take_picture.show()
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground")
        backgroundThread?.start()
        backgroundHandler = Handler(backgroundThread?.looper)
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (ex: InterruptedException) {
            ex.printStackTrace()
        }
    }

    private fun takePicture() {
        lockFocus()
    }

    private fun unlockFocus() {
        try {
            previewRequestBuilder[CaptureRequest.CONTROL_AF_TRIGGER] = CameraMetadata.CONTROL_AF_TRIGGER_CANCEL
            cameraCaptureSession?.capture(previewRequestBuilder.build(), captureSessionCaptureCallback, backgroundHandler)

            // After this, the camera will go back to the normal state of preview.
            state = CameraState.PREVIEW
            cameraCaptureSession?.setRepeatingRequest(previewRequest, captureSessionCaptureCallback, backgroundHandler)
        } catch (ex: CameraAccessException) {
            ex.printStackTrace()
        }
    }

    //region Callback implementations
    private val captureSessionCaptureCallback = object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
            process(result)
        }

        override fun onCaptureProgressed(session: CameraCaptureSession, request: CaptureRequest, partialResult: CaptureResult) {
            process(partialResult)
        }

        private fun process(result: CaptureResult) {
            when (state) {
                CameraState.PREVIEW -> {
                    // We have nothing to do when the camera preview is working normally.
                }
                CameraState.WAITING_LOCK -> {
                    val afState: Int? = result[CaptureResult.CONTROL_AF_STATE]

                    if (afState == null) {
                        captureStillPicture()
                    } else if (afState == CaptureRequest.CONTROL_AF_STATE_FOCUSED_LOCKED
                            || afState == CaptureRequest.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED) {
                        // CONTROL_AE_STATE can be null on some devices
                        val aeState: Int? = result[CaptureResult.CONTROL_AE_STATE]
                        if (aeState == null || aeState == CaptureRequest.CONTROL_AE_STATE_CONVERGED) {
                            state = CameraState.PICTURE_TAKEN
                            captureStillPicture()
                        } else {
                            runPrecaptureSequence()
                        }
                    }
                }
                CameraState.WAITING_PRECAPTURE -> {
                    // CONTROL_AE_STATE can be null on some devices
                    val aeState: Int? = result[CaptureResult.CONTROL_AE_STATE]
                    if (aeState == null
                            || aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE
                            || aeState == CaptureResult.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        state = CameraState.WAITING_NON_PRECAPTURE
                    }
                }
                CameraState.WAITING_NON_PRECAPTURE -> {
                    // CONTROL_AE_STATE can be null on some devices
                    val aeState: Int? = result[CaptureResult.CONTROL_AE_STATE]
                    if (aeState == null
                            || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        state = CameraState.PICTURE_TAKEN
                        captureStillPicture()
                    }
                }
                else -> return
            }
        }
    }

    private val captureSessionStateCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigured(session: CameraCaptureSession) {
            // The camera is already closed.
            if (cameraDevice == null) {
                return
            }

            // When the session is ready, we start displaying the preview.
            cameraCaptureSession = session

            try {
                // Auto focus should be continuous for camera preview.
                previewRequestBuilder[CaptureRequest.CONTROL_AF_MODE] = CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE

                // TODO: Add flash support here.

                // Finally, we start displaying the camera preview.
                previewRequest = previewRequestBuilder.build()
                cameraCaptureSession?.setRepeatingRequest(previewRequest, captureSessionCaptureCallback, backgroundHandler)
            } catch (ex: CameraAccessException) {
                ex.printStackTrace()
            }
            Log.i(tag, "CameraCaptureSession configured successfully.")
        }

        override fun onConfigureFailed(session: CameraCaptureSession) {
            // TODO: Show error dialog.
            Log.e(tag, "CameraCaptureSession configuration failed.")
        }
    }

    private val deviceStateCallback = object : CameraDevice.StateCallback() {
        override fun onDisconnected(camera: CameraDevice) {
            cameraOpenCloseLock.release()
            camera.close()
            cameraDevice = null
            Log.i(tag, "CameraDevice was disconnected.")
        }

        override fun onError(camera: CameraDevice, error: Int) {
            cameraOpenCloseLock.release()
            camera.close()
            cameraDevice = null
            closeDialog()
            // TODO: Show error dialog here.
            Log.e(tag, "An error occurred with the CameraDevice.")
        }

        override fun onOpened(camera: CameraDevice) {
            // This method is called when the camera is opened.  We start camera preview here.
            cameraOpenCloseLock.release()
            cameraDevice = camera
            createCameraPreviewSession()
            Log.i(tag, "CameraDevice was opened.")
        }
    }

    private fun onImageAvailable(reader: ImageReader) {
        backgroundHandler?.post {
            val image: Image = reader.acquireNextImage()
            val buffer: ByteBuffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())

            buffer.get(bytes)

            FileOutputStream(file).use {
                try {
                    it.write(bytes)
                } catch (ex: IOException) {
                    ex.printStackTrace()
                } finally {
                    image.close()
                }
            }
        }
    }

    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
            openCamera(width, height)
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
            return true
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
            configureTransform(width, height)
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
        }
    }
    //endregion

    companion object {
        fun newInstance() = CameraDialogFragment()
    }

    private class CompareSizesByArea : Comparator<Size> {

        override fun compare(o1: Size?, o2: Size?): Int {
            val areaDiff: Long = (o1?.width as Int).toLong() * o1.height - (o2?.width as Int).toLong() * o2.height

            return areaDiff.sign
        }
    }

    private enum class CameraState {
        PREVIEW,
        WAITING_LOCK,
        WAITING_PRECAPTURE,
        WAITING_NON_PRECAPTURE,
        PICTURE_TAKEN
    }
}