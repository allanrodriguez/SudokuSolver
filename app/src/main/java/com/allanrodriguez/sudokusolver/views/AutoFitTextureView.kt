package com.allanrodriguez.sudokusolver.views

import android.content.Context
import android.util.AttributeSet
import android.view.TextureView
import java.lang.IllegalArgumentException

/**
 * A {@link TextureView} that can be adjusted to a specified aspect ratio.
 */
class AutoFitTextureView : TextureView {

    private var ratioWidth: Int = 0
    private var ratioHeight: Int = 0

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val width: Int = MeasureSpec.getSize(widthMeasureSpec)
        val height: Int = MeasureSpec.getSize(heightMeasureSpec)

        var ratio = 0.0
        if (ratioHeight > 0) {
            ratio = ratioWidth.toDouble() / ratioHeight
        }

        var widthHeight = 0.0
        if (height > 0) {
            widthHeight = width.toDouble() / height
        }

        if (ratio == 0.0 || ratio == widthHeight) {
            setMeasuredDimension(width, height)
        } else if (ratio < widthHeight) {
            setMeasuredDimension(width, (width * ratioHeight.toDouble() / ratioWidth).toInt())
        } else {
            setMeasuredDimension((height * ratioWidth.toDouble() / ratioHeight).toInt(), height)
        }
    }

    fun setAspectRatio(width: Int, height: Int) {
        if (width < 0 || height < 0) {
            throw IllegalArgumentException("Size cannot be negative.")
        }
        ratioWidth =  width
        ratioHeight = height
        requestLayout()
    }
}
