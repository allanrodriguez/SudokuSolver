package com.allanrodriguez.libsudokusolverkt

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SudokuSolverTests {

    @Test
    fun `can solve easy puzzle`() {
        // Arrange
        val puzzleArr: IntArray = intArrayOf(
                0, 0, 0, 3, 0, 5, 4, 0, 0,
                5, 0, 0, 6, 1, 0, 0, 9, 8,
                6, 0, 7, 0, 0, 4, 0, 3, 0,
                4, 0, 0, 0, 8, 6, 0, 5, 0,
                0, 0, 0, 4, 2, 3, 0, 0, 0,
                0, 9, 0, 7, 5, 0, 0, 0, 2,
                0, 7, 0, 5, 0, 0, 6, 0, 4,
                1, 4, 0, 0, 6, 7, 0, 0, 5,
                0, 0, 5, 1, 0, 9, 0, 0, 0
        )

        val puzzle = Puzzle(puzzleArr)
        val solver = SudokuSolver(puzzle)

        // Act
        val solveResult: Boolean = solver.solve()

        puzzle.print()

        // Assert
        assertTrue(solveResult)
    }

    // @Test
    // fun `can solve nyt hard puzzle`() {
    //     // Arrange
    //     val puzzleArr: IntArray = intArrayOf(
    //             0, 9, 0, 0, 0, 0, 8, 0, 0,
    //             0, 0, 0, 8, 2, 0, 5, 0, 0,
    //             0, 0, 0, 0, 0, 1, 0, 6, 0,
    //             0, 6, 0, 0, 0, 0, 9, 0, 4,
    //             5, 0, 9, 0, 0, 7, 0, 0, 0,
    //             2, 0, 0, 0, 1, 0, 7, 0, 0,
    //             0, 0, 0, 1, 4, 0, 0, 2, 7,
    //             6, 0, 0, 0, 0, 0, 0, 0, 0,
    //             0, 0, 0, 0, 8, 9, 0, 3, 0
    //     )

    //     val puzzle = Puzzle(puzzleArr)
    //     val solver = SudokuSolver(puzzle)

    //     // Act
    //     val solveResult: Boolean = solver.solve()

    //     puzzle.print()

    //     // Assert
    //     assertTrue(solveResult)
    // }

    // // http://www.sudokuessentials.com/support-files/sudoku-very-hard-1.pdf
    // @Test
    // fun `can solve sudokuessentials very hard puzzle`() {
    //     // Arrange
    //     val puzzleArr: IntArray = intArrayOf(
    //             0, 3, 0, 4, 8, 0, 6, 0, 9,
    //             0, 0, 0, 0, 2, 7, 0, 0, 0,
    //             8, 0, 0, 3, 0, 0, 0, 0, 0,
    //             0, 1, 9, 0, 0, 0, 0, 0, 0,
    //             7, 8, 0, 0, 0, 2, 0, 9, 3,
    //             0, 0, 0, 0, 0, 4, 8, 7, 0,
    //             0, 0, 0, 0, 0, 5, 0, 0, 6,
    //             0, 0, 0, 1, 3, 0, 0, 0, 0,
    //             9, 0, 2, 0, 4, 8, 0, 1, 0
    //     )

    //     val puzzle = Puzzle(puzzleArr)
    //     val solver = SudokuSolver(puzzle)

    //     // Act
    //     val solveResult: Boolean = solver.solve()

    //     puzzle.print()

    //     // Assert
    //     assertTrue(solveResult)
    // }
}