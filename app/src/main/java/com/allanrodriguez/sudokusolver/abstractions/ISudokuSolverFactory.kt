package com.allanrodriguez.sudokusolver.abstractions

import com.allanrodriguez.libbruteforcesudokusolver.abstractions.ISudokuSolver

interface ISudokuSolverFactory {

    fun newInstance(puzzle: Array<IntArray>): ISudokuSolver
}