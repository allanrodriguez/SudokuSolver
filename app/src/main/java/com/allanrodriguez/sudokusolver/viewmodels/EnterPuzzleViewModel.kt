package com.allanrodriguez.sudokusolver.viewmodels

import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.allanrodriguez.libbruteforcesudokusolver.abstractions.ISudokuSolver
import com.allanrodriguez.sudokusolver.abstractions.ISudokuSolverFactory

class EnterPuzzleViewModel(private val factory: ISudokuSolverFactory) : ViewModel() {

    val isSolving: MutableLiveData<Boolean> = MutableLiveData()
    val showPuzzleEmptyDialog: MutableLiveData<Boolean> = MutableLiveData()
    val showPuzzleFullDialog: MutableLiveData<Boolean> = MutableLiveData()
    val sudoku: List<List<CellViewModel>> = List(9) { List(9) { CellViewModel() } }

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
        isSolving.value = false
        showPuzzleEmptyDialog.value = false
        showPuzzleFullDialog.value = false
    }

    fun clear() {
        for (row: List<CellViewModel> in sudoku) {
            for (cell: CellViewModel in row) {
                cell.clear()
            }
        }
    }

    fun onSolveButtonClick(view: View) {
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
}