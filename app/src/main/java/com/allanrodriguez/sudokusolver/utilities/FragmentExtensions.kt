package com.allanrodriguez.sudokusolver.utilities

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.allanrodriguez.sudokusolver.R
import com.allanrodriguez.sudokusolver.fragments.EnterPuzzleFragment

private const val TAG = "FragmentExtensions"

fun Fragment.showDialogFragment(dialog: DialogFragment) {
    val localFragmentManager: FragmentManager? = fragmentManager

    if (localFragmentManager == null) {
        Log.e(EnterPuzzleFragment.TAG, "FragmentManager was null when attempting to show dialog fragment.")
        return
    }

    // Show the dialog in a small pop-up window if app is run on a tablet.
    if (resources.getBoolean(R.bool.large_layout)) {
        dialog.show(localFragmentManager, dialog::class.java.simpleName)
    } else {
        localFragmentManager
                .beginTransaction()
                .setCustomAnimations(
                        R.anim.slide_up,
                        R.anim.slide_down,
                        R.anim.slide_up,
                        R.anim.slide_down
                )
                .add(android.R.id.content, dialog, dialog::class.java.simpleName)
                .addToBackStack(null)
                .commit()
    }
}

fun AppCompatActivity.showDialogFragment(dialog: DialogFragment) {
    val localFragmentManager: FragmentManager? = supportFragmentManager

    if (localFragmentManager == null) {
        Log.e(TAG, "FragmentManager was null when attempting to show dialog fragment.")
        return
    }

    // Show the dialog in a small pop-up window if app is run on a tablet.
    if (resources.getBoolean(R.bool.large_layout)) {
        dialog.show(localFragmentManager, dialog::class.java.simpleName)
    } else {
        localFragmentManager
                .beginTransaction()
                .setCustomAnimations(
                        R.anim.slide_up,
                        R.anim.slide_down,
                        R.anim.slide_up,
                        R.anim.slide_down
                )
                .add(android.R.id.content, dialog, dialog::class.java.simpleName)
                .addToBackStack(null)
                .commit()
    }
}