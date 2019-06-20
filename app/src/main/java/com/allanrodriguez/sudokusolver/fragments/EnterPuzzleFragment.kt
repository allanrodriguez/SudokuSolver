package com.allanrodriguez.sudokusolver.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.allanrodriguez.sudokusolver.R
import com.allanrodriguez.sudokusolver.databinding.FragmentEnterPuzzleBinding
import com.allanrodriguez.sudokusolver.factories.EnterPuzzleViewModelFactory
import com.allanrodriguez.sudokusolver.factories.SudokuSolverFactory
import com.allanrodriguez.sudokusolver.viewmodels.EnterPuzzleViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_enter_puzzle.*

class EnterPuzzleFragment : Fragment() {

    companion object {
        const val TAG = "EnterPuzzleFragment"

        fun newInstance() = EnterPuzzleFragment()
    }

    private lateinit var binding: FragmentEnterPuzzleBinding
    private lateinit var viewModel: EnterPuzzleViewModel
    private var isLargeLayout = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_enter_puzzle, container, false)
        binding.lifecycleOwner = this
        setHasOptionsMenu(true)

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        isLargeLayout = resources.getBoolean(R.bool.large_layout)

        val sudokuSolverFactory = SudokuSolverFactory()
        val enterPuzzleViewModelFactory = EnterPuzzleViewModelFactory(sudokuSolverFactory)
        viewModel = ViewModelProviders.of(this, enterPuzzleViewModelFactory).get(EnterPuzzleViewModel::class.java)
        binding.enterPuzzleVm = viewModel

        val puzzleEmptyObserver = Observer<Boolean> { isPuzzleEmpty ->
            if (isPuzzleEmpty) {
                AlertDialog.Builder(context!!, R.style.AppTheme_Dialog)
                        .setMessage(R.string.text_sudoku_empty)
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            viewModel.showPuzzleEmptyDialog.value = false
                        }
                    .show()
            }
        }
        val puzzleFullObserver = Observer<Boolean> { isPuzzleFull ->
            if (isPuzzleFull) {
                AlertDialog.Builder(context!!, R.style.AppTheme_Dialog)
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
        sudoku_layout.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                val imm: InputMethodManager =
                        context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
            }
        }

        sudoku_layout.post { Log.d(TAG, "Sudoku layout height: ${sudoku_layout.height}") }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.enter_puzzle, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val context: Context = context!!

        when (item.itemId) {
            R.id.action_clear -> showSnackbar(context)
            R.id.action_about -> {
                val aboutDialogFragment: AboutDialogFragment = AboutDialogFragment.newInstance()
                showDialogFragment(aboutDialogFragment)
            }
        }

        return true
    }

    private fun showDialogFragment(dialog: DialogFragment) {
        // Show the dialog in a small pop-up window if app is run on a tablet.
        if (isLargeLayout) {
            dialog.show(fragmentManager!!, dialog::class.java.simpleName)
        } else {
            fragmentManager!!
                    .beginTransaction()
                    .setCustomAnimations(
                            R.anim.slide_up,
                            R.anim.slide_down,
                            R.anim.slide_up,
                            R.anim.slide_down
                    )
                    .add(android.R.id.content, dialog, dialog::class.java.simpleName)
                    .addToBackStack(null)
                    .commit()
        }
    }

    private fun showSnackbar(context: Context) {
        AlertDialog.Builder(context, R.style.AppTheme_Dialog)
                .setMessage(R.string.text_clear_sudoku)
                .setPositiveButton(R.string.yes) { _, _ ->
                    val fragment: Fragment =
                            fragmentManager?.findFragmentByTag(EnterPuzzleFragment::class.java.simpleName)!!
                    val enterPuzzleVm: EnterPuzzleViewModel =
                            ViewModelProviders.of(fragment).get(EnterPuzzleViewModel::class.java)
                    enterPuzzleVm.clear()

                    Snackbar.make(sudoku_layout, R.string.text_sudoku_cleared, Snackbar.LENGTH_LONG)
                            .setAction(R.string.action_dismiss) { }
                            .setAnchorView(enter_puzzle_camera_fab)
                            .show()
                }
                .setNegativeButton(R.string.no, null)
                .show()
    }
}
