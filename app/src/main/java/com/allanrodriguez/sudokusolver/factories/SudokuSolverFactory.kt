package com.allanrodriguez.sudokusolver.factories

import com.allanrodriguez.libbruteforcesudokusolver.SudokuSolver
import com.allanrodriguez.libbruteforcesudokusolver.abstractions.ISudokuSolver
import com.allanrodriguez.sudokusolver.abstractions.ISudokuSolverFactory

class SudokuSolverFactory : ISudokuSolverFactory {

    override fun newInstance(puzzle: Array<IntArray>): ISudokuSolver {
        return SudokuSolver(puzzle)
    }
}