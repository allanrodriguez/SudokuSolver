package com.allanrodriguez.libbruteforcesudokusolver

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class PuzzleTests {

    @Test
    fun `set throws IndexOutOfBoundsException when row is less than 0`() {
        // Arrange
        val invalidRow = -1
        val col = 0
        val value = 1
        val puzzle = Puzzle()

        // Act/Assert
        Assertions.assertThrows(IndexOutOfBoundsException::class.java) {
            puzzle[invalidRow, col] = value
        }
    }

    @Test
    fun `set throws IndexOutOfBoundsException when row is greater than 8`() {
        // Arrange
        val invalidRow = 9
        val col = 0
        val value = 1
        val puzzle = Puzzle()

        // Act/Assert
        Assertions.assertThrows(IndexOutOfBoundsException::class.java) {
            puzzle[invalidRow, col] = value
        }
    }

    @Test
    fun `set throws IndexOutOfBoundsException when col is less than 0`() {
        // Arrange
        val row = 0
        val invalidCol = -1
        val value = 1
        val puzzle = Puzzle()

        // Act/Assert
        Assertions.assertThrows(IndexOutOfBoundsException::class.java) {
            puzzle[row, invalidCol] = value
        }
    }

    @Test
    fun `set throws IndexOutOfBoundsException when col is greater than 8`() {
        // Arrange
        val row = 0
        val invalidCol = 9
        val value = 1
        val puzzle = Puzzle()

        // Act/Assert
        Assertions.assertThrows(IndexOutOfBoundsException::class.java) {
            puzzle[row, invalidCol] = value
        }
    }

    @Test
    fun `set sets puzzle cell at row and col to value`() {
        // Arrange
        val row = 0
        val col = 0
        val value = 1
        val puzzle = Puzzle()

        // Act
        puzzle[row, col] = value

        // Assert
        Assertions.assertEquals(value, puzzle[row, col])
    }

    @Test
    fun `set with Pair throws IndexOutOfBoundsException when row is less than 0`() {
        // Arrange
        val pairWithInvalidRow: Pair<Int, Int> = Pair(-1, 0)
        val value = 1
        val puzzle = Puzzle()

        // Act/Assert
        Assertions.assertThrows(IndexOutOfBoundsException::class.java) {
            puzzle[pairWithInvalidRow] = value
        }
    }

    @Test
    fun `set with Pair throws IndexOutOfBoundsException when row is greater than 8`() {
        // Arrange
        val pairWithInvalidRow: Pair<Int, Int> = Pair(9, 0)
        val value = 1
        val puzzle = Puzzle()

        // Act/Assert
        Assertions.assertThrows(IndexOutOfBoundsException::class.java) {
            puzzle[pairWithInvalidRow] = value
        }
    }

    @Test
    fun `set with Pair throws IndexOutOfBoundsException when col is less than 0`() {
        // Arrange
        val pairWithInvalidCol: Pair<Int, Int> = Pair(0, -1)
        val value = 1
        val puzzle = Puzzle()

        // Act/Assert
        Assertions.assertThrows(IndexOutOfBoundsException::class.java) {
            puzzle[pairWithInvalidCol] = value
        }
    }

    @Test
    fun `set with Pair throws IndexOutOfBoundsException when col is greater than 8`() {
        // Arrange
        val pairWithInvalidCol: Pair<Int, Int> = Pair(0, 9)
        val value = 1
        val puzzle = Puzzle()

        // Act/Assert
        Assertions.assertThrows(IndexOutOfBoundsException::class.java) {
            puzzle[pairWithInvalidCol] = value
        }
    }

    @Test
    fun `set with Pair sets puzzle cell at row and col to value`() {
        // Arrange
        val pair: Pair<Int, Int> = Pair(0, 0)
        val value = 1
        val puzzle = Puzzle()

        // Act
        puzzle[pair] = value

        // Assert
        Assertions.assertEquals(value, puzzle[pair])
    }

    @Test
    fun `get throws IndexOutOfBoundsException when row is less than 0`() {
        // Arrange
        val invalidRow = -1
        val col = 0
        val puzzle = Puzzle()

        // Act/Assert
        Assertions.assertThrows(IndexOutOfBoundsException::class.java) {
            puzzle[invalidRow, col]
        }
    }

    @Test
    fun `get throws IndexOutOfBoundsException when row is greater than 8`() {
        // Arrange
        val invalidRow = 9
        val col = 0
        val puzzle = Puzzle()

        // Act/Assert
        Assertions.assertThrows(IndexOutOfBoundsException::class.java) {
            puzzle[invalidRow, col]
        }
    }

    @Test
    fun `get throws IndexOutOfBoundsException when col is less than 0`() {
        // Arrange
        val row = 0
        val invalidCol = -1
        val puzzle = Puzzle()

        // Act/Assert
        Assertions.assertThrows(IndexOutOfBoundsException::class.java) {
            puzzle[row, invalidCol]
        }
    }

    @Test
    fun `get throws IndexOutOfBoundsException when col is greater than 8`() {
        // Arrange
        val row = 0
        val invalidCol = 9
        val puzzle = Puzzle()

        // Act/Assert
        Assertions.assertThrows(IndexOutOfBoundsException::class.java) {
            puzzle[row, invalidCol]
        }
    }

    @Test
    fun `get with Pair throws IndexOutOfBoundsException when row is less than 0`() {
        // Arrange
        val pairWithInvalidRow: Pair<Int, Int> = Pair(-1, 0)
        val puzzle = Puzzle()

        // Act/Assert
        Assertions.assertThrows(IndexOutOfBoundsException::class.java) {
            puzzle[pairWithInvalidRow]
        }
    }

    @Test
    fun `get with Pair throws IndexOutOfBoundsException when row is greater than 8`() {
        // Arrange
        val pairWithInvalidRow: Pair<Int, Int> = Pair(9, 0)
        val puzzle = Puzzle()

        // Act/Assert
        Assertions.assertThrows(IndexOutOfBoundsException::class.java) {
            puzzle[pairWithInvalidRow]
        }
    }

    @Test
    fun `get with Pair throws IndexOutOfBoundsException when col is less than 0`() {
        // Arrange
        val pairWithInvalidCol: Pair<Int, Int> = Pair(0, -1)
        val puzzle = Puzzle()

        // Act/Assert
        Assertions.assertThrows(IndexOutOfBoundsException::class.java) {
            puzzle[pairWithInvalidCol]
        }
    }

    @Test
    fun `get with Pair throws IndexOutOfBoundsException when col is greater than 8`() {
        // Arrange
        val pairWithInvalidCol: Pair<Int, Int> = Pair(0, 9)
        val puzzle = Puzzle()

        // Act/Assert
        Assertions.assertThrows(IndexOutOfBoundsException::class.java) {
            puzzle[pairWithInvalidCol]
        }
    }

    @Test
    fun `equals returns true when comparing two empty puzzles`() {
        // Arrange
        val puzzle1 = Puzzle()
        val puzzle2 = Puzzle()

        // Assert
        Assertions.assertEquals(puzzle1, puzzle2)
    }

    @Test
    fun `equals returns true when comparing two full puzzles`() {
        // Arrange
        val puzzle1 = Puzzle()
        val puzzle2 = Puzzle()

        for (i: Int in 0..8) {
            for (j: Int in 0..8) {
                puzzle1[i, j] = 1
                puzzle2[i, j] = 1
            }
        }

        // Assert
        Assertions.assertEquals(puzzle1, puzzle2)
    }

    @Test
    fun `equals returns false when comparing puzzles with different values`() {
        // Arrange
        val puzzle1 = Puzzle()
        val puzzle2 = Puzzle()

        puzzle1[0, 0] = 1
        puzzle2[0, 0] = 2

        // Assert
        Assertions.assertNotEquals(puzzle1, puzzle2)
    }

    @Test
    fun `equals returns false when comparing puzzle with object of a different type`() {
        // Arrange
        val puzzle1 = Puzzle()
        val puzzle2: Array<IntArray> = Array(9) {
            IntArray(9)
        }

        // Assert
        Assertions.assertNotEquals(puzzle1, puzzle2)
    }

    @Test
    fun `toString returns puzzle as string with all cells set to '_'`() {
        // Arrange
        val expectedPuzzleString: String =
                "+-------+-------+-------+\n" +
                "| _ _ _ | _ _ _ | _ _ _ |\n" +
                "| _ _ _ | _ _ _ | _ _ _ |\n" +
                "| _ _ _ | _ _ _ | _ _ _ |\n" +
                "+-------+-------+-------+\n" +
                "| _ _ _ | _ _ _ | _ _ _ |\n" +
                "| _ _ _ | _ _ _ | _ _ _ |\n" +
                "| _ _ _ | _ _ _ | _ _ _ |\n" +
                "+-------+-------+-------+\n" +
                "| _ _ _ | _ _ _ | _ _ _ |\n" +
                "| _ _ _ | _ _ _ | _ _ _ |\n" +
                "| _ _ _ | _ _ _ | _ _ _ |\n" +
                "+-------+-------+-------+\n"
        val puzzle = Puzzle()

        // Act
        val actualPuzzleString: String = puzzle.toString()

        // Assert
        Assertions.assertEquals(expectedPuzzleString, actualPuzzleString)
    }

    @Test
    fun `toString returns puzzle with solved cells as string with all cells set to their numbers`() {
        // Arrange
        val expectedPuzzleString: String =
                "+-------+-------+-------+\n" +
                "| 1 1 1 | 1 1 1 | 1 1 1 |\n" +
                "| 1 1 1 | 1 1 1 | 1 1 1 |\n" +
                "| 1 1 1 | 1 1 1 | 1 1 1 |\n" +
                "+-------+-------+-------+\n" +
                "| 1 1 1 | 1 1 1 | 1 1 1 |\n" +
                "| 1 1 1 | 1 1 1 | 1 1 1 |\n" +
                "| 1 1 1 | 1 1 1 | 1 1 1 |\n" +
                "+-------+-------+-------+\n" +
                "| 1 1 1 | 1 1 1 | 1 1 1 |\n" +
                "| 1 1 1 | 1 1 1 | 1 1 1 |\n" +
                "| 1 1 1 | 1 1 1 | 1 1 1 |\n" +
                "+-------+-------+-------+\n"
        val puzzle = Puzzle()

        // Setting all the puzzle's cells to 1
        for (row: Int in 0..8) {
            for (col: Int in 0..8) {
                puzzle[row, col] = 1
            }
        }

        // Act
        val actualPuzzleString: String = puzzle.toString()

        // Assert
        Assertions.assertEquals(expectedPuzzleString, actualPuzzleString)
    }
}
