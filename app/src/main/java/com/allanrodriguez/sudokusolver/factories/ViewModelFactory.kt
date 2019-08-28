package com.allanrodriguez.sudokusolver.factories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class ViewModelFactory @Inject constructor(private val creators: @JvmSuppressWildcards Map<Class<out ViewModel>, Provider<ViewModel>>) :
        ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val creator = creators[modelClass]

        @Suppress("UNCHECKED_CAST")
        return (creator
                ?: creators.filterKeys { classKey -> modelClass.isAssignableFrom(classKey) }.values.first()).get() as T
    }
}
