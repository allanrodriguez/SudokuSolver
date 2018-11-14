package com.allanrodriguez.libsudokusolverkt

import com.allanrodriguez.libsudokusolverkt.abstractions.ICell
import com.allanrodriguez.libsudokusolverkt.abstractions.IPuzzle
import com.allanrodriguez.libsudokusolverkt.abstractions.getTopLeftCoordinatesForSubgrid

class Puzzle() : IPuzzle {

    //region Properties
    override var unsolvedCount: Int = 81

    override val solved: Boolean
        get() = unsolvedCount == 0

    private val puzzle: Array<Array<ICell>> = Array(9) { row ->
        Array(9) { col ->
            val cell: ICell = Cell(row, col)
            cell.addOnCellValueSetListener { c ->
                if (c.value > 0) {
                    --unsolvedCount
                } else {
                    ++unsolvedCount
                }
            }
            cell
        }
    }
    //endregion

    constructor(puzzle: IntArray) : this() {
        if (puzzle.size != 81) {
            throw Exception("Array is not of size 81.")
        }

        for (i: Int in puzzle.indices) {
            if (puzzle[i] > 0) {
                getCellAt(i / 9, i % 9).value = puzzle[i]
            }
        }
    }

    override fun clear() {
        for (row: Array<ICell> in puzzle) {
            for (cell: ICell in row) {
                cell.clear()
            }
        }
        unsolvedCount = 81
    }

    override fun getCellAt(row: Int, col: Int): ICell {
        return puzzle[row][col]
    }

    override fun isSubgridSolved(subgrid: Int): Boolean {
        val (subgridRow: Int, subgridCol: Int) = getTopLeftCoordinatesForSubgrid(subgrid)

        for (i: Int in 0..8) {
            if (puzzle[subgridRow + i / 3][subgridCol + i % 3].value == 0) {
                return false
            }
        }

        return true
    }

    override fun print() {
        val sb = StringBuilder()

        for (i: Int in puzzle.indices) {
            if (i % 3 == 0) {
                sb.append("+-------+-------+-------+\n")
            }
            for (j: Int in puzzle[i].indices) {
                if (j % 3 == 0) {
                    sb.append("| ")
                }
                if (puzzle[i][j].value > 0) {
                    sb.append(puzzle[i][j].value).append(" ")
                } else {
                    sb.append("_ ")
                }
            }
            sb.append("|\n")
        }
        sb.append("+-------+-------+-------+\n")

        println(sb.toString())
    }
}