package com.allanrodriguez.sudokusolver.views

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageButton 
import androidx.core.content.ContextCompat

import com.allanrodriguez.sudokusolver.R
import com.allanrodriguez.sudokusolver.abstractions.FlashState

class FlashButton : AppCompatImageButton {

    //region Properties
    private var flashListener: FlashListener? = null
    var flashState: FlashState = FlashState.OFF
        set(value) {
            if (field != value) {
                field = value
                val drawable: Int = when (field) {
                    FlashState.OFF -> {
                        flashListener?.onOff()
                        R.drawable.ic_flash_off_white_24dp
                    }
                    FlashState.ON -> {
                        flashListener?.onOn()
                        R.drawable.ic_flash_on_white_24dp
                    }
                    FlashState.AUTO -> {
                        flashListener?.onAuto()
                        R.drawable.ic_flash_auto_white_24dp
                    }
                }
                setImageDrawable(resources.getDrawable(drawable, null))
            }
        }
    //endregion

    //region Constructors
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        setBackgroundColor(ContextCompat.getColor(context, R.color.transparent))
        setImageDrawable(resources.getDrawable(R.drawable.ic_flash_off_white_24dp, null))
        setOnClickListener(null)
    }
    //endregion

    override fun setOnClickListener(l: OnClickListener?) {
        super.setOnClickListener { view ->
            flashState = when (flashState) {
                FlashState.OFF -> FlashState.ON
                FlashState.ON -> FlashState.AUTO
                FlashState.AUTO -> FlashState.OFF
            }
            l?.onClick(view)
        }
    }

    override fun setVisibility(visibility: Int) {
        super.setVisibility(visibility)
        flashState = FlashState.OFF
    }

    fun setOnFlashStateChangedListener(listener: FlashListener) {
        flashListener = listener
    }

    interface FlashListener {
        fun onAuto()
        fun onOn()
        fun onOff()
    }
}