package com.allanrodriguez.sudokusolver.viewmodels

import androidx.lifecycle.ViewModel

class EnterPuzzleViewModel : ViewModel() {

    val sudoku: Array<Array<CellViewModel>> = Array(9) { _ -> Array(9) { _ -> CellViewModel() } }
}
