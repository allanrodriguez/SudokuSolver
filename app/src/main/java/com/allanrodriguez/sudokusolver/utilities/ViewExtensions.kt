package com.allanrodriguez.sudokusolver.utilities

import android.widget.ImageButton
import com.allanrodriguez.sudokusolver.abstractions.ANIMATION_FAST_MILLIS

fun ImageButton.simulateClick(delay: Long = ANIMATION_FAST_MILLIS) {
    performClick()
    isPressed = true
    invalidate()
    postDelayed({
        invalidate()
        isPressed = false
    }, delay)
}