package com.allanrodriguez.sudokusolver.abstractions

import android.view.View
import androidx.lifecycle.MutableLiveData
import com.allanrodriguez.sudokusolver.viewmodels.CellViewModel

interface IEnterPuzzleViewModel {
    val isCameraButtonClickable: MutableLiveData<Boolean>
    val isSolving: MutableLiveData<Boolean>
    val showPuzzleEmptyDialog: MutableLiveData<Boolean>
    val showPuzzleFullDialog: MutableLiveData<Boolean>
    val sudoku: List<List<CellViewModel>>

    fun clear()
    fun onCameraButtonClick(view: View)
    fun onSolveButtonClick(view: View)

    fun addCameraButtonClickListener(listener: () -> Unit)
    fun removeCameraButtonClickListener(listener: () -> Unit): Boolean
}