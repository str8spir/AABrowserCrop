package com.kododake.aabrowser.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout

class InterceptTouchFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    var onInterceptTouchListener: ((MotionEvent) -> Unit)? = null

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        onInterceptTouchListener?.invoke(ev)
        return super.onInterceptTouchEvent(ev)
    }
}
