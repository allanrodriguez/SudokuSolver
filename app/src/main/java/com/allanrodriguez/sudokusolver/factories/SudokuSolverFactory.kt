package com.allanrodriguez.sudokusolver.factories

import com.allanrodriguez.libbruteforcesudokusolver.SudokuSolver
import com.allanrodriguez.libbruteforcesudokusolver.abstractions.ISudokuSolver
import com.allanrodriguez.sudokusolver.abstractions.ISudokuSolverFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SudokuSolverFactory @Inject constructor() : ISudokuSolverFactory {

    override fun newInstance(puzzle: Array<IntArray>): ISudokuSolver {
        return SudokuSolver(puzzle)
    }
}
