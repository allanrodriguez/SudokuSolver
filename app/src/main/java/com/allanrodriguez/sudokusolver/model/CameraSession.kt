package com.allanrodriguez.sudokusolver.model

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.*
import android.hardware.camera2.*
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.Surface
import android.view.TextureView
import com.allanrodriguez.sudokusolver.abstractions.FlashState
import com.allanrodriguez.sudokusolver.views.AutoFitTextureView
import java.io.*
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.sign

class CameraSession(private val activity: Activity, private val textureView: AutoFitTextureView) {

    //region Properties
    val file: File = File(activity.cacheDir, "sudoku.jpg")

    var isStopped = false
        private set

    private val cameraOpenCloseLock: Semaphore = Semaphore(1)
    private val maxPreviewWidth: Int = 1920
    private val maxPreviewHeight: Int = 1080
    private val onErrorListeners: MutableList<() -> Unit> = mutableListOf()
    private val onFlashSupportChangedListeners: MutableList<(Boolean) -> Unit> = mutableListOf()
    private val onPictureAvailableListeners: MutableList<() -> Unit> = mutableListOf()
    private val orientations: SparseIntArray = SparseIntArray()

    private var backgroundHandler: Handler? = null
    private var backgroundThread: HandlerThread? = null
    private var cameraCaptureSession: CameraCaptureSession? = null
    private var cameraDevice: CameraDevice? = null
    private var imageReader: ImageReader? = null

    private var isFlashSupported: Boolean = false
        private set(value) {
            if (field != value) {
                field = value
                onFlashSupportChanged()
            }
        }

    private var sensorOrientation: Int = 0
    private var state: CameraState = CameraState.PREVIEW

    private lateinit var cameraId: String
    private lateinit var previewRequest: CaptureRequest
    private lateinit var previewRequestBuilder: CaptureRequest.Builder
    private lateinit var previewSize: Size
    //endregion

    init {
        orientations.append(Surface.ROTATION_0, 90)
        orientations.append(Surface.ROTATION_90, 0)
        orientations.append(Surface.ROTATION_180, 270)
        orientations.append(Surface.ROTATION_270, 180)
    }

    fun addOnErrorListener(listener: () -> Unit) {
        if (!onErrorListeners.contains(listener)) {
            onErrorListeners.add(listener)
        }
    }

    fun removeOnErrorListener(listener: () -> Unit): Boolean {
        return onErrorListeners.remove(listener)
    }

    fun addOnFlashSupportChangedListener(listener: (Boolean) -> Unit) {
        if (!onFlashSupportChangedListeners.contains(listener)) {
            onFlashSupportChangedListeners.add(listener)
        }
    }

    fun removeOnFlashSupportChangedListener(listener: (Boolean) -> Unit): Boolean {
        return onFlashSupportChangedListeners.remove(listener)
    }

    fun addOnPictureAvailableListener(listener: () -> Unit) {
        if (!onPictureAvailableListeners.contains(listener)) {
            onPictureAvailableListeners.add(listener)
        }
    }

    fun removeOnPictureAvailableListener(listener: () -> Unit): Boolean {
        return onPictureAvailableListeners.remove(listener)
    }

    fun start() {
        startBackgroundThread()

        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).
        if (textureView.isAvailable) {
            openCamera(textureView.width, textureView.height)
        } else {
            textureView.surfaceTextureListener = SurfaceTextureListener()
        }

        isStopped = false
    }

    fun stop() {
        closeCamera()
        stopBackgroundThread()
        isStopped = true
    }

    fun deletePicture(): Boolean {
        if (file.exists()) {
            return file.delete()
        }

        return false
    }

    fun setFlash(flashState: FlashState): Boolean {
        if (isFlashSupported) {
            return false
        }

        previewRequestBuilder[CaptureRequest.CONTROL_AE_MODE] = when (flashState) {
            FlashState.OFF -> CaptureRequest.CONTROL_AE_MODE_ON
            FlashState.ON -> CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH
            FlashState.AUTO -> CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
        }

        return true
    }

    fun takePicture() {
        lockFocus()
    }

    private fun captureStillPicture() {
        try {
            if (cameraDevice == null) {
                return
            }

            // This is the CaptureRequest.Builder that we use to take a picture.
            val captureBuilder: CaptureRequest.Builder =
                cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)!!.apply {
                    addTarget(imageReader?.surface as Surface)
                }

            // Use the same AE and AF modes as the preview.
            captureBuilder[CaptureRequest.CONTROL_AF_MODE] = CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE

            // Orientation
            val rotation: Int = activity.windowManager.defaultDisplay.rotation
            captureBuilder[CaptureRequest.JPEG_ORIENTATION] = getOrientation(rotation)

            val captureCallback = object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    Log.d(TAG, "Saved image to file at $file")
                    onPictureAvailable()
                    unlockFocus()
                }
            }

            cameraCaptureSession?.stopRepeating()
            cameraCaptureSession?.abortCaptures()
            cameraCaptureSession?.capture(captureBuilder.build(), captureCallback, null)
        } catch (ex: CameraAccessException) {
            logStackTrace(ex)
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
    private fun chooseOptimalSize(
        choices: Array<Size>,
        textureViewWidth: Int,
        textureViewHeight: Int,
        maxWidth: Int,
        maxHeight: Int,
        aspectRatio: Size
    ): Size {
        // Collect the supported resolutions that are at least as big as the preview Surface
        val bigEnough: MutableList<Size> = mutableListOf()

        // Collect the supported resolutions that are smaller than the preview Surface
        val notBigEnough: MutableList<Size> = mutableListOf()

        val w: Int = aspectRatio.width
        val h: Int = aspectRatio.height

        for (option: Size in choices) {
            if (option.width <= maxWidth && option.height <= maxHeight && option.height == option.width * h / w) {
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
            else -> Log.e(TAG, "Couldn't find any suitable preview size.")
        }

        return choices[0]
    }

    private fun openCamera(surfaceWidth: Int, surfaceHeight: Int) {
        setUpCameraOutputs(surfaceWidth, surfaceHeight)
        configureTransform(surfaceWidth, surfaceHeight)

        val cameraManager: CameraManager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        try {
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw RuntimeException("Time out waiting to lock camera opening.")
            }

            cameraManager.openCamera(cameraId, CameraDeviceStateCallback(), backgroundHandler)
        } catch (ex: CameraAccessException) {
            logStackTrace(ex)
        } catch (ex: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera opening.", ex)
        } catch (ex: SecurityException) {
            throw RuntimeException("Permission was not granted to use the camera.", ex)
        }
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

    private fun configureTransform(viewWidth: Int, viewHeight: Int) {
        val rotation: Int = activity.windowManager.defaultDisplay.rotation
        val matrix = Matrix()

        val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
        val bufferRect = RectF(0f, 0f, previewSize.height.toFloat(), previewSize.width.toFloat())

        val centerX: Float = viewRect.centerX()
        val centerY: Float = viewRect.centerY()

        if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)

            val scale: Float = max(
                viewHeight.toFloat() / previewSize.height,
                viewWidth.toFloat() / previewSize.width
            )

            matrix.postScale(scale, scale, centerX, centerY)
            matrix.postRotate(90f * (rotation - 2f), centerX, centerY)
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180f, centerX, centerY)
        }

        textureView.setTransform(matrix)
    }

    private fun createCameraPreviewSession() {
        try {
            val surfaceTexture: SurfaceTexture = textureView.surfaceTexture as SurfaceTexture

            // We configure the size of default buffer to be the size of camera preview we want.
            surfaceTexture.setDefaultBufferSize(previewSize.width, previewSize.height)

            // This is the output Surface we need to start preview.
            val surface = Surface(surfaceTexture)

            // We set up a CaptureRequest.Builder with the output Surface.
            previewRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)!!.apply {
                addTarget(surface)
            }

            // Here, we create a CameraCaptureSession for camera preview.
            cameraDevice?.createCaptureSession(
                listOf(surface, imageReader?.surface),
                CameraCaptureSessionStateCallback(),
                null
            )
        } catch (ex: CameraAccessException) {
            logStackTrace(ex)
        }
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
            cameraCaptureSession?.capture(
                previewRequestBuilder.build(),
                CameraCaptureSessionCaptureCallback(),
                backgroundHandler
            )
        } catch (ex: CameraAccessException) {
            logStackTrace(ex)
        }
    }

    private fun unlockFocus() {
        try {
            previewRequestBuilder[CaptureRequest.CONTROL_AF_TRIGGER] = CameraMetadata.CONTROL_AF_TRIGGER_CANCEL
            cameraCaptureSession?.capture(
                previewRequestBuilder.build(),
                CameraCaptureSessionCaptureCallback(),
                backgroundHandler
            )

            // After this, the camera will go back to the normal state of preview.
            state = CameraState.PREVIEW
            cameraCaptureSession?.setRepeatingRequest(
                previewRequest,
                CameraCaptureSessionCaptureCallback(),
                backgroundHandler
            )
        } catch (ex: CameraAccessException) {
            logStackTrace(ex)
        }
    }

    private fun logStackTrace(ex: Exception) {
        val stackTrace = StringWriter()
        ex.printStackTrace(PrintWriter(stackTrace))
        Log.e(TAG, stackTrace.toString())
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
                    logStackTrace(ex)
                } finally {
                    image.close()
                }
            }
        }
    }

    private fun onError() {
        for (listener: () -> Unit in onErrorListeners) {
            listener()
        }
    }

    private fun onFlashSupportChanged() {
        for (listener: (Boolean) -> Unit in onFlashSupportChangedListeners) {
            listener(isFlashSupported)
        }
    }

    private fun onPictureAvailable() {
        for (listener: () -> Unit in onPictureAvailableListeners) {
            listener()
        }
    }

    private fun runPrecaptureSequence() {
        try {
            // This is how to tell the camera to trigger.
            previewRequestBuilder[CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER] =
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START

            // Tell #captureSessionCaptureCallback to wait for the precapture sequence to be set.
            state = CameraState.WAITING_PRECAPTURE
            cameraCaptureSession?.capture(
                previewRequestBuilder.build(),
                CameraCaptureSessionCaptureCallback(),
                backgroundHandler
            )
        } catch (ex: CameraAccessException) {
            logStackTrace(ex)
        }
    }

    private fun setUpCameraOutputs(width: Int, height: Int) {
        val cameraManager: CameraManager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraList: Array<String> = cameraManager.cameraIdList

        try {
            for (cameraId: String in cameraList) {
                val characteristics: CameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId)
                val lensDirection: Int? = characteristics[CameraCharacteristics.LENS_FACING]

                // No front-facing cameras wanted!
                if (lensDirection == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue
                }

                val map: StreamConfigurationMap? =
                    characteristics[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP] ?: continue

                // For still image captures, we use the largest available size.
                val largest: Size =
                    Collections.max(map?.getOutputSizes(ImageFormat.JPEG)?.toList(), CompareSizesByArea())
                imageReader = ImageReader.newInstance(largest.width, largest.height, ImageFormat.JPEG, 2).apply {
                    setOnImageAvailableListener(::onImageAvailable, backgroundHandler)
                }

                // Find out if we need to swap dimension to get the preview size relative to sensor coordinates.
                val displayRotation: Int = activity.windowManager?.defaultDisplay?.rotation as Int
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
                    else -> Log.e(TAG, "Display rotation is invalid: $displayRotation")
                }

                val displaySize = Point()
                activity.windowManager?.defaultDisplay?.getSize(displaySize)

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

                previewSize = chooseOptimalSize(
                    map?.getOutputSizes(SurfaceTexture::class.java) as Array<Size>,
                    rotatedPreviewWidth,
                    rotatedPreviewHeight,
                    maxPreviewWidth,
                    maxPreviewHeight,
                    largest
                )

                val orientation: Int = activity.resources.configuration.orientation
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    textureView.setAspectRatio(previewSize.width, previewSize.height)
                } else {
                    textureView.setAspectRatio(previewSize.height, previewSize.width)
                }

                isFlashSupported = characteristics[CameraCharacteristics.FLASH_INFO_AVAILABLE] ?: false
                this.cameraId = cameraId

                return
            }
        } catch (ex: CameraAccessException) {
            logStackTrace(ex)
        } catch (ex: NullPointerException) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            onError()
        }
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraSessionBackground")
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
            logStackTrace(ex)
        }
    }

    //region Helper classes
    private inner class CameraCaptureSessionCaptureCallback : CameraCaptureSession.CaptureCallback() {

        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            process(result)
        }

        override fun onCaptureProgressed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            partialResult: CaptureResult
        ) {
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
                        || afState == CaptureRequest.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED
                    ) {
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
                        || aeState == CaptureResult.CONTROL_AE_STATE_FLASH_REQUIRED
                    ) {
                        state = CameraState.WAITING_NON_PRECAPTURE
                    }
                }
                CameraState.WAITING_NON_PRECAPTURE -> {
                    // CONTROL_AE_STATE can be null on some devices
                    val aeState: Int? = result[CaptureResult.CONTROL_AE_STATE]
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        state = CameraState.PICTURE_TAKEN
                        captureStillPicture()
                    }
                }
                else -> return
            }
        }
    }

    private inner class CameraCaptureSessionStateCallback : CameraCaptureSession.StateCallback() {

        override fun onConfigured(session: CameraCaptureSession) {
            // The camera is already closed.
            if (cameraDevice == null) {
                return
            }

            // When the session is ready, we start displaying the preview.
            cameraCaptureSession = session

            try {
                // Auto focus should be continuous for camera preview.
                previewRequestBuilder[CaptureRequest.CONTROL_AF_MODE] =
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE

                // Finally, we start displaying the camera preview.
                previewRequest = previewRequestBuilder.build()
                cameraCaptureSession?.setRepeatingRequest(
                    previewRequest,
                    CameraCaptureSessionCaptureCallback(),
                    backgroundHandler
                )
            } catch (ex: CameraAccessException) {
                logStackTrace(ex)
            }
            Log.i(TAG, "CameraCaptureSession configured successfully.")
        }

        override fun onConfigureFailed(session: CameraCaptureSession) {
            Log.e(TAG, "CameraCaptureSession configuration failed.")
            onError()
        }
    }

    private inner class CameraDeviceStateCallback : CameraDevice.StateCallback() {

        override fun onDisconnected(camera: CameraDevice) {
            cameraOpenCloseLock.release()
            camera.close()
            cameraDevice = null
            Log.i(TAG, "CameraDevice was disconnected.")
        }

        override fun onError(camera: CameraDevice, error: Int) {
            cameraOpenCloseLock.release()
            camera.close()
            cameraDevice = null
            Log.e(TAG, "An error occurred with the CameraDevice.: $error")
            onError()
        }

        override fun onOpened(camera: CameraDevice) {
            // This method is called when the camera is opened.  We start camera preview here.
            cameraOpenCloseLock.release()
            cameraDevice = camera
            createCameraPreviewSession()
            Log.i(TAG, "CameraDevice was opened.")
        }
    }

    private enum class CameraState {
        PREVIEW,
        WAITING_LOCK,
        WAITING_PRECAPTURE,
        WAITING_NON_PRECAPTURE,
        PICTURE_TAKEN
    }

    private class CompareSizesByArea : Comparator<Size> {

        override fun compare(o1: Size?, o2: Size?): Int {
            val areaDiff: Long = (o1?.width as Int).toLong() * o1.height - (o2?.width as Int).toLong() * o2.height

            return areaDiff.sign
        }
    }

    private inner class SurfaceTextureListener : TextureView.SurfaceTextureListener {

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
        const val TAG = "CameraSession"
    }
}