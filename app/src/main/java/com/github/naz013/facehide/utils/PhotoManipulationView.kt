package com.github.naz013.facehide.utils

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.*
import androidx.annotation.Px
import com.github.naz013.facehide.RecognitionViewModel
import timber.log.Timber
import java.util.*
import kotlin.math.min

class PhotoManipulationView : View {

    private var resizedPhoto: Bitmap? = null

    private val paint: Paint = Paint()
    private var mWidth: Int = 0
    private var mHeight: Int = 0

    private val faces: MutableList<Face> = mutableListOf()
    private var mSelectedItem = 0
    private var isSlided = false

    private var mX: Float = 0f
    private var mY: Float = 0f

    constructor(context: Context): this(context, null)
    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, deffStyleAttr: Int): super(context, attrs, deffStyleAttr) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = dp2px(2).toFloat()

        setOnTouchListener { _, _ -> false }
    }

    override fun onDraw(canvas: Canvas?) {
        if (canvas == null) {
            super.onDraw(canvas)
        } else {
            val bmp = resizedPhoto ?: return
            canvas.drawBitmap(bmp, 0f, 0f, paint)
            for (f in faces) f.draw(canvas)
        }
    }

    fun setPhoto(bitmap: Bitmap) {
        this.resizedPhoto = scaledBitmap(bitmap, mWidth, mHeight)
        this.faces.clear()
        this.invalidate()
    }

    fun showFaces(scanResult: RecognitionViewModel.ScanResult) {
        val currentPhoto = resizedPhoto ?: return
        val widthFactor = currentPhoto.width.toFloat() / scanResult.bmp.width.toFloat()
        val heightFactor = currentPhoto.height.toFloat() / scanResult.bmp.height.toFloat()
        val factor = (widthFactor + heightFactor) / 2f

        Timber.d("showFaces: $factor")

        val newFaces = scanResult.list.map {
            val rect = it.boundingBox
            val left = (rect.left.toFloat() * factor).toInt()
            val top = (rect.top.toFloat() * factor).toInt()
            val right = (rect.right.toFloat() * factor).toInt()
            val bottom = (rect.bottom.toFloat() * factor).toInt()
            Face(Rect(left, top, right, bottom))
        }
        this.faces.clear()
        this.faces.addAll(newFaces)
        this.invalidate()
    }

    private fun scaledBitmap(bitmap: Bitmap, width: Int, height: Int): Bitmap? {
        if (bitmap.width <= 0 || bitmap.height <= 0) return null
        if (width <= 0 || height <= 0) return null

        Timber.d("scaledBitmap: req w -> $width, req h -> $height")

        val widthFactor = width.toFloat() / bitmap.width.toFloat()
        val heightFactor = height.toFloat() / bitmap.height.toFloat()
        val scaleFactor = min(widthFactor, heightFactor)

        Timber.d("scaledBitmap: factor -> $scaleFactor")

        val scaleWidth = (bitmap.width.toFloat() * scaleFactor).toInt()
        val scaleHeight = (bitmap.height.toFloat() * scaleFactor).toInt()

        Timber.d("scaledBitmap: nw -> $scaleWidth, nh -> $scaleHeight")

        return Bitmap.createScaledBitmap(bitmap, scaleWidth, scaleHeight, false)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        mHeight = View.MeasureSpec.getSize(heightMeasureSpec)
        mWidth = View.MeasureSpec.getSize(widthMeasureSpec)
    }

    @Px
    private fun dp2px(dp: Int): Int {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager?
        var display: Display? = null
        if (wm != null) display = wm.defaultDisplay
        val displayMetrics = DisplayMetrics()
        display?.getMetrics(displayMetrics)
        return (dp * displayMetrics.density + 0.5f).toInt()
    }

    override fun setOnLongClickListener(l: OnLongClickListener?) {
    }

    override fun setOnClickListener(l: OnClickListener?) {
    }

    override fun setOnTouchListener(l: OnTouchListener?) {
        super.setOnTouchListener { v, event -> processTouch(v, event) }
    }

    private fun processTouch(v: View, event: MotionEvent): Boolean {
        when {
            event.action == MotionEvent.ACTION_DOWN -> {
                mX = event.x
                mY = event.y
                isSlided = false
                return true
            }
            event.action == MotionEvent.ACTION_MOVE -> {
                if (Math.abs(event.x - mX) > 30 || Math.abs(event.y - mY) > 30) {
                    isSlided = true
                    return false
                }
                return true
            }
            event.action == MotionEvent.ACTION_UP -> {
                if (!isSlided) {
                    val item = findIndex(event.x, event.y)
                    if (item != mSelectedItem) {
                        mSelectedItem = item

                        v.playSoundEffect(SoundEffectConstants.CLICK)
                    }
                }
                isSlided = false
                return v.performClick()
            }
            else -> return false
        }
    }

    private fun findIndex(x: Float, y: Float): Int {
        for (i in 0 until faces.size) {
            if (faces[i].rect.contains(x.toInt(), y.toInt())) {
                return i
            }
        }
        return 0
    }

    inner class Face(val rect: Rect, var mask: Bitmap? = null, private val color: Int = colors[Random().nextInt(colors.size)]) {
        fun draw(canvas: Canvas) {
            Timber.d("draw: $rect")
            paint.color = color
            canvas.drawRect(rect, paint)
        }
    }

    companion object {
        private val rawColors = arrayOf("#F44336", "#E91E63", "#9C27B0", "#673AB7",
            "#3F51B5", "#2196F3", "#03A9F4", "#00BCD4",
            "#009688", "#4CAF50", "#8BC34A", "#CDDC39",
            "#FFEB3B", "#FFC107", "#FF9800", "#FF5722",
            "#795548", "#9E9E9E", "#607D8B", "#FFFFFF"
        )

        private val colors = rawColors.map{ Color.parseColor(it) }
    }
}