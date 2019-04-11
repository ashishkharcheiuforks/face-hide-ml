package com.github.naz013.facehide.utils

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.*
import androidx.annotation.DrawableRes
import androidx.annotation.Px
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.github.naz013.facehide.R
import com.github.naz013.facehide.RecognitionViewModel
import timber.log.Timber
import java.util.*
import kotlin.math.min


class PhotoManipulationView : View {

    private val paint: Paint = Paint()
    private var mShadowPaint: Paint = Paint()
    private var mArrowPaint: Paint = Paint()
    private var mWidth: Int = 0
    private var mHeight: Int = 0
    private var mPopupHeight: Int = 56

    private var facePopup: FacePopup? = null
    private var photoPopup: PhotoPopup? = null
    private var photo: Photo? = null
    private val faces: MutableList<Face> = mutableListOf()
    private val masks: MutableList<Mask> = mutableListOf()

    private var mSelectedItem = -1
    private var isSlided = false
    private var isPhotoMenuVisible = false

    private var mX: Float = 0f
    private var mY: Float = 0f

    constructor(context: Context): this(context, null)
    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, deffStyleAttr: Int): super(context, attrs, deffStyleAttr) {
        mPopupHeight = dp2px(56)

        masks.clear()
        val rect = Rect(0, 0, mPopupHeight, mPopupHeight)
        val list = emojis.map {
            Mask(Point(), rect, toDrawable(it), paint)
        }.filter { it.bitmap != null }.toList()
        masks.addAll(list)

        mShadowPaint.isAntiAlias = true
        mShadowPaint.color = Color.WHITE
        mShadowPaint.setShadowLayer(dp2px(5).toFloat(), 0f, 0f, Color.parseColor("#40000000"))
        mShadowPaint.style = Paint.Style.FILL
        setLayerType(View.LAYER_TYPE_SOFTWARE, mShadowPaint)

        mArrowPaint.isAntiAlias = true
        mArrowPaint.color = Color.WHITE
        mArrowPaint.style = Paint.Style.FILL

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
            if (mSelectedItem != -1) {
                facePopup?.draw(canvas)
            } else if (isPhotoMenuVisible) {
                photoPopup?.draw(canvas)
            }
        }
    }

    fun hidePopups(): Boolean {
        if (isPhotoMenuVisible || mSelectedItem != -1) {
            Timber.d("hidePopups: ")
            isPhotoMenuVisible = false
            mSelectedItem = -1
            facePopup = null
            photoPopup = null
            this.invalidate()
            return true
        }
        return false
    }

    fun setPhoto(bitmap: Bitmap) {
        this.photo = null
        this.faces.clear()
        hidePopups()

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
                    if (mSelectedItem != -1 || isPhotoMenuVisible) {
                        hidePopups()
                        v.playSoundEffect(SoundEffectConstants.CLICK)
                    } else {
                        val index = findIndex(event.x, event.y)
                        val isPhoto = photo?.inBounds(event.x.toInt(), event.y.toInt()) ?: false
                        Timber.d("processTouch: $isPhoto, $event")
                        if (index != -1) {
                            if (index != mSelectedItem) {
                                Timber.d("processTouch: face clicked")
                                mSelectedItem = index
                                facePopup = FacePopup(faces[index])
                                v.playSoundEffect(SoundEffectConstants.CLICK)
                                this.invalidate()
                            }
                        } else if (isPhoto) {
                            Timber.d("processTouch: photo clicked")
                            isPhotoMenuVisible = true
                            photoPopup = PhotoPopup(findPhotoPlace(event.x, event.y))
                            v.playSoundEffect(SoundEffectConstants.CLICK)
                            this.invalidate()
                        } else {
                            hidePopups()
                        }
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

    private fun toDrawable(@DrawableRes res: Int): Bitmap? {
        if (res == 0) return null
        val drawable = ContextCompat.getDrawable(context, res)
        return drawable?.toBitmap()
    }

    private fun findPhotoPlace(x: Float, y: Float): PopupBg {
        val gravity = if (mHeight - y > mPopupHeight * 2) {
            Gravity.BOTTOM
        } else {
            Gravity.TOP
        }
        val half = mPopupHeight / 2
        val right = mWidth - half
        val point = PointF(x, y)
        val rect = if (gravity == Gravity.TOP) {
            val top = y.toInt() - half - mPopupHeight
            val bottom = top + mPopupHeight
            Rect(half, top, right, bottom)
        } else {
            val top = y.toInt() + half
            val bottom = top + mPopupHeight
            Rect(half, top, right, bottom)
        }
        return PopupBg(Arrow(gravity, half, point, mArrowPaint), rect.toRectF(), mShadowPaint, dp2px(5).toFloat())
    }

    private fun findPopupPlace(face: Face): PopupBg {
        val gravity = if (mHeight - face.rect.bottom > mPopupHeight * 2) {
            Gravity.BOTTOM
        } else {
            Gravity.TOP
        }
        val half = mPopupHeight / 2
        val right = mWidth - half
        val point: Point
        val rect = if (gravity == Gravity.TOP) {
            val top = face.rect.top - half - mPopupHeight
            val bottom = top + mPopupHeight
            point = Point(face.rect.centerX(), face.rect.top)
            Rect(half, top, right, bottom)
        } else {
            val top = face.rect.bottom + half
            val bottom = top + mPopupHeight
            point = Point(face.rect.centerX(), face.rect.bottom)
            Rect(half, top, right, bottom)
        }
        return PopupBg(Arrow(gravity, half, point.toPointF(), mArrowPaint), rect.toRectF(), mShadowPaint, dp2px(5).toFloat())
    }

    private fun Rect.toRectF(): RectF = RectF(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())

    private fun Point.toPointF(): PointF = PointF(x.toFloat(), y.toFloat())

    private fun toRect(rect: Rect, point: Point): Rect {
        return Rect(point.x, point.y, point.x + rect.width(), point.y + rect.height())
    }

    private fun toRectF(rect: Rect, point: PointF): RectF {
        return RectF(
            point.x, point.y,
            point.x + rect.width().toFloat(), point.y + rect.height().toFloat()
        )
    }

    inner class PhotoPopup(popupBg: PopupBg) : Popup(popupBg) {
        override fun draw(canvas: Canvas) {
            super.draw(canvas)
        }
    }

    inner class FacePopup(face: Face) : Popup(findPopupPlace(face)) {
        private val emojiArray = EmojiArray(face, masks, Point(popupBg.topLeft()))
        override fun draw(canvas: Canvas) {
            super.draw(canvas)
            emojiArray.draw(canvas)
        }
    }

    inner class EmojiArray(face: Face, val emojis: List<Mask>, point: Point) {
        init {
            for (i in 0 until emojis.size) {
                emojis[i].point = Point(point.x + (point.x * i), point.y)
            }
        }
        fun draw(canvas: Canvas) {
            for (e in emojis) {
                e.draw(canvas)
            }
        }
    }

    open inner class Popup(protected val popupBg: PopupBg) {
        open fun draw(canvas: Canvas) {
            popupBg.draw(canvas)
        }
    }

    inner class PopupBg(private val arrow: Arrow, private val rect: RectF, private val paint: Paint,
                        private val radius: Float = dp2px(5).toFloat()) {
        fun topLeft(): Point = Point(rect.left.toInt(), rect.top.toInt())
        fun draw(canvas: Canvas) {
            Timber.d("draw: $rect")
            canvas.drawRoundRect(rect, radius, radius, paint)
            arrow.draw(canvas)
        }
    }

    inner class Arrow(private val gravity: Gravity, private val side: Int, private val point: PointF, private val paint: Paint) {

        private val halfSide = side / 2f

        fun draw(canvas: Canvas) {
            Timber.d("draw: $gravity, $side, $point")
            val path = Path()
            when (gravity) {
                Gravity.TOP -> {
                    path.moveTo(point.x + halfSide, point.y - side)
                    path.lineTo(point.x - halfSide, point.y - side)
                    path.lineTo(point.x, point.y - halfSide)
                }
                Gravity.BOTTOM -> {
                    path.moveTo(point.x - halfSide, point.y + side)
                    path.lineTo(point.x + halfSide, point.y + side)
                    path.lineTo(point.x, point.y + halfSide)
                }
                Gravity.RIGHT -> {

                }
                Gravity.LEFT -> {

                }
            }
            path.close()
            canvas.drawPath(path, paint)
//            canvas.drawCircle(point.x, point.y, dp2px(10).toFloat(), paint)
        }
    }

    inner class Face(val rect: Rect, var mask: Mask? = null,
                     private val color: Int = colors[Random().nextInt(colors.size)]) {
        fun draw(canvas: Canvas) {
            Timber.d("draw: $rect")
            paint.color = color
            canvas.drawRect(rect, paint)
            mask?.paint = paint
            mask?.rect = rect
            mask?.point = Point(0, 0)
            mask?.draw(canvas)
        }
    }

    inner class Mask(var point: Point, var rect: Rect, val bitmap: Bitmap?, var paint: Paint) {
        private val padding = dp2px(4)
        fun draw(canvas: Canvas) {
            Timber.d("draw: $rect")
            if (bitmap != null) {
                val r = toRect(rect, point)
                r.left = r.left + padding
                r.top = r.top + padding
                r.right = r.right - padding
                r.bottom = r.bottom - padding
                canvas.drawBitmap(bitmap, null, r, paint)
            }
        }
    }

    inner class Photo(val bitmap: Bitmap, val point: Point) {
        private var bounds: Rect = Rect(point.x, point.y, point.x + bitmap.width, point.y + bitmap.height)
        fun inBounds(x: Int, y: Int): Boolean = bounds.contains(x, y)
        private fun x(): Float = point.x.toFloat()
        private fun y(): Float = point.y.toFloat()
        fun draw(canvas: Canvas) {
            Timber.d("draw: $point, $bounds")
            canvas.drawBitmap(bitmap, x(), y(), paint)
        }
    }

    enum class Gravity {
        TOP,
        BOTTOM,
        RIGHT,
        LEFT
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
        private val rawColors = arrayOf("#F44336", "#E91E63", "#9C27B0", "#673AB7",
            "#3F51B5", "#2196F3", "#03A9F4", "#00BCD4",
            "#009688", "#4CAF50", "#8BC34A", "#CDDC39",
            "#FFEB3B", "#FFC107", "#FF9800", "#FF5722",
            "#795548", "#9E9E9E", "#607D8B", "#FFFFFF"
        )

        private val colors = rawColors.map{ Color.parseColor(it) }
    }
}