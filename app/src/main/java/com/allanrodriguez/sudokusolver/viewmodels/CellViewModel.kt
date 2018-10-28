package com.allanrodriguez.sudokusolver.viewmodels

import androidx.lifecycle.MutableLiveData

class CellViewModel {

    var cellValue: MutableLiveData<String> = MutableLiveData()
    var wasValueSetByUser: MutableLiveData<Boolean> = MutableLiveData()
    var isDuplicate: MutableLiveData<Boolean> = MutableLiveData()

    init {
        wasValueSetByUser.value = true
        cellValue.value = ""
        isDuplicate.value = false
    }
}
