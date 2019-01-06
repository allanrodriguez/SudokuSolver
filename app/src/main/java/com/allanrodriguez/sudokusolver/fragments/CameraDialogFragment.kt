package com.allanrodriguez.sudokusolver.fragments

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.view.*
import android.view.View.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.allanrodriguez.sudokusolver.R
import com.allanrodriguez.sudokusolver.abstractions.FlashState
import com.allanrodriguez.sudokusolver.model.CameraSession
import com.allanrodriguez.sudokusolver.views.AutoFitTextureView
import com.allanrodriguez.sudokusolver.views.FlashButton
import kotlinx.android.synthetic.main.fragment_camera_dialog.*

class CameraDialogFragment : DialogFragment() {

    //region Properties
    private lateinit var cameraSession: CameraSession
    private lateinit var textureView: AutoFitTextureView

    private var isImagePreviewShowing = false
    private var isLargeLayout = false
    //endregion

    //region Lifecycle methods
    override fun onAttach(context: Context?) {
        super.onAttach(context)
        isLargeLayout = resources.getBoolean(R.bool.large_layout)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_camera_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        textureView = camera_preview
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        camera_dialog_toolbar.setNavigationOnClickListener {
            closeDialog()
        }

        button_take_picture.setOnClickListener {
            it.isClickable = false
            cameraSession.takePicture()
        }

        button_flash.setOnFlashStateChangedListener(FlashListener())

        button_accept.setOnClickListener {
            val imageRect = Rect(camera_preview.left, camera_preview.top, camera_preview.right, camera_preview.bottom)
            val squareRect = Rect(camera_window.left, camera_window.top, camera_window.right, camera_window.bottom)

            val fragment: Fragment = ParseOcrFragment.newInstance(cameraSession.file, imageRect, squareRect)

            closeDialog()

            fragmentManager?.beginTransaction()
                ?.setCustomAnimations(
                    R.anim.fade_in,
                    R.anim.fade_out,
                    R.anim.fade_in,
                    R.anim.fade_out
                )
                ?.add(android.R.id.content, fragment, ParseOcrFragment::class.java.simpleName)
                ?.addToBackStack(null)
                ?.commit()
        }

        button_retake.setOnClickListener {
            cancelImagePreview()
        }
    }

    override fun onStart() {
        super.onStart()

        cameraSession = CameraSession(activity as Activity, lifecycle, textureView).apply {
            addOnErrorListener(::onError)
            addOnFlashSupportChangedListener(::onFlashSupportChanged)
            addOnPictureAvailableListener(::onPictureAvailable)
        }
        lifecycle.addObserver(cameraSession)

        if (isLargeLayout) {
            context?.resources?.displayMetrics?.let {
                val width: Int = (it.widthPixels * 0.75).toInt()
                val height: Int = (it.heightPixels * 0.75).toInt()

                dialog.window?.setLayout(width, height)
            }
        }
    }

    override fun onStop() {
        super.onStop()

        lifecycle.removeObserver(cameraSession)
        cameraSession.removeOnErrorListener(::onError)
        cameraSession.removeOnPictureAvailableListener(::onPictureAvailable)
        cameraSession.removeOnFlashSupportChangedListener(::onFlashSupportChanged)
    }
    //endregion

    private fun brightenScreens() {
        val from: Int = ContextCompat.getColor(context as Context, R.color.black)
        val to: Int = ContextCompat.getColor(context as Context, R.color.transparentBlack)

        val colorTransition: ValueAnimator = ValueAnimator.ofArgb(from, to).apply {
            duration = 250
            addUpdateListener {
                val color: Int = it.animatedValue as Int

                top_screen.setBackgroundColor(color)
                left_screen.setBackgroundColor(color)
                right_screen.setBackgroundColor(color)
                bottom_screen.setBackgroundColor(color)
            }
        }

        colorTransition.start()
    }

    private fun showImagePreview() {
        image_preview.setImageBitmap(BitmapFactory.decodeFile(cameraSession.file.absolutePath))
        image_preview.visibility = VISIBLE
        darkenScreens()
        camera_directions.visibility = INVISIBLE
        showAcceptRetakeButtons()
        isImagePreviewShowing = true
    }

    private fun cancelImagePreview() {
        image_preview.setImageDrawable(null)
        image_preview.visibility = INVISIBLE
        cameraSession.deletePicture()
        brightenScreens()
        camera_directions.visibility = VISIBLE
        showTakePictureButton()
        isImagePreviewShowing = false
    }

    private fun closeDialog() {
        if (isLargeLayout) {
            dismiss()
        } else {
            fragmentManager?.popBackStack()
        }
    }

    private fun darkenScreens() {
        val from: Int = ContextCompat.getColor(context as Context, R.color.transparentBlack)
        val to: Int = ContextCompat.getColor(context as Context, R.color.black)

        val colorTransition: ValueAnimator = ValueAnimator.ofArgb(from, to).apply {
            duration = 250
            addUpdateListener {
                val color: Int = it.animatedValue as Int

                top_screen.setBackgroundColor(color)
                left_screen.setBackgroundColor(color)
                right_screen.setBackgroundColor(color)
                bottom_screen.setBackgroundColor(color)
            }
        }
        colorTransition.start()
    }

    private fun onError() {
        closeDialog()
        AlertDialog.Builder(context!!)
            .setTitle(R.string.error)
            .setMessage(R.string.text_camera_error)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun onFlashSupportChanged(isFlashSupported: Boolean) {
        activity?.runOnUiThread {
            button_flash.visibility = if (isFlashSupported) VISIBLE else GONE
        }
    }

    private fun onPictureAvailable() {
        activity?.runOnUiThread(::showImagePreview)
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

    companion object {
        fun newInstance() = CameraDialogFragment()
    }

    private inner class FlashListener : FlashButton.FlashListener {

        override fun onAuto() {
            cameraSession.setFlash(FlashState.AUTO)
        }

        override fun onOff() {
            cameraSession.setFlash(FlashState.OFF)
        }

        override fun onOn() {
            cameraSession.setFlash(FlashState.ON)
        }
    }
}