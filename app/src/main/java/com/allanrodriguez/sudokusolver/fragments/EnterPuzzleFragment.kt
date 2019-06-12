package com.allanrodriguez.sudokusolver.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.allanrodriguez.sudokusolver.R
import com.allanrodriguez.sudokusolver.databinding.FragmentEnterPuzzleBinding
import com.allanrodriguez.sudokusolver.factories.EnterPuzzleViewModelFactory
import com.allanrodriguez.sudokusolver.factories.SudokuSolverFactory
import com.allanrodriguez.sudokusolver.viewmodels.EnterPuzzleViewModel
import kotlinx.android.synthetic.main.fragment_enter_puzzle.*

class EnterPuzzleFragment : Fragment() {

    companion object {
        fun newInstance() = EnterPuzzleFragment()
    }

    private lateinit var binding: FragmentEnterPuzzleBinding
    private lateinit var viewModel: EnterPuzzleViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_enter_puzzle, container, false)
        binding.lifecycleOwner = this

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val sudokuSolverFactory = SudokuSolverFactory()
        val enterPuzzleViewModelFactory = EnterPuzzleViewModelFactory(sudokuSolverFactory)
        viewModel = ViewModelProviders.of(this, enterPuzzleViewModelFactory).get(EnterPuzzleViewModel::class.java)
        binding.enterPuzzleVm = viewModel

        val puzzleEmptyObserver = Observer<Boolean> { isPuzzleEmpty ->
            if (isPuzzleEmpty) {
                AlertDialog.Builder(context!!)
                        .setMessage(R.string.text_sudoku_empty)
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            viewModel.showPuzzleEmptyDialog.value = false
                        }
                    .show()
            }
        }
        val puzzleFullObserver = Observer<Boolean> { isPuzzleFull ->
            if (isPuzzleFull) {
                AlertDialog.Builder(context!!)
                        .setMessage(R.string.text_sudoku_full)
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            viewModel.showPuzzleFullDialog.value = false
                        }
                        .show()
            }
        }

        viewModel.showPuzzleEmptyDialog.observe(this, puzzleEmptyObserver)
        viewModel.showPuzzleFullDialog.observe(this, puzzleFullObserver)

        // Remove focus from puzzle EditTexts on click outside
        sudoku_layout.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                val imm: InputMethodManager =
                        context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
            }
        }
    }
}
