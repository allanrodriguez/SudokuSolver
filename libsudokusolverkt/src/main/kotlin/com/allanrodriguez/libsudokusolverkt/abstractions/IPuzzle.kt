package com.allanrodriguez.libsudokusolverkt.abstractions

interface IPuzzle {

    /**
     * Whether or not the puzzle has been solved.
     */
    val solved: Boolean

    /**
     * The number of unsolved cells in the puzzle.
     */
    val unsolvedCount: Int

    /**
     * Clears all the modifications made to the puzzle.
     */
    fun clear()

    /**
     * Returns the ICell object at the specified row and column.
     */
    fun getCellAt(row: Int, col: Int): ICell

    /**
     * Checks if the specified subgrid is solved.
     */
    fun isSubgridSolved(subgrid: Int): Boolean

    /**
     * Prints the sudoku puzzle.
     */
    fun print()
}