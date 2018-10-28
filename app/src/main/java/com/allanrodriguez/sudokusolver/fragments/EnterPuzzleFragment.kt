package com.allanrodriguez.sudokusolver.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.allanrodriguez.sudokusolver.R
import com.allanrodriguez.sudokusolver.databinding.FragmentEnterPuzzleBinding
import com.allanrodriguez.sudokusolver.viewmodels.EnterPuzzleViewModel
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_enter_puzzle.*

class EnterPuzzleFragment : Fragment() {

    companion object {
        fun newInstance() = EnterPuzzleFragment()
    }

    private lateinit var binding: FragmentEnterPuzzleBinding
    private lateinit var viewModel: EnterPuzzleViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_enter_puzzle, container, false)
        binding.setLifecycleOwner(this)

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(EnterPuzzleViewModel::class.java)
        binding.enterPuzzleVm = viewModel

        button_solve.setOnClickListener { view ->
            val snackbar: Snackbar = Snackbar.make(view, "Replace with your own action", BaseTransientBottomBar.LENGTH_LONG)
                    .setAction("Action", null)
//            val snackbarView: View = snackbar.view
//            val params: CoordinatorLayout.LayoutParams = snackbarView.layoutParams as CoordinatorLayout.LayoutParams

//            params.setMargins(params.leftMargin, params.topMargin, params.rightMargin, params.bottomMargin + 50)
//            snackbarView.layoutParams = params

            snackbar.show()
        }
    }
}
