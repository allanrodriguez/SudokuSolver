package com.allanrodriguez.sudokusolver.di.components

import android.content.Context
import com.allanrodriguez.sudokusolver.Application
import com.allanrodriguez.sudokusolver.di.modules.FragmentModule
import com.allanrodriguez.sudokusolver.di.modules.SudokuSolverFactoryModule
import com.allanrodriguez.sudokusolver.di.modules.ViewModelModule
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjectionModule
import dagger.android.AndroidInjector
import javax.inject.Singleton

@Singleton
@Component(modules = [AndroidInjectionModule::class, FragmentModule::class, SudokuSolverFactoryModule::class, ViewModelModule::class])
interface ApplicationComponent : AndroidInjector<Application> {

    @Component.Factory
    interface Factory {

        fun create(@BindsInstance applicationContext: Context): ApplicationComponent
    }
}
