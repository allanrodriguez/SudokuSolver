package com.allanrodriguez.sudokusolver.viewmodels

import androidx.lifecycle.ViewModel

class EnterPuzzleViewModel : ViewModel() {

    val sudoku: Array<Array<CellViewModel>> = Array(9) { Array(9) { CellViewModel() } }

    fun clear() {
        for (row: Array<CellViewModel> in sudoku) {
            for (cell: CellViewModel in row) {
                cell.clear()
            }
        }
    }
}