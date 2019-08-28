package com.allanrodriguez.sudokusolver.viewmodels

import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.allanrodriguez.libbruteforcesudokusolver.abstractions.ISudokuSolver
import com.allanrodriguez.sudokusolver.abstractions.IEnterPuzzleViewModel
import com.allanrodriguez.sudokusolver.abstractions.ISudokuSolverFactory
import javax.inject.Inject

class EnterPuzzleViewModel @Inject constructor(private val factory: ISudokuSolverFactory) : ViewModel(),
        IEnterPuzzleViewModel {

    override val isCameraButtonClickable: MutableLiveData<Boolean> = MutableLiveData()
    override val isSolving: MutableLiveData<Boolean> = MutableLiveData()
    override val showPuzzleEmptyDialog: MutableLiveData<Boolean> = MutableLiveData()
    override val showPuzzleFullDialog: MutableLiveData<Boolean> = MutableLiveData()
    override val sudoku: List<List<CellViewModel>> = List(9) { List(9) { CellViewModel() } }

    private val cameraButtonClickListeners = mutableListOf<() -> Unit>()

    private val sudokuArray: Array<IntArray>
        get() {
            return Array(9) { row ->
                IntArray(9) { col ->
                    val value: Int? = sudoku[row][col].cellValue.value?.toIntOrNull()

                    value ?: 0
                }
            }
        }

    init {
        isCameraButtonClickable.value = true
        isSolving.value = false
        showPuzzleEmptyDialog.value = false
        showPuzzleFullDialog.value = false
    }

    override fun clear() {
        for (row: List<CellViewModel> in sudoku) {
            for (cell: CellViewModel in row) {
                cell.clear()
            }
        }
    }

    override fun onCameraButtonClick(view: View) {
        isCameraButtonClickable.value = false
        for (l: () -> Unit in cameraButtonClickListeners) {
            l()
        }
        isCameraButtonClickable.value = true
    }

    override fun onSolveButtonClick(view: View) {
        isSolving.value = true
        val sudokuSolver: ISudokuSolver = factory.newInstance(sudokuArray)

        when (sudokuSolver.numberOfUnknowns) {
            81 -> showPuzzleEmptyDialog.value = true
            0 -> showPuzzleFullDialog.value = true
            else -> {
                sudokuSolver.solve()

                for (i: Int in 0..8) {
                    for (j: Int in 0..8) {
                        val value: String = sudokuSolver.presentState[i, j].toString()
                        if (sudoku[i][j].cellValue.value != value) {
                            sudoku[i][j].setValue(value)
                        }
                    }
                }
            }
        }

        isSolving.value = false
    }

    override fun addCameraButtonClickListener(listener: () -> Unit) {
        if (!cameraButtonClickListeners.contains(listener)) {
            cameraButtonClickListeners.add(listener)
        }
    }

    override fun removeCameraButtonClickListener(listener: () -> Unit): Boolean {
        return cameraButtonClickListeners.remove(listener)
    }
}