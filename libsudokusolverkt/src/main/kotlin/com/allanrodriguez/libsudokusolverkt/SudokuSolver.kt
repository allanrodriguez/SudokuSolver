package com.allanrodriguez.libsudokusolverkt

import com.allanrodriguez.libsudokusolverkt.abstractions.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.system.measureTimeMillis

class SudokuSolver(override val puzzle: IPuzzle) : ISolver {

    private var hasChangedSinceLastSweep: Boolean = true
    private var easyPassCount: Int = 0

    init {
        puzzle.print()

        val originalUnsolvedCount: Int = puzzle.unsolvedCount
        println("$originalUnsolvedCount cell(s) have yet to be solved.")

        for (i: Int in 0..8) {
            for (j: Int in 0..8) {
                val cell: ICell = puzzle.getCellAt(i, j)
                cell.addOnCellValueSetListener { c ->
                    if (c.value > 0) {
                        checkForDuplicates(c)
                        hasChangedSinceLastSweep = true
                        removePossibleValueFromGroups(c)
                    }
                }
                if (cell.value > 0) {
                    // If the cell value is greater than 0, change the possible values for all cell
                    // values of 0 in the row and column of cell value such that they do not include
                    // the cell value.
                    removePossibleValueFromGroups(cell)
                }
            }
        }

        println("Initial sweep solved ${originalUnsolvedCount - puzzle.unsolvedCount} cell(s).")
    }

    override fun clear() {
        puzzle.clear()
        hasChangedSinceLastSweep = true
    }

    override fun solve(): Boolean {
        val time: Long = measureTimeMillis {
            while (hasChangedSinceLastSweep && !puzzle.solved) {
                tryEasySolvingMethods()
            }
        }

        println("Solve attempt took $time ms to complete.")

        return puzzle.solved
    }

    private fun changePossibleCellValue(cell: ICell, value: Int) {

        if (cell.removePossible(value - 1) && cell.possibleSize == 2) {
            runBlocking {
                launch {
                    eliminatePairsFromGroup(cell, GroupType.ROW)
                }

                launch {
                    eliminatePairsFromGroup(cell, GroupType.COLUMN)
                }
            }
        }
    }

    private fun checkForDuplicates(cell: ICell) {
        if (cell.value == 0) {
            return
        }

        fun throwException(row: Int, col: Int) {
            puzzle.print()
            throw Exception("Duplicate values in conflict at (${cell.row}, ${cell.col}) and ($row, $col).")
        }

        val (subgridRow: Int, subgridCol: Int) = getTopLeftCoordinatesForSubgrid(cell.subgrid)

        for (i: Int in 0..8) {
            val duplicateInRow: Boolean = i != cell.col && puzzle.getCellAt(cell.row, i).value == cell.value
            val duplicateInCol: Boolean = i != cell.row && puzzle.getCellAt(i, cell.col).value == cell.value
            val duplicateInSubgrid: Boolean = subgridRow + i / 3 != cell.row && subgridCol + i % 3 != cell.col
                    && puzzle.getCellAt(subgridRow + i / 3, subgridCol + i % 3).value == cell.value

            when {
                duplicateInRow -> throwException(cell.row, i)
                duplicateInCol -> throwException(i, cell.col)
                duplicateInSubgrid -> throwException(subgridRow + i / 3, subgridRow + i % 3)
            }
        }
    }

    private fun eliminateFromSubgrid(subgrid: Int) {
        if (puzzle.isSubgridSolved(subgrid)) {
            return
        }

        val (startRow: Int, startCol: Int) = getTopLeftCoordinatesForSubgrid(subgrid)
        val blockInteractionCells: MutableList<ICell> = mutableListOf()
        val cellsWithPossibleValue: Array<MutableList<ICell>> = Array(9) { mutableListOf<ICell>() }

        for (i: Int in startRow..startRow + 2) {
            for (j: Int in startCol..startCol + 2) {
                val cell: ICell = puzzle.getCellAt(i, j)
                for (k: Int in 0..8) {
                    if (cell.isPossible(k)) {
                        cellsWithPossibleValue[k].add(cell)
                    }
                }
                if (cell.possibleSize <= 3) {
                    blockInteractionCells.add(cell)
                }
            }
        }

        for (i: Int in cellsWithPossibleValue.indices) {
            if (cellsWithPossibleValue[i].size == 1) {
                cellsWithPossibleValue[i][0].value = i + 1
            }
        }


    }

    private fun eliminatePairsFromGroup(cell: ICell, group: GroupType) {
        val possibleNumbers: MutableList<Int> = mutableListOf()

        for (i: Int in 0..8) {
            val foundCellWithSamePossiblePair: Boolean = when (group) {
                GroupType.COLUMN -> cell.row != i && cell.possibleEquals(puzzle.getCellAt(i, cell.col))
                GroupType.ROW -> cell.col != i && cell.possibleEquals(puzzle.getCellAt(cell.row, i))
                else -> throw Exception("Invalid condition, GroupType should be either ROW or COLUMN.")
            }
            if (foundCellWithSamePossiblePair) {
                for (j: Int in 0..8) {
                    if (cell.isPossible(j)) {
                        possibleNumbers.add(j)
                    }
                }
                break
            }
        }

        for (i: Int in 0..8) {
            val currentCell: ICell = when (group) {
                GroupType.COLUMN -> puzzle.getCellAt(i, cell.col)
                GroupType.ROW -> puzzle.getCellAt(cell.row, i)
                else -> throw Exception("Invalid condition, GroupType should be either ROW or COLUMN.")
            }
            if (!cell.possibleEquals(currentCell)) {
                for (number: Int in possibleNumbers) {
                    if (currentCell.isPossible(number)) {
                        changePossibleCellValue(currentCell, number + 1)
                    }
                }
            }
        }
    }

    private fun tryEasySolvingMethods() {
        hasChangedSinceLastSweep = false
        val originalUnsolvedCount: Int = puzzle.unsolvedCount

        for (i: Int in 0..8) {
            eliminateFromSubgrid(i)
        }

        println("Pass ${++easyPassCount} with easy solving methods solved ${originalUnsolvedCount - puzzle.unsolvedCount} cell(s).")
    }

    private fun removePossibleValueFromGroup(cell: ICell, group: GroupType) {
        val (startRow: Int, startCol: Int) = getTopLeftCoordinatesForSubgrid(cell.subgrid)

        for (i: Int in 0..8) {
            val currentCell: ICell = when (group) {
                GroupType.COLUMN -> puzzle.getCellAt(cell.row, i)
                GroupType.ROW -> puzzle.getCellAt(i, cell.col)
                GroupType.SUBGRID -> puzzle.getCellAt(startRow + i / 3, startCol + i % 3)
            }
            if (currentCell.value == 0) {
                changePossibleCellValue(currentCell, cell.value)
            }
        }
    }

    private fun removePossibleValueFromGroups(cell: ICell) = runBlocking {
        launch {
            removePossibleValueFromGroup(cell, GroupType.ROW)
        }

        launch {
            removePossibleValueFromGroup(cell, GroupType.COLUMN)
        }

        launch {
            removePossibleValueFromGroup(cell, GroupType.SUBGRID)
        }
    }
}