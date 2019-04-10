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

    private val paint: Paint = Paint()
    private var mWidth: Int = 0
    private var mHeight: Int = 0

    private var photo: Photo? = null
    private val faces: MutableList<Face> = mutableListOf()

    private var mSelectedItem = -1
    private var isSlided = false
    private var isPhotoMenuVisible = false

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
            photo?.draw(canvas)
            for (f in faces) f.draw(canvas)
        }
    }

    fun hidePopups(): Boolean {
        if (isPhotoMenuVisible || mSelectedItem != -1) {
            Timber.d("hidePopups: ")
            isPhotoMenuVisible = false
            mSelectedItem = -1
            this.invalidate()
            return true
        }
        return false
    }

    fun setPhoto(bitmap: Bitmap) {
        this.photo = null
        this.faces.clear()

        val resizedPhoto = scaledBitmap(bitmap, mWidth, mHeight)
        if (resizedPhoto != null) {
            this.photo = Photo(resizedPhoto, selectPoint(resizedPhoto, mWidth, mHeight))
        }

        this.invalidate()
    }

    fun showFaces(scanResult: RecognitionViewModel.ScanResult) {
        val currentPhoto = photo ?: return
        val currentBitmap = currentPhoto.bitmap
        val widthFactor = currentBitmap.width.toFloat() / scanResult.bmp.width.toFloat()
        val heightFactor = currentBitmap.height.toFloat() / scanResult.bmp.height.toFloat()
        val factor = (widthFactor + heightFactor) / 2f

        Timber.d("showFaces: $factor")

        val newFaces = scanResult.list.map {
            val rect = it.boundingBox
            val point = currentPhoto.point
            val left = (rect.left.toFloat() * factor).toInt() + point.x
            val top = (rect.top.toFloat() * factor).toInt() + point.y
            val right = (rect.right.toFloat() * factor).toInt() + point.x
            val bottom = (rect.bottom.toFloat() * factor).toInt() + point.y
            Face(Rect(left, top, right, bottom))
        }
        this.faces.clear()
        this.faces.addAll(newFaces)
        this.invalidate()
    }

    private fun selectPoint(bitmap: Bitmap, width: Int, height: Int): Point {
        if (bitmap.width == width && bitmap.height == height) return Point(0, 0)
        var wMargin = (width - bitmap.width) / 2
        if (wMargin <= 0) wMargin = 0

        var hMargin = (height - bitmap.height) / 2
        if (hMargin <= 0) hMargin = 0

        return Point(wMargin, hMargin)
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
                    val index = findIndex(event.x, event.y)
                    val isPhoto = photo?.inBounds(event.rawX.toInt(), event.rawY.toInt()) ?: false
                    Timber.d("processTouch: $isPhoto, $event")
                    if (index != -1) {
                        if (index != mSelectedItem) {
                            Timber.d("processTouch: face clicked")
                            mSelectedItem = index
                            v.playSoundEffect(SoundEffectConstants.CLICK)
                            this.invalidate()
                        }
                    } else if (isPhoto) {
                        Timber.d("processTouch: photo clicked")
                        isPhotoMenuVisible = true
                        v.playSoundEffect(SoundEffectConstants.CLICK)
                        this.invalidate()
                    } else {
                        hidePopups()
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
        return -1
    }

    inner class Photo(val bitmap: Bitmap, val point: Point) {
        var bounds: Rect = Rect(point.x, point.y, point.x + bitmap.width, point.y + bitmap.height)

        fun inBounds(x: Int, y: Int): Boolean = bounds.contains(x, y)
        private fun x(): Float = point.x.toFloat()
        private fun y(): Float = point.y.toFloat()

        fun draw(canvas: Canvas) {
            Timber.d("draw: $point, $bounds")
            canvas.drawBitmap(bitmap, x(), y(), paint)
        }
    }

    inner class Face(val rect: Rect, var mask: Bitmap? = null,
                     private val color: Int = colors[Random().nextInt(colors.size)]) {


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