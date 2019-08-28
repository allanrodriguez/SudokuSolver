package com.allanrodriguez.sudokusolver.di.modules

import com.allanrodriguez.sudokusolver.abstractions.ISudokuSolverFactory
import com.allanrodriguez.sudokusolver.factories.SudokuSolverFactory
import dagger.Binds
import dagger.Module

@Module
abstract class SudokuSolverFactoryModule {

    @Binds
    abstract fun bindSudokuSolverFactory(factory: SudokuSolverFactory): ISudokuSolverFactory
}
