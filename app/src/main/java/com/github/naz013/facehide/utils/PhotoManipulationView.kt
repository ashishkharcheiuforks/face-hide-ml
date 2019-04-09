package com.github.naz013.facehide.utils

import android.content.Context
import android.util.AttributeSet
import android.view.View

class PhotoManipulationView : View {

    constructor(context: Context): this(context, null)
    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, deffStyleAttr: Int): super(context, attrs, deffStyleAttr) {

    }
}