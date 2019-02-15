package com.allanrodriguez.libbruteforcesudokusolver.abstractions

interface IPuzzle {

    operator fun get(row: Int, col: Int): Int
    operator fun get(location: Pair<Int, Int>): Int
}
