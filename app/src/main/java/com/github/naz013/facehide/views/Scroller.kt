package com.github.naz013.facehide.views

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageView
import com.github.naz013.facehide.R
import com.github.naz013.facehide.utils.ViewUtils

class Scroller : LinearLayout {

    private var scrollView: HorizontalScrollView
    private val masks: MutableList<PhotoManipulationView.Mask> = mutableListOf()

    constructor(context: Context): this(context, null)
    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, deffStyleAttr: Int): super(context, attrs, deffStyleAttr) {
        View.inflate(context, R.layout.view_emoji_scroller, this)
        orientation = VERTICAL

        scrollView = findViewById(R.id.scrollView)

        val linear = findViewById<LinearLayout>(R.id.emojiContainer)
        linear.removeAllViewsInLayout()

        val params = LayoutParams(ViewUtils.dp2px(context, 40), ViewUtils.dp2px(context, 40))

        for (i in 0 until emojis.size) {
            val iv = AppCompatImageView(context)
            iv.id = emojis[i]
            iv.setImageResource(emojis[i])
            linear.addView(iv, params)
        }
    }

    fun touch(event: MotionEvent): Boolean {
        val res = scrollView.dispatchTouchEvent(event)
        invalidate()
        return res
    }

    companion object {
        private val emojis = arrayOf(
            R.drawable.ic_wink,
            R.drawable.ic_unhappy,
            R.drawable.ic_tongue_out,
            R.drawable.ic_suspicious,
            R.drawable.ic_suspicious_1,
            R.drawable.ic_surprised,
            R.drawable.ic_surprised_1,
            R.drawable.ic_smile,
            R.drawable.ic_smiling,
            R.drawable.ic_smart,
            R.drawable.ic_secret,
            R.drawable.ic_sad,
            R.drawable.ic_quiet,
            R.drawable.ic_ninja,
            R.drawable.ic_nerd,
            R.drawable.ic_mad,
            R.drawable.ic_kissing,
            R.drawable.ic_in_love,
            R.drawable.ic_ill,
            R.drawable.ic_happy,
            R.drawable.ic_happy_1,
            R.drawable.ic_happy_2,
            R.drawable.ic_happy_3,
            R.drawable.ic_happy_4,
            R.drawable.ic_embarrassed,
            R.drawable.ic_emoticons,
            R.drawable.ic_crying,
            R.drawable.ic_crying_1,
            R.drawable.ic_confused,
            R.drawable.ic_confused_1,
            R.drawable.ic_bored,
            R.drawable.ic_bored_1,
            R.drawable.ic_bored_2,
            R.drawable.ic_angry,
            R.drawable.ic_angry_1
        )
    }
}