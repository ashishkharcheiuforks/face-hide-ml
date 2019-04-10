package com.github.naz013.facehide

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.github.naz013.facehide.utils.launchDefault
import com.github.naz013.facehide.utils.withUIContext
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import timber.log.Timber
import java.io.IOException
import kotlin.math.min


class RecognitionViewModel : ViewModel(), LifecycleObserver {

    private val realTimeOpts = FirebaseVisionFaceDetectorOptions.Builder()
        .setPerformanceMode(FirebaseVisionFaceDetectorOptions.FAST)
        .setContourMode(FirebaseVisionFaceDetectorOptions.NO_CONTOURS)
        .build()

    private val detector = FirebaseVision.getInstance().getVisionFaceDetector(realTimeOpts)

    private val _originalPhoto: MutableLiveData<Bitmap> = MutableLiveData()
    val originalPhoto: LiveData<Bitmap> = _originalPhoto

    private val _foundFaces: MutableLiveData<ScanResult> = MutableLiveData()
    val foundFaces: LiveData<ScanResult> = _foundFaces

    private var scaledPhoto: Bitmap? = null

    fun detectFromBitmap(bitmap: Bitmap) {
        scaledPhoto = null
        launchDefault {
            _originalPhoto.postValue(bitmap)
            val scaled = scaledBitmap(bitmap, 1024)
            scaledPhoto = scaled
            val image = FirebaseVisionImage.fromBitmap(scaled)
            withUIContext {
                runDetection(image)
            }
        }
    }

    fun detectFromFile(context: Context, uri: Uri) {
        launchDefault {
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                val stream = context.contentResolver.openInputStream(uri)
                val bmp = stream.let {
                    if (it != null) {
                        val ei = ExifInterface(it)
                        val orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
                        Timber.d("detectFromFile: $orientation")
                        when (orientation) {
                            ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(bitmap, 90f)
                            ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(bitmap, 180f)
                            ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(bitmap, 270f)
                            ExifInterface.ORIENTATION_NORMAL -> bitmap
                            else -> bitmap
                        }
                    } else bitmap
                }
                detectFromBitmap(bmp)
            } catch (e: IOException) {
                showError()
            }
        }
    }

    private fun rotateImage(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun scaledBitmap(bitmap: Bitmap, maxSize: Int = 512): Bitmap {
        if (bitmap.width <= maxSize || bitmap.height <= maxSize) return bitmap
        val width = bitmap.width
        val height = bitmap.height

        Timber.d("scaledBitmap: w -> $width, h -> $height")

        val min = min(width, height)
        val scaleFactor = maxSize.toFloat() / min.toFloat()

        Timber.d("scaledBitmap: $min, factor -> $scaleFactor")

        val scaleWidth = (width.toFloat() * scaleFactor).toInt()
        val scaleHeight = (height.toFloat() * scaleFactor).toInt()

        Timber.d("scaledBitmap: nw -> $scaleWidth, nh -> $scaleHeight")

        return Bitmap.createScaledBitmap(bitmap, scaleWidth, scaleHeight, false)
    }

    private fun showError() {

    }

    private fun runDetection(image: FirebaseVisionImage) {
        Timber.d("runDetection: ")
        detector.detectInImage(image)
            .addOnSuccessListener {
                Timber.d("runDetection: success $it")
                scaledPhoto?.let { photo ->
                    _foundFaces.postValue(ScanResult(photo, it))
                }
            }
            .addOnCanceledListener { Timber.d("runDetection: cancel ") }
            .addOnFailureListener { Timber.d("runDetection: failure $it") }
    }

    data class ScanResult(val bmp: Bitmap, val list: List<FirebaseVisionFace>)
}