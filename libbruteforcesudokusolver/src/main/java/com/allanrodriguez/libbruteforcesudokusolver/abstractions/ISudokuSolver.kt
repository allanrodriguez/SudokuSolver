package com.allanrodriguez.libbruteforcesudokusolver.abstractions

interface ISudokuSolver {

    val currentPointer: Int
    val initialState: IPuzzle
    val isFailure: Boolean
    val isSolved: Boolean
    val isUnique: Boolean
    val locationOfUnknowns: List<Pair<Int, Int>>
    val numberOfGuesses: Int
    val numberOfUnknowns: Int
    val presentState: IPuzzle

    fun solve()
}
