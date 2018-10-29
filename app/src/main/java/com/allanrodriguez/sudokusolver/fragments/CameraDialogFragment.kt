package com.allanrodriguez.sudokusolver.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProviders
import com.allanrodriguez.sudokusolver.R
import com.allanrodriguez.sudokusolver.viewmodels.CameraDialogViewModel
import kotlinx.android.synthetic.main.fragment_camera_dialog.*

class CameraDialogFragment : DialogFragment() {

    companion object {
        fun newInstance() = CameraDialogFragment()
    }

    private lateinit var viewModel: CameraDialogViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_camera_dialog, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(CameraDialogViewModel::class.java)

        camera_dialog_toolbar.setNavigationOnClickListener { _ -> fragmentManager?.popBackStack() }
    }

}
