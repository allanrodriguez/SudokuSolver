package com.allanrodriguez.libsudokusolverkt

import com.allanrodriguez.libsudokusolverkt.abstractions.ICell
import com.allanrodriguez.libsudokusolverkt.abstractions.getSubgridFromCoordinates

class Cell(override val row: Int, override val col: Int) : ICell {

    //region Properties
    private val onCellValueSetListeners: MutableList<(ICell) -> Unit> = mutableListOf()
    private val possibleValues: BooleanArray = BooleanArray(9) { true }

    override var possibleSize: Int = 9
        private set

    override val subgrid: Int = getSubgridFromCoordinates(row, col)

    override var value: Int = 0
        set(value) {
            if (field != value) {
                field = value
                if (value > 0) {
                    possibleValues.fill(false)
                } else {
                    possibleValues.fill(true)
                }
                possibleSize = if (value > 0) 0 else 9
                onCellValueSet()
            }
        }
    //endregion

    private constructor(originalCell: ICell) : this(originalCell.row, originalCell.col) {
        if (originalCell.value > 0) {
            value = originalCell.value
        } else {
            for (i: Int in possibleValues.indices) {
                possibleValues[i] = originalCell.isPossibleValue(i + 1)
            }
            possibleSize = originalCell.possibleSize
        }
    }

    override fun addOnCellValueSetListener(listener: (ICell) -> Unit) {
        onCellValueSetListeners.add(listener)
    }

    override fun copy(): ICell = Cell(this)

    override fun clear() {
        value = 0
    }

    override fun isPossibleValue(number: Int): Boolean {
        return possibleValues[number - 1]
    }

    override fun possibleEquals(cell: ICell): Boolean {
        for (i: Int in possibleValues.indices) {
            if (possibleValues[i] != cell.isPossibleValue(i + 1)) {
                return false
            }
        }

        return true
    }

    override fun removeOnCellValueSetListener(listener: (ICell) -> Unit) {
        if (onCellValueSetListeners.contains(listener)) {
            onCellValueSetListeners.remove(listener)
        }
    }

    override fun removePossibleValue(number: Int): Boolean {
        val zeroIndexedNumber: Int = number - 1

        if (value > 0 || !possibleValues[zeroIndexedNumber]) {
            return false
        }

        possibleValues[zeroIndexedNumber] = false
        --possibleSize

        if (possibleSize == 1) {
            for (i: Int in possibleValues.indices) {
                if (possibleValues[i]) {
                    value = i + 1

                    return true
                }
            }
        }

        return true
    }

    private fun onCellValueSet() {
        for (listener: (ICell) -> Unit in onCellValueSetListeners) {
            listener(this)
        }
    }
}