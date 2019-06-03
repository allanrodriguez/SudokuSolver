package com.allanrodriguez.libbruteforcesudokusolver

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class SudokuSolverTests {

    @Test
    fun `currentPointer at start is 0`() {
        // Arrange
        val expectedCurrentPointerValue = 0
        val emptyPuzzle: Array<IntArray> = Array(9) {
            IntArray(9)
        }

        // Act
        val sudokuSolver = SudokuSolver(emptyPuzzle)

        // Assert
        Assertions.assertEquals(expectedCurrentPointerValue, sudokuSolver.currentPointer)
    }

    @Test
    fun `isFailure at start is false`() {
        // Arrange
        val emptyPuzzle: Array<IntArray> = Array(9) {
            IntArray(9)
        }

        // Act
        val sudokuSolver = SudokuSolver(emptyPuzzle)

        // Assert
        Assertions.assertFalse(sudokuSolver.isFailure)
    }

    @Test
    fun `isSolved at start is false`() {
        // Arrange
        val emptyPuzzle: Array<IntArray> = Array(9) {
            IntArray(9)
        }

        // Act
        val sudokuSolver = SudokuSolver(emptyPuzzle)

        // Assert
        Assertions.assertFalse(sudokuSolver.isSolved)
    }

    @Test
    fun `isUnique at start is false`() {
        // Arrange
        val emptyPuzzle: Array<IntArray> = Array(9) {
            IntArray(9)
        }

        // Act
        val sudokuSolver = SudokuSolver(emptyPuzzle)

        // Assert
        Assertions.assertFalse(sudokuSolver.isUnique)
    }

    @Test
    fun `locationOfUnknowns for an empty puzzle at start has size 81`() {
        // Arrange
        val expectedLocationOfUnknownsSize = 81
        val emptyPuzzle: Array<IntArray> = Array(9) {
            IntArray(9)
        }

        // Act
        val sudokuSolver = SudokuSolver(emptyPuzzle)

        // Assert
        Assertions.assertEquals(expectedLocationOfUnknownsSize, sudokuSolver.locationOfUnknowns.size)
    }

    @Test
    fun `numberOfGuesses at start is 0`() {
        // Arrange
        val expectedNumberOfGuessesValue = 0
        val emptyPuzzle: Array<IntArray> = Array(9) {
            IntArray(9)
        }

        // Act
        val sudokuSolver = SudokuSolver(emptyPuzzle)

        // Assert
        Assertions.assertEquals(expectedNumberOfGuessesValue, sudokuSolver.numberOfGuesses)
    }

    @Test
    fun `numberOfUnknowns equals locationOfUnknowns size`() {
        // Arrange
        val emptyPuzzle: Array<IntArray> = Array(9) {
            IntArray(9)
        }

        // Act
        val sudokuSolver = SudokuSolver(emptyPuzzle)

        // Assert
        Assertions.assertEquals(sudokuSolver.locationOfUnknowns.size, sudokuSolver.numberOfUnknowns)
    }

    @Test
    fun `initialState and presentState are equal at start`() {
        // Arrange
        val emptyPuzzle: Array<IntArray> = Array(9) {
            IntArray(9)
        }

        // Act
        val sudokuSolver = SudokuSolver(emptyPuzzle)

        // Assert
        Assertions.assertEquals(sudokuSolver.initialState, sudokuSolver.presentState)
    }
}