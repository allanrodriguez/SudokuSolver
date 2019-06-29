package com.allanrodriguez.sudokusolver.fragments

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.allanrodriguez.sudokusolver.R
import com.allanrodriguez.sudokusolver.abstractions.MY_PERMISSIONS_REQUEST_CAMERA
import com.allanrodriguez.sudokusolver.activities.CameraActivity
import com.allanrodriguez.sudokusolver.databinding.FragmentEnterPuzzleBinding
import com.allanrodriguez.sudokusolver.factories.EnterPuzzleViewModelFactory
import com.allanrodriguez.sudokusolver.factories.SudokuSolverFactory
import com.allanrodriguez.sudokusolver.utilities.showDialogFragment
import com.allanrodriguez.sudokusolver.viewmodels.EnterPuzzleViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_enter_puzzle.*

class EnterPuzzleFragment : Fragment() {

    private lateinit var inputMethodManager: InputMethodManager
    private lateinit var binding: FragmentEnterPuzzleBinding
    private lateinit var viewModel: EnterPuzzleViewModel
    private var isLargeLayout = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_enter_puzzle, container, false)

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        setHasOptionsMenu(true)

        inputMethodManager = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        isLargeLayout = resources.getBoolean(R.bool.large_layout)

        val sudokuSolverFactory = SudokuSolverFactory()
        val enterPuzzleViewModelFactory = EnterPuzzleViewModelFactory(sudokuSolverFactory)
        viewModel = ViewModelProviders.of(this, enterPuzzleViewModelFactory).get(EnterPuzzleViewModel::class.java)
        binding.enterPuzzleVm = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        setUpObservables()

        // Remove focus from puzzle EditTexts on click outside
        scrollable_sudoku_layout.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                inputMethodManager.hideSoftInputFromWindow(v.windowToken, 0)
            }
        }

        viewModel.addCameraButtonClickListener {
            context?.let { c ->
                val intent = Intent(c, CameraActivity::class.java)
                startActivity(intent)
            }
        }

        sudoku_layout.post {
            val layoutParams: ViewGroup.LayoutParams = scrollable_sudoku_layout.layoutParams
            layoutParams.height = sudoku_layout.height
            scrollable_sudoku_layout.layoutParams = layoutParams
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.enter_puzzle, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val localContext: Context? = context

        if (localContext == null) {
            Log.e(TAG, "Context was null when attempting to launch camera dialog.")
            return true
        }

        when (item.itemId) {
            R.id.action_clear -> showSnackbar(localContext)
            R.id.action_about -> {
                val aboutDialogFragment: AboutDialogFragment = AboutDialogFragment.newInstance()
                showDialogFragment(aboutDialogFragment)
            }
        }

        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        val cameraIndex: Int = permissions.indexOf(Manifest.permission.CAMERA)

        if (requestCode == MY_PERMISSIONS_REQUEST_CAMERA
                && cameraIndex >= 0
                && grantResults[cameraIndex] == PackageManager.PERMISSION_GRANTED) {
            launchCameraDialog()
        } else {
            Log.i(tag, "Camera permission was denied.")
        }
    }

    private fun setUpObservables() {
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
    }

    private fun launchCameraDialog() {
        val localContext: Context? = context

        if (localContext == null) {
            Log.e(TAG, "Context was null when attempting to launch camera dialog.")
            return
        }

        when {
            ContextCompat.checkSelfPermission(localContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                Log.i(TAG, "Camera permission was granted.")
                val cameraDialog: CameraDialogFragment = CameraDialogFragment.newInstance()
                showDialogFragment(cameraDialog)
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                Log.i(TAG, "Showing camera permission request rationale.")
                AlertDialog.Builder(localContext)
                        .setTitle("Sudoku Solver needs permission to use your camera to read puzzles from pictures.")
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            Log.i(tag, "Requesting camera permission...")
                            requestPermissions(arrayOf(Manifest.permission.CAMERA), MY_PERMISSIONS_REQUEST_CAMERA)
                        }
                        .show()
            }
            else -> {
                Log.i(tag, "Requesting camera permission...")
                requestPermissions(arrayOf(Manifest.permission.CAMERA), MY_PERMISSIONS_REQUEST_CAMERA)
            }
        }
    }

    private fun showSnackbar(context: Context) {
        AlertDialog.Builder(context, R.style.AppTheme_Dialog)
                .setMessage(R.string.text_clear_sudoku)
                .setPositiveButton(R.string.yes) { _, _ ->
                    viewModel.clear()

                    Snackbar.make(scrollable_sudoku_layout, R.string.text_sudoku_cleared, Snackbar.LENGTH_LONG)
                            .setAction(R.string.action_dismiss) { }
                            .setAnchorView(enter_puzzle_camera_fab)
                            .show()
                }
                .setNegativeButton(R.string.no, null)
                .show()
    }

    companion object {
        const val TAG = "EnterPuzzleFragment"
    }
}
