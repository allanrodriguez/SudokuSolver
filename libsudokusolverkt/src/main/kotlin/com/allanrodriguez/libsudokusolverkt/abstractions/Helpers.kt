package com.allanrodriguez.libsudokusolverkt.abstractions

fun getTopLeftCoordinatesForSubgrid(subgrid: Int): Pair<Int, Int> {
    if (subgrid < 0 || subgrid > 8) {
        throw IndexOutOfBoundsException("Subgrid should be between 0 and 8")
    }
    val dividedSubgrid: Int = subgrid / 3
    val moduloSubgrid: Int = subgrid % 3

    return Pair(3 * dividedSubgrid, 3 * moduloSubgrid)
}

fun getSubgridFromCoordinates(row: Int, col: Int): Int {
    if (row < 0 || row > 8 || col < 0 || col > 8) {
        throw IndexOutOfBoundsException("Row and column should be between 0 and 8")
    }

    val dividedRow = row / 3

    return 3 * dividedRow + col / 3
}