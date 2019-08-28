package com.allanrodriguez.sudokusolver.di.modules

import com.allanrodriguez.sudokusolver.fragments.EnterPuzzleFragment
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class FragmentModule {

    @ContributesAndroidInjector(modules = [ViewModelFactoryModule::class])
    abstract fun enterPuzzleFragment(): EnterPuzzleFragment
}
