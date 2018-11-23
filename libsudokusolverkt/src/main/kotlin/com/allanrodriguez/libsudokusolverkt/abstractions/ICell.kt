package com.allanrodriguez.libsudokusolverkt.abstractions

interface ICell {

    val possibleSize: Int
    val row: Int
    val col: Int
    val subgrid: Int
    var value: Int

    fun removePossibleValue(number: Int): Boolean
    fun isPossibleValue(number: Int): Boolean
    fun possibleEquals(cell: ICell): Boolean

    fun copy(): ICell
    fun clear()

    fun addOnCellValueSetListener(listener: (ICell) -> Unit)
    fun removeOnCellValueSetListener(listener: (ICell) -> Unit)

}