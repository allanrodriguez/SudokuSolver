package com.allanrodriguez.sudokusolver.fragments

import android.content.Context
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.thoughtbot.expandablerecyclerview.models.ExpandableGroup
import kotlinx.android.synthetic.main.fragment_about_dialog.*

import com.allanrodriguez.sudokusolver.R
import com.allanrodriguez.sudokusolver.abstractions.Licenses
import com.allanrodriguez.sudokusolver.viewmodels.LicenseViewModel
import com.allanrodriguez.sudokusolver.views.LicensesListAdapter

class AboutDialogFragment : DialogFragment() {

    private var isLargeLayout: Boolean = false

    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager

    override fun onAttach(context: Context) {
        super.onAttach(context)
        isLargeLayout = resources.getBoolean(R.bool.large_layout)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_about_dialog, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        source_code_message.movementMethod = LinkMovementMethod.getInstance()

        val licenses: Array<LicenseViewModel> = arrayOf(
                LicenseViewModel("Expandable RecyclerView", "Copyright 2016 Amanda Hill and thoughtbot, inc.\n\n${Licenses.MIT}"),
                LicenseViewModel("Leptonica", "Copyright 2001 Leptonica\n\n${Licenses.BSD_2_CLAUSE}"),
                LicenseViewModel("Material design icons", Licenses.APACHE_2_0),
                LicenseViewModel("OpenCV", "Copyright 2000-2018, Intel Corporation, all rights reserved.\n" +
                        "Copyright 2009-2011, Willow Garage Inc., all rights reserved.\n" +
                        "Copyright 2009-2016, NVIDIA Corporation, all rights reserved.\n" +
                        "Copyright 2010-2013, Advanced Micro Devices, Inc., all rights reserved.\n" +
                        "Copyright 2015-2016, OpenCV Foundation, all rights reserved.\n" +
                        "Copyright 2015-2016, Itseez Inc., all rights reserved.\n" +
                        "Third party copyrights are property of their respective owners.\n\n${Licenses.BSD_3_CLAUSE}"),
                LicenseViewModel("Tesseract", Licenses.APACHE_2_0)
        )

        val groups: MutableList<ExpandableGroup<LicenseViewModel>> = mutableListOf()

        for (viewModel: LicenseViewModel in licenses) {
            groups.add(ExpandableGroup(viewModel.title, listOf(viewModel)))
        }

        viewManager = LinearLayoutManager(context)
        viewAdapter = LicensesListAdapter(groups)

        licenses_list.setHasFixedSize(true)
        licenses_list.layoutManager = viewManager
        licenses_list.adapter = viewAdapter

        if (isLargeLayout) {
            dialog?.setCanceledOnTouchOutside(false)
        }

        about_dialog_toolbar.setNavigationOnClickListener {
            closeDialog()
        }
    }

    override fun onStart() {
        super.onStart()

        if (isLargeLayout) {
            context?.resources?.displayMetrics?.let {
                val width: Int = (it.widthPixels * 0.75).toInt()
                val height: Int = (it.heightPixels * 0.75).toInt()

                dialog?.window?.setLayout(width, height)
            }
        }
    }

    private fun closeDialog() {
        if (isLargeLayout) {
            dismiss()
        } else {
            fragmentManager?.popBackStack()
        }
    }

    companion object {
        fun newInstance() = AboutDialogFragment()
    }

}
