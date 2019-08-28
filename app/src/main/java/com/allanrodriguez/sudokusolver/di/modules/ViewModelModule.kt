package com.allanrodriguez.sudokusolver.di.modules

import androidx.lifecycle.ViewModel
import com.allanrodriguez.sudokusolver.viewmodels.EnterPuzzleViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class ViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(EnterPuzzleViewModel::class)
    abstract fun bindEnterPuzzleViewModel(viewModel: EnterPuzzleViewModel): ViewModel
}
