package com.allanrodriguez.libsudokusolverkt

import com.allanrodriguez.libsudokusolverkt.abstractions.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.system.measureTimeMillis

class SudokuSolver(override val puzzle: IPuzzle) : ISolver {

    private val originalUnsolvedCount: Int = puzzle.unsolvedCount

    private var hasChangedSinceLastSweep: Boolean = true
    private var easyPassCount: Int = 0
    private var mediumPassCount: Int = 0

    init {
        puzzle.print()

        println("$originalUnsolvedCount cell(s) have yet to be solved.")

        for (i: Int in 0..8) {
            for (j: Int in 0..8) {
                val cell: ICell = puzzle.getCellAt(i, j)
                cell.addOnCellValueSetListener { c ->
                    if (c.value > 0) {
                        checkForDuplicates(c)
                        println("Cell at (${c.row}, ${c.col}) solved with value ${c.value}.")
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
                hasChangedSinceLastSweep = false

                tryEasySolvingMethods()

                if (!hasChangedSinceLastSweep) {
                    tryModerateSolvingMethods()
                }
            }
        }

        println("*** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** ***")
        println("Solve attempt took $time ms to complete.")
        println("${originalUnsolvedCount - puzzle.unsolvedCount} cell(s) were solved.")

        return puzzle.solved
    }

    private fun blockRowColInteraction(cells: List<ICell>, possibleValueToRemove: Int) {
        var haveSameRow = true
        var haveSameCol = true

        val firstCell: ICell = cells[0]

        for (cell: ICell in cells) {
            haveSameRow = haveSameRow && cell.row == firstCell.row
            haveSameCol = haveSameCol && cell.col == firstCell.col
        }

        for (i: Int in 0..8) {
            val currentCell: ICell = when {
                haveSameCol -> puzzle.getCellAt(i, firstCell.col)
                haveSameRow -> puzzle.getCellAt(firstCell.row, i)
                else -> return
            }
            if (currentCell.subgrid != firstCell.subgrid && currentCell.isPossibleValue(possibleValueToRemove)) {
                println("blockRowColInteraction: Removed $possibleValueToRemove from cell at (${currentCell.row}, ${currentCell.col})")
                changePossibleCellValue(currentCell, possibleValueToRemove)
            }
        }
    }

    private fun changePossibleCellValue(cell: ICell, value: Int) {
        if (cell.removePossibleValue(value)) {
            println("changePossibleCellValue: Removed $value from cell at (${cell.row}, ${cell.col})")
            hasChangedSinceLastSweep = true

            if (cell.possibleSize == 2 || cell.possibleSize == 3) {
                runBlocking {
                    launch {
                        eliminateNakedSubsets(cell, GroupType.ROW)
                    }

                    launch {
                        eliminateNakedSubsets(cell, GroupType.COLUMN)
                    }
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
        val cellsWithPossibleValue: Array<MutableList<ICell>> = Array(9) { mutableListOf<ICell>() }

        for (i: Int in startRow..startRow + 2) {
            for (j: Int in startCol..startCol + 2) {
                val cell: ICell = puzzle.getCellAt(i, j)
                if (cell.value > 0) {
                    continue
                }

                for (k: Int in 0..8) {
                    if (cell.isPossibleValue(k + 1)) {
                        cellsWithPossibleValue[k].add(cell)
                    }
                }
            }
        }

        var cellSolved = true

        while (cellSolved) {
            cellSolved = false

            for (i: Int in cellsWithPossibleValue.indices) {
                if (cellsWithPossibleValue[i].size == 1) {
                    cellsWithPossibleValue[i][0].value = i + 1
                    cellsWithPossibleValue[i].clear()
                    cellSolved = true
                }
            }
        }

        for (i: Int in cellsWithPossibleValue.indices) {
            if (cellsWithPossibleValue[i].isNotEmpty() && cellsWithPossibleValue[i].size <= 3) {
                blockRowColInteraction(cellsWithPossibleValue[i], i + 1)
            }
        }
    }

    private fun eliminateNakedSubsets(cell: ICell, group: GroupType) {
        val possibleNumbers: MutableList<Int> = mutableListOf()
        var cellsWithSamePossibleValues = 0

        for (i: Int in 0..8) {
            val foundCellWithSamePossibleValues: Boolean = when (group) {
                GroupType.COLUMN -> cell.row != i && cell.possibleEquals(puzzle.getCellAt(i, cell.col))
                GroupType.ROW -> cell.col != i && cell.possibleEquals(puzzle.getCellAt(cell.row, i))
                else -> throw Exception("Invalid condition, GroupType should be either ROW or COLUMN.")
            }

            if (foundCellWithSamePossibleValues) {
                ++cellsWithSamePossibleValues
            }

            if (cellsWithSamePossibleValues == cell.possibleSize) {
                for (j: Int in 1..9) {
                    if (cell.isPossibleValue(j)) {
                        possibleNumbers.add(j)
                    }
                }
                break
            }
        }

        if (possibleNumbers.isEmpty()) {
            return
        }

        for (i: Int in 0..8) {
            val currentCell: ICell = when (group) {
                GroupType.COLUMN -> puzzle.getCellAt(i, cell.col)
                GroupType.ROW -> puzzle.getCellAt(cell.row, i)
                else -> throw Exception("Invalid condition, GroupType should be either ROW or COLUMN.")
            }
            if (!cell.possibleEquals(currentCell)) {
                for (number: Int in possibleNumbers) {
                    if (currentCell.isPossibleValue(number)) {
                        println("eliminateNakedSubsets: Removed $number from cell at (${currentCell.row}, ${currentCell.col})")
                        changePossibleCellValue(currentCell, number)
                    }
                }
            }
        }
    }

    private fun findGroupIndexWithIdenticalCellPair(pair: Triple<Int, Int, Int>, startIndex: Int, group: GroupType): Int {
        for (i: Int in startIndex..8) {
            var possibleCount = 0

            when (group) {
                GroupType.COLUMN -> {
                    if (puzzle.getCellAt(pair.first, i).isPossibleValue(pair.third)
                            && puzzle.getCellAt(pair.second, i).isPossibleValue(pair.third)) {
                        for (j: Int in 0..8) {
                            if (puzzle.getCellAt(j, i).isPossibleValue(pair.third)) {
                                ++possibleCount
                            }
                        }
                    }
                }
                GroupType.ROW -> {
                    if (puzzle.getCellAt(i, pair.first).isPossibleValue(pair.third)
                            && puzzle.getCellAt(i, pair.second).isPossibleValue(pair.third)) {
                        for (j: Int in 0..8) {
                            if (puzzle.getCellAt(i, j).isPossibleValue(pair.third)) {
                                ++possibleCount
                            }
                        }
                    }
                }
                GroupType.SUBGRID -> throw Exception("Invalid condition, GroupType should be either ROW or COLUMN.")
            }

            if (possibleCount == 2) {
                return i
            }
        }

        return -1
    }

    private fun getCellPairsWithUniquePossibleValue(index: Int, group: GroupType): List<Triple<Int, Int, Int>> {
        val cellPairs: MutableList<Triple<Int, Int, Int>> = mutableListOf()
        val cellsWithPossibleValue: Array<MutableList<Int>> = Array(9) { mutableListOf<Int>() }

        for (i: Int in 0..8) {
            val cell: ICell = when (group) {
                GroupType.COLUMN -> puzzle.getCellAt(i, index)
                GroupType.ROW -> puzzle.getCellAt(index, i)
                GroupType.SUBGRID -> throw Exception("Invalid condition, GroupType should be either ROW or COLUMN.")
            }
            if (cell.value > 0) {
                continue
            }

            for (j: Int in 0..8) {
                if (cell.isPossibleValue(j + 1)) {
                    val otherCoordinate: Int = when (group) {
                        GroupType.COLUMN -> cell.row
                        GroupType.ROW -> cell.col
                        GroupType.SUBGRID -> throw Exception("Invalid condition, GroupType should be either ROW or COLUMN.")
                    }
                    cellsWithPossibleValue[j].add(otherCoordinate)
                }
            }
        }

        for (i: Int in cellsWithPossibleValue.indices) {
            if (cellsWithPossibleValue[i].size == 2) {
                cellPairs.add(Triple(cellsWithPossibleValue[i][0], cellsWithPossibleValue[i][1], i + 1))
            }
        }

        return cellPairs
    }

    private fun tryEasySolvingMethods() {
        val originalUnsolvedCount: Int = puzzle.unsolvedCount

        for (i: Int in 0..8) {
            eliminateFromSubgrid(i)
        }

        println("Pass ${++easyPassCount} with easy solving methods solved ${originalUnsolvedCount - puzzle.unsolvedCount} cell(s).")
    }

    private fun tryModerateSolvingMethods() = runBlocking {
        val originalUnsolvedCount: Int = puzzle.unsolvedCount

        launch {
            xWing(GroupType.ROW)
        }

        launch {
            xWing(GroupType.COLUMN)
        }

        println("Pass ${++mediumPassCount} with medium solving methods solved ${originalUnsolvedCount - puzzle.unsolvedCount} cell(s).")
    }

    private fun removePossibleValueFromGroup(cell: ICell, group: GroupType) {
        val (startRow: Int, startCol: Int) = getTopLeftCoordinatesForSubgrid(cell.subgrid)

        for (i: Int in 0..8) {
            val currentCell: ICell = when (group) {
                GroupType.COLUMN -> puzzle.getCellAt(cell.row, i)
                GroupType.ROW -> puzzle.getCellAt(i, cell.col)
                GroupType.SUBGRID -> puzzle.getCellAt(startRow + i / 3, startCol + i % 3)
            }
            if (currentCell.isPossibleValue(cell.value)) {
                println("removePossibleValueFromGroup: Removed ${cell.value} from cell at (${currentCell.row}, ${currentCell.col})")
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

    private fun xWing(group: GroupType) {
        val originalUnsolvedCount: Int = puzzle.unsolvedCount

        for (i: Int in 0..7) {
            val cellPairs: List<Triple<Int, Int, Int>> = getCellPairsWithUniquePossibleValue(i, group)

            for (pair: Triple<Int, Int, Int> in cellPairs) {
                val identicalGroupIndex: Int = findGroupIndexWithIdenticalCellPair(pair, i + 1, group)
                if (identicalGroupIndex < 0) {
                    break
                }

                for (j: Int in 0..8) {
                    if (j != i && j != identicalGroupIndex) {
                        for (k: Int in listOf(pair.first, pair.second)) {
                            val cell: ICell = when (group) {
                                GroupType.COLUMN -> puzzle.getCellAt(k, j)
                                GroupType.ROW -> puzzle.getCellAt(j, k)
                                GroupType.SUBGRID -> throw Exception("Invalid condition, GroupType should be either ROW or COLUMN.")
                            }
                            if (cell.isPossibleValue(pair.third)) {
                                changePossibleCellValue(cell, pair.third)
                            }

                            if (puzzle.unsolvedCount < originalUnsolvedCount) {
                                return
                            }
                        }
                    }
                }
            }
        }
    }
}