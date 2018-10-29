package com.allanrodriguez.sudokusolver.viewmodels

import androidx.lifecycle.MutableLiveData

class CellViewModel {

    var cellValue: MutableLiveData<String> = MutableLiveData()
    var wasValueSetByUser: MutableLiveData<Boolean> = MutableLiveData()
    var isDuplicate: MutableLiveData<Boolean> = MutableLiveData()

    init { clear() }

    fun clear() {
        cellValue.value = ""
        isDuplicate.value = false
        wasValueSetByUser.value = true
    }
}
