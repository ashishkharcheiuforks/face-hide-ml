package com.github.naz013.facehide.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SoundEffectConstants
import android.view.View
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import com.github.naz013.facehide.R
import com.github.naz013.facehide.data.RecognitionViewModel
import com.github.naz013.facehide.data.Size
import com.github.naz013.facehide.utils.Prefs
import com.github.naz013.facehide.utils.ViewUtils
import timber.log.Timber
import java.util.*
import kotlin.math.max
import kotlin.math.min


class PhotoManipulationView : View {

    private val paint: Paint = Paint()
    private val mShadowPaint: Paint = Paint()
    private val mArrowPaint: Paint = Paint()

    private val faces: MutableList<Face> = mutableListOf()
    private val random = Random()

    private var mWidth: Int = 0
    private var mHeight: Int = 0
    private var mPopupHeight: Int = 56

    private var photo: Photo? = null
    private var isSlided = false

    private var mX: Float = 0f
    private var mY: Float = 0f

    var emojiPopupListener: ((Int, Boolean) -> Unit)? = null

    constructor(context: Context): this(context, null)
    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, deffStyleAttr: Int): super(context, attrs, deffStyleAttr) {
        mPopupHeight = ViewUtils.dp2px(context, 56)

        mShadowPaint.isAntiAlias = true
        mShadowPaint.color = Color.WHITE
        mShadowPaint.setShadowLayer(ViewUtils.dp2px(context, 5).toFloat(), 0f, 0f, Color.parseColor("#40000000"))
        mShadowPaint.style = Paint.Style.FILL
        setLayerType(LAYER_TYPE_SOFTWARE, mShadowPaint)

        mArrowPaint.isAntiAlias = true
        mArrowPaint.color = Color.WHITE
        mArrowPaint.style = Paint.Style.FILL

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = ViewUtils.dp2px(context, 2).toFloat()

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

    fun clear() {
        this.photo = null
        this.faces.clear()
    }

    fun setPhoto(bitmap: Bitmap) {
        clear()

        val resizedPhoto = scaledBitmap(bitmap, mWidth, mHeight)
        if (resizedPhoto != null) {
            this.photo = Photo(resizedPhoto, selectPoint(resizedPhoto, mWidth, mHeight))
        }

        this.invalidate()
    }

    fun prepareResults(): Results? {
        val currentPhoto = photo ?: return null
        val currentBitmap = currentPhoto.bitmap
        val size = Size(currentBitmap.width, currentBitmap.height)
        val masks = mutableListOf<Mask>()
        faces.forEach {
            val mask = it.mask
            if(mask != null) {
                masks.add(mask)
            }
        }
        masks.forEach {
            val rect = it.rect
            val point = currentPhoto.point
            val left = rect.left - point.x
            val top = rect.top - point.y
            val right = rect.right - point.x
            val bottom = rect.bottom - point.y
            it.rect = Rect(left, top, right, bottom)
        }
        return Results(size, masks)
    }

    fun showFaces(scanResult: RecognitionViewModel.ScanResult) {
        val autoDetect = Prefs.getInstance(context).isAutoFace()
        val currentPhoto = photo ?: return
        val currentBitmap = currentPhoto.bitmap
        val widthFactor = currentBitmap.width.toFloat() / scanResult.size.width.toFloat()
        val heightFactor = currentBitmap.height.toFloat() / scanResult.size.height.toFloat()
        val factor = (widthFactor + heightFactor) / 2f

        Timber.d("showFaces: $factor")

        val newFaces = scanResult.list.map {
            val rect = it.boundingBox
            val point = currentPhoto.point
            val left = (rect.left.toFloat() * factor).toInt() + point.x
            val top = (rect.top.toFloat() * factor).toInt() + point.y
            val right = (rect.right.toFloat() * factor).toInt() + point.x
            val bottom = (rect.bottom.toFloat() * factor).toInt() + point.y
            if (autoDetect) {
                val bmp = toDrawable(getAutoEmoji(it.smilingProbability))
                if (bmp != null) {
                    Face(Rect(left, top, right, bottom), Mask(bmp))
                } else {
                    Face(Rect(left, top, right, bottom))
                }
            } else {
                Face(Rect(left, top, right, bottom))
            }
        }
        this.faces.clear()
        this.faces.addAll(newFaces)
        this.invalidate()
    }

    fun setEmojiToFace(faceId: Int, emojiId: Int) {
        if (emojiId == 0) {
            faces[faceId].mask = null
        } else {
            val bmp = toDrawable(emojiId)
            if (bmp != null) {
                faces[faceId].mask = Mask(bmp)
            }
        }
        this.invalidate()
    }

    fun hasPhoto(): Boolean {
        return photo != null
    }

    private fun getAutoEmoji(smilingProbability: Float): Int {
        if (smilingProbability == -1f) return R.drawable.ic_confused
        return when {
            smilingProbability >= 0.7f -> R.drawable.ic_smiling
            smilingProbability >= 0.4f -> R.drawable.ic_confused
            else -> R.drawable.ic_sad
        }
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
        mHeight = MeasureSpec.getSize(heightMeasureSpec)
        mWidth = MeasureSpec.getSize(widthMeasureSpec)
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
                    val isPhoto = photo?.inBounds(event.x.toInt(), event.y.toInt()) ?: false
                    Timber.d("processTouch: $isPhoto, $event")
                    if (index != -1) {
                        emojiPopupListener?.invoke(index, faces[index].mask != null)
                        v.playSoundEffect(SoundEffectConstants.CLICK)
                    } else if (isPhoto) {
                        Timber.d("processTouch: photo clicked")
//                        v.playSoundEffect(SoundEffectConstants.CLICK)
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
        return AppCompatResources.getDrawable(context, res)?.toBitmap()
    }

    inner class Face(val rect: Rect, var mask: Mask? = null) {
        private val shader = LinearGradient(0f, 0f, 0f, rect.width().toFloat(), colors[random.nextInt(colors.size)],
            colors[random.nextInt(colors.size)], Shader.TileMode.MIRROR)
        fun draw(canvas: Canvas) {
            Timber.d("draw: $rect")
            paint.shader = shader
            canvas.drawRect(rect, paint)
            mask?.paint = paint
            mask?.rect = rect
            mask?.draw(canvas)
        }
    }

    inner class Mask(private val bitmap: Bitmap, var paint: Paint = Paint()) {
        var rect: Rect = Rect()
            set(value) {
                field = value
                if (min(value.width(), value.height()) * 2 < max(value.width(), value.height())) {
                    srcRect = if (value.width() > value.height()) {
                        if (value.centerY() > mHeight / 2) {
                            //bottom
                            Rect(0, 0, bitmap.width, value.height())
                        } else {
                            //top
                            Rect(0, bitmap.height - value.height(), bitmap.width, bitmap.height)
                        }
                    } else {
                        if (value.centerX() > mWidth / 2) {
                            //right
                            Rect(0, 0, value.width(), bitmap.height)
                        } else {
                            //left
                            Rect(bitmap.width - value.width(), 0, bitmap.width, bitmap.height)
                        }
                    }

                } else {
                    srcRect = null
                }
            }
        var srcRect: Rect? = Rect()

        fun draw(canvas: Canvas) {
            Timber.d("draw: $rect")
            val dstRect = Rect(rect)
            canvas.drawBitmap(bitmap, srcRect, dstRect, paint)
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

    inner class Results(val size: Size, val faces: List<Mask>)

    companion object {

        private val rawColors = arrayOf(
            "#7E1037", "#C14E76", "#DF7DA6", "#F7B2CF", "#FFE4E1",
            "#35BBCA", "#0191B4", "#F8D90F", "#D3DD18", "#FE7A15",
            "#47CACC", "#63BCC9", "#CDB3D4", "#E7B7C8", "#FFBE88",
            "#FC6B05", "#FFB62B", "#65B017", "#99D8DB", "#9BB7BB",
            "#D6A3DC", "#F7DB70", "#EABEBF", "#75CCE8", "#A5DEE5",
            "#60EFDB", "#BEF2E5", "#C5E7F1", "#79CEED", "#6F89A2"
        )

        private val colors = rawColors.map{ Color.parseColor(it) }
    }
}