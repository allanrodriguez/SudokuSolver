package com.allanrodriguez.libbruteforcesudokusolver

import com.allanrodriguez.libbruteforcesudokusolver.abstractions.IPuzzle
import com.allanrodriguez.libbruteforcesudokusolver.abstractions.ISudokuSolver

class SudokuSolver(puzzle: Array<IntArray>) : ISudokuSolver {

    //region Properties
    override val initialState: IPuzzle
        get() = _initialState
    override val locationOfUnknowns: List<Pair<Int, Int>>
        get() = _locationOfUnknowns


    override var currentPointer: Int = 0
        private set
    override var isFailure: Boolean = false
    override var isSolved: Boolean = false
    override var isUnique: Boolean = false

    override var numberOfGuesses: Int = 0
    override var numberOfUnknowns: Int = 0

    override val presentState: IPuzzle
        get() = _presentState

    private val _initialState = Puzzle()
    private val _locationOfUnknowns: MutableList<Pair<Int, Int>> = mutableListOf()
    private val _presentState = Puzzle()
    //endregion

    init {
        for (i: Int in 0..8) {
            for (j: Int in 0..8) {
                val value: Int = puzzle[i][j]
                _initialState[i, j] = value
                _presentState[i, j] = value
                if (value == 0) {
                    _locationOfUnknowns.add(Pair(i, j))
                }
            }
        }
        numberOfUnknowns = locationOfUnknowns.size
    }

    override fun solve() {
        if (!isValidState()) {
            throw Exception("Initial state not valid.")
        }

        while (true) {
            if (takeNewGuess()) {
                break
            }
            if (isSolved) {
                break
            }
        }

        if (isFailure) {
            println("Failure after $numberOfGuesses guess(es).")
        } else {
            println("Solved after $numberOfGuesses guess(es).")
        }
    }

    private fun getTopLeftSubgridCoordinates(row: Int, col: Int): Pair<Int, Int> {
        if (row < 0 || row > 8 || col < 0 || col > 8) {
            throw IndexOutOfBoundsException("Row and column should be between 0 and 8")
        }

        val dividedRow: Int = row / 3
        val subgrid: Int = 3 * dividedRow + col / 3

        val dividedSubgrid: Int = subgrid / 3
        val moduloSubgrid: Int = subgrid % 3

        return Pair(3 * dividedSubgrid, 3 * moduloSubgrid)
    }

    private fun isValidState(): Boolean {
        for (i: Int in 0..8) {
            for (j: Int in 0..8) {
                if (!isValidStateForCellAt(i, j)) {
                    return false
                }
            }
        }

        return true
    }

    private fun isValidStateForCellAt(row: Int, col: Int): Boolean {
        val value: Int = _presentState[row, col]

        if (value == 0) {  // automatically valid
            return true
        }

        // Test row
        for (i: Int in 0..8) {
            if (i != col && _presentState[row, i] == value) {
                return false
            }
        }

        // Test column
        for (i: Int in 0..8) {
            if (i != row && _presentState[i, col] == value) {
                return false
            }
        }

        // Test local 3x3 block
        val startRowCol: Pair<Int, Int> = getTopLeftSubgridCoordinates(row, col)
        for (i: Int in startRowCol.first until startRowCol.first + 3) {
            for (j: Int in startRowCol.second until startRowCol.second + 3) {
                if (i != row && j != col && _presentState[i, j] == value) {
                    return false
                }
            }
        }

        return true
    }

    private fun takeNewGuess(): Boolean {
        val index: Pair<Int, Int> = locationOfUnknowns[currentPointer]
        if (_presentState[index] == 9) {
            if (currentPointer == 0) {
                isFailure = true
                return true
            }
            _presentState[index] = 0
            --currentPointer
        } else {
            ++numberOfGuesses
            ++_presentState[index]

            if (isValidStateForCellAt(index.first, index.second)) {
                if (currentPointer == locationOfUnknowns.size - 1) {
                    isSolved = true
                } else {
                    ++currentPointer
                }
            }
        }

        return false
    }
}
