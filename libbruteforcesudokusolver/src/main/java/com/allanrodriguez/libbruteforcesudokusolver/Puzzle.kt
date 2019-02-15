package com.allanrodriguez.libbruteforcesudokusolver

import com.allanrodriguez.libbruteforcesudokusolver.abstractions.IPuzzle

class Puzzle : IPuzzle {

    private val puzzle: Array<IntArray> = Array(9) { IntArray(9) }

    override operator fun get(location: Pair<Int, Int>): Int {
        if (location.first < 0 || location.first > 8) {
            throw IndexOutOfBoundsException("first")
        }
        if (location.second < 0 || location.second > 8) {
            throw IndexOutOfBoundsException("second")
        }

        return puzzle[location.first][location.second]
    }

    override operator fun get(row: Int, col: Int): Int {
        if (row < 0 || row > 8) {
            throw IndexOutOfBoundsException("row")
        }
        if (col < 0 || col > 8) {
            throw IndexOutOfBoundsException("col")
        }

        return puzzle[row][col]
    }

    operator fun set(row: Int, col: Int, value: Int) {
        if (row < 0 || row > 8) {
            throw IndexOutOfBoundsException("row")
        }
        if (col < 0 || col > 8) {
            throw IndexOutOfBoundsException("col")
        }

        puzzle[row][col] = value
    }

    operator fun set(location: Pair<Int, Int>, value: Int) {
        if (location.first < 0 || location.first > 8) {
            throw IndexOutOfBoundsException("first")
        }
        if (location.second < 0 || location.second > 8) {
            throw IndexOutOfBoundsException("second")
        }

        puzzle[location.first][location.second] = value
    }

    override fun toString(): String {
        val sb = StringBuilder()

        for (i: Int in puzzle.indices) {
            if (i % 3 == 0) {
                sb.append("+-------+-------+-------+\n")
            }
            for (j: Int in puzzle[i].indices) {
                if (j % 3 == 0) {
                    sb.append("| ")
                }
                if (puzzle[i][j] > 0) {
                    sb.append(puzzle[i][j]).append(" ")
                } else {
                    sb.append("_ ")
                }
            }
            sb.append("|\n")
        }
        sb.append("+-------+-------+-------+\n")

        return sb.toString()
    }
}