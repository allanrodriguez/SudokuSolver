package com.allanrodriguez.libsudokusolverkt

import com.allanrodriguez.libsudokusolverkt.abstractions.ICell
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CellTests {

    @Test
    fun `calls added listener when value is set`() {
        // Arrange
        var wasListenerCalled = false
        val listener: (ICell) -> Unit = { wasListenerCalled = true }
        val cell = Cell(0, 0)

        cell.addOnCellValueSetListener(listener)

        // Act
        cell.value = 3

        // Assert
        assertTrue(wasListenerCalled)
    }

    @Test
    fun `does not call added listener the second time the value is set to the same number`() {
        // Arrange
        var numTimesListenerCalled = 0
        val listener: (ICell) -> Unit = { ++numTimesListenerCalled }
        val cell = Cell(0, 0)

        cell.addOnCellValueSetListener(listener)

        // Act
        cell.value = 3
        cell.value = 3

        // Assert
        assertEquals(1, numTimesListenerCalled)
    }

    @Test
    fun `calls added listener twice when the value is set to different numbers`() {
        // Arrange
        var numTimesListenerCalled = 0
        val listener: (ICell) -> Unit = { ++numTimesListenerCalled }
        val cell = Cell(0, 0)

        cell.addOnCellValueSetListener(listener)

        // Act
        cell.value = 3
        cell.value = 8

        // Assert
        assertEquals(2, numTimesListenerCalled)
    }

    @Test
    fun `setting value greater than 0 sets all possible values to false`() {
        // Arrange
        val cell = Cell(0, 0)

        // Act
        cell.value = 4

        // Assert
        for (i: Int in 0..8) {
            assertFalse(cell.isPossible(i))
        }
    }

    @Test
    fun `setting value greater than 0 sets possibleSize to 0`() {
        // Arrange
        val cell = Cell(0, 0)

        // Act
        cell.value = 4

        // Assert
        assertEquals(0, cell.possibleSize)
    }

    @Test
    fun `clear sets value to 0`() {
        // Arrange
        val cell = Cell(0, 0)
        cell.value = 4

        // Act
        cell.clear()

        // Assert
        assertEquals(0, cell.value)
    }

    @Test
    fun `clear sets possibleSize to 9`() {
        // Arrange
        val cell = Cell(0, 0)
        cell.value = 4

        // Act
        cell.clear()

        // Assert
        assertEquals(9, cell.possibleSize)
    }

    @Test
    fun `removePossible returns false when value is greater than 0`() {
        // Arrange
        val cell = Cell(0, 0)
        cell.value = 4

        // Act
        val removePossibleResult: Boolean = cell.removePossible(2)

        // Assert
        assertFalse(removePossibleResult)
    }

    @Test
    fun `removePossible returns false when number specified is not a possible value`() {
        // Arrange
        val cell = Cell(0, 0)
        cell.removePossible(2)

        // Act
        val removePossibleResult: Boolean = cell.removePossible(2)

        // Assert
        assertFalse(removePossibleResult)
    }

    @Test
    fun `removePossible returns true when number specified is a possible value`() {
        // Arrange
        val cell = Cell(0, 0)

        // Act
        val removePossibleResult: Boolean = cell.removePossible(2)

        // Assert
        assertTrue(removePossibleResult)
    }

    @Test
    fun `removePossible removes specified number from being a possible value`() {
        // Arrange
        val cell = Cell(0, 0)

        // Act
        cell.removePossible(2)

        val is2StillAPossibleValue: Boolean = cell.isPossible(2)

        // Assert
        assertFalse(is2StillAPossibleValue)
    }

    @Test
    fun `value is set to last possible value`() {
        // Arrange
        val cell = Cell(0, 0)

        // Act
        for (i: Int in 1..8) {
            cell.removePossible(i)
        }

        // Assert
        assertEquals(1, cell.value)
    }
}