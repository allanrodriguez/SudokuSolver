package com.allanrodriguez.libsudokusolverkt.abstractions

interface ISolver {

    val puzzle: IPuzzle

    fun clear()
    fun solve(): Boolean
}