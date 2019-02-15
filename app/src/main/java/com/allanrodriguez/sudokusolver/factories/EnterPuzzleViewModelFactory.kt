package com.allanrodriguez.sudokusolver.factories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.allanrodriguez.sudokusolver.abstractions.ISudokuSolverFactory

class EnterPuzzleViewModelFactory(private val param: ISudokuSolverFactory) : ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        try {
            return modelClass.getConstructor(ISudokuSolverFactory::class.java).newInstance(param)
        } catch (e: InstantiationException) {
            throw RuntimeException("Cannot create an instance of $modelClass", e)
        } catch (e: IllegalAccessException) {
            throw RuntimeException("Cannot create an instance of $modelClass", e)
        }
    }
}