package com.allanrodriguez.sudokusolver.utilities

import android.content.Context
import android.graphics.Matrix
import android.hardware.display.DisplayManager
import android.util.Size
import android.view.*
import androidx.camera.core.Preview
import androidx.camera.core.PreviewConfig
import java.lang.ref.WeakReference
import java.util.*
import kotlin.math.roundToInt

class AutoFitPreviewBuilder(config: PreviewConfig, viewFinderRef: WeakReference<TextureView>) {

    val useCase = Preview(config)

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit

        override fun onDisplayRemoved(displayId: Int) = Unit

        override fun onDisplayChanged(displayId: Int) {
            val viewFinder: TextureView = viewFinderRef.get() ?: return
            if (displayId == viewFinderDisplay) {
                val display: Display = displayManager.getDisplay(displayId)
                val rotation: Int? = getDisplaySurfaceRotation(display)
                updateTransform(viewFinder, rotation, bufferDimens, viewFinderDimens)
            }
        }
    }

    private var bufferDimens: Size = Size(0, 0)
    private var bufferRotation: Int = 0
    private var viewFinderDimens: Size = Size(0, 0)
    private var viewFinderDisplay: Int = -1
    private var viewFinderRotation: Int? = null
    private var displayManager: DisplayManager

    init {
        val viewFinder: TextureView = viewFinderRef.get()
                ?: throw IllegalArgumentException("Invalid reference to viewfinder used.")

        viewFinderDisplay = viewFinder.display.displayId
        viewFinderRotation = getDisplaySurfaceRotation(viewFinder.display) ?: 0

        useCase.onPreviewOutputUpdateListener = Preview.OnPreviewOutputUpdateListener { p ->
            val vf: TextureView = viewFinderRef.get() ?: return@OnPreviewOutputUpdateListener

            val parent = vf.parent as ViewGroup
            parent.removeView(vf)
            parent.addView(vf, 0)

            vf.surfaceTexture = p.surfaceTexture
            bufferRotation = p.rotationDegrees
            val rotation: Int? = getDisplaySurfaceRotation(vf.display)
            updateTransform(vf, rotation, p.textureSize, viewFinderDimens)
        }

        viewFinder.addOnLayoutChangeListener { view, left, top, right, bottom, _, _, _, _ ->
            val vf = view as TextureView
            val newViewFinderDimens = Size(right - left, bottom - top)
            val rotation: Int? = getDisplaySurfaceRotation(viewFinder.display)
            updateTransform(vf, rotation, bufferDimens, newViewFinderDimens)
        }

        displayManager = viewFinder.context
                .getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        displayManager.registerDisplayListener(displayListener, null)

        viewFinder.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View?) {
                displayManager.registerDisplayListener(displayListener, null)
            }

            override fun onViewDetachedFromWindow(view: View?) {
                displayManager.unregisterDisplayListener(displayListener)
            }
        })
    }

    private fun getDisplaySurfaceRotation(display: Display?): Int? {
        return when (display?.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> null
        }
    }

    private fun updateTransform(textureView: TextureView?, rotation: Int?, newBufferDimens: Size, newViewFinderDimens: Size) {
        textureView ?: return

        if (rotation == viewFinderRotation
                && Objects.equals(newBufferDimens, bufferDimens)
                && Objects.equals(newViewFinderDimens, viewFinderDimens)) {
            return
        }

        rotation ?: return
        viewFinderRotation = rotation

        if (newBufferDimens.height * newBufferDimens.width <= 0) {
            return
        }

        bufferDimens = newBufferDimens

        if (newViewFinderDimens.height * newViewFinderDimens.width <= 0) {
            return
        }

        viewFinderDimens = newViewFinderDimens

        val matrix = Matrix()

        val centerX: Float = viewFinderDimens.width / 2f
        val centerY: Float = viewFinderDimens.height / 2f

        viewFinderRotation?.let { r -> matrix.postRotate(-r.toFloat(), centerX, centerY) }

        val bufferRatio: Double = bufferDimens.height / bufferDimens.width.toDouble()
        val viewFinderRatio: Double = viewFinderDimens.width / viewFinderDimens.height.toDouble()
        val scaledWidth: Int
        val scaledHeight: Int

        if (viewFinderRatio > 1.0) {
            if (bufferRatio > 1 / viewFinderRatio) {
                scaledHeight = viewFinderDimens.width
                scaledWidth = (viewFinderDimens.width * bufferRatio).roundToInt()
            } else {
                scaledHeight = (viewFinderDimens.height / bufferRatio).roundToInt()
                scaledWidth = viewFinderDimens.height
            }
        } else {
            if (bufferRatio > viewFinderRatio) {
                scaledHeight = viewFinderDimens.height
                scaledWidth = (viewFinderDimens.height * bufferRatio).roundToInt()
            } else {
                scaledHeight = (viewFinderDimens.width / bufferRatio).roundToInt()
                scaledWidth = viewFinderDimens.width
            }
        }

        val xScale: Float = scaledWidth / viewFinderDimens.width.toFloat()
        val yScale: Float = scaledHeight / viewFinderDimens.height.toFloat()

        matrix.preScale(xScale, yScale, centerX, centerY)

        textureView.setTransform(matrix)
    }
}