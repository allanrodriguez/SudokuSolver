package com.allanrodriguez.sudokusolver.viewmodels

import androidx.lifecycle.MutableLiveData

class CellViewModel {

    var cellValue: MutableLiveData<String> = MutableLiveData()
    var isDuplicate: MutableLiveData<Boolean> = MutableLiveData()
    var wasValueSetByUser: MutableLiveData<Boolean> = MutableLiveData()
    var willValueBeSetByUser: Boolean = true

    init {
        clear()
    }

    fun clear() {
        cellValue.value = ""
        isDuplicate.value = false
        wasValueSetByUser.value = true
    }

    fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        if (willValueBeSetByUser && wasValueSetByUser.value != true) {
            wasValueSetByUser.value = true
        } else if (!willValueBeSetByUser && wasValueSetByUser.value != false) {
            wasValueSetByUser.value = false
            willValueBeSetByUser = true
        }
    }

    fun setValue(value: String) {
        willValueBeSetByUser = false
        cellValue.value = value
    }
}
