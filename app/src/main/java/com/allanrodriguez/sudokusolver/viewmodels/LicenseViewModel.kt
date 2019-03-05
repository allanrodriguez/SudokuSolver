package com.allanrodriguez.sudokusolver.viewmodels

import android.os.Parcel
import android.os.Parcelable

data class LicenseViewModel(val title: String, val licenseContents: String) : Parcelable {

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeStringArray(arrayOf(title, licenseContents))
    }

    class Creator : Parcelable.Creator<LicenseViewModel> {

        override fun createFromParcel(source: Parcel?): LicenseViewModel {
            val fromParcel: Array<String> = Array(2) { "" }
            source?.readStringArray(fromParcel)

            return LicenseViewModel(fromParcel[0], fromParcel[1])
        }

        override fun newArray(size: Int): Array<LicenseViewModel?> {
            return Array(size) { null }
        }
    }

    companion object {
        @JvmField
        val CREATOR = Creator()
    }
}