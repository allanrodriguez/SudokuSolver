package com.allanrodriguez.sudokusolver.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.allanrodriguez.sudokusolver.R
import com.allanrodriguez.sudokusolver.viewmodels.LicenseViewModel
import com.allanrodriguez.sudokusolver.views.LicensesListAdapter
import kotlinx.android.synthetic.main.fragment_about_dialog.*

class AboutDialogFragment : DialogFragment() {

    private var isLargeLayout: Boolean = false

    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        isLargeLayout = resources.getBoolean(R.bool.large_layout)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_about_dialog, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val arr: Array<LicenseViewModel> = arrayOf(
                LicenseViewModel("Hello", "1"),
                LicenseViewModel("Potato", "2")
        )

        viewManager = LinearLayoutManager(context)
        viewAdapter = LicensesListAdapter(arr)

        licenses_list.setHasFixedSize(true)
        licenses_list.layoutManager = viewManager
        licenses_list.adapter = viewAdapter

        if (isLargeLayout) {
            dialog.setCanceledOnTouchOutside(false)
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

                dialog.window?.setLayout(width, height)
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
