package com.github.naz013.facehide.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Rect
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.github.naz013.facehide.utils.launchDefault
import com.github.naz013.facehide.utils.withUIContext
import com.github.naz013.facehide.views.PhotoManipulationView
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.min


class RecognitionViewModel : ViewModel(), LifecycleObserver {

    private val realTimeOpts = FirebaseVisionFaceDetectorOptions.Builder()
        .setPerformanceMode(FirebaseVisionFaceDetectorOptions.FAST)
        .setContourMode(FirebaseVisionFaceDetectorOptions.NO_CONTOURS)
        .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
        .build()

    private val detector = FirebaseVision.getInstance().getVisionFaceDetector(realTimeOpts)

    private val _originalPhoto: MutableLiveData<Bitmap> = MutableLiveData()
    val originalPhoto: LiveData<Bitmap> = _originalPhoto

    private val _foundFaces: MutableLiveData<ScanResult> = MutableLiveData()
    val foundFaces: LiveData<ScanResult> = _foundFaces

    private val _isSaved: MutableLiveData<String> = MutableLiveData()
    val isSaved: LiveData<String> = _isSaved

    private val _isLoading: MutableLiveData<Boolean> = MutableLiveData()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error: MutableLiveData<Int> = MutableLiveData()
    val error: LiveData<Int> = _error

    private var size: Size? = null

    fun clear() {
        size = null
        _originalPhoto.postValue(null)
        _foundFaces.postValue(null)
    }

    fun savePhoto(fileName: String, results: PhotoManipulationView.Results) {
        val original = originalPhoto.value
        if (original == null) {
            _error.postValue(NO_IMAGE)
            return
        }
        _isLoading.postValue(true)
        launchDefault {
            val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val appPath = File(path, "FaceHide")
            if (!appPath.exists()) {
                appPath.mkdirs()
            }
            val file = File(appPath, "$fileName.jpg")
            if (file.exists()) file.delete()

            val size = results.size
            val widthFactor = original.width.toFloat() / size.width.toFloat()
            val heightFactor = original.height.toFloat() / size.height.toFloat()
            val factor = (widthFactor + heightFactor) / 2f

            Timber.d("savePhoto: $factor, $file")

            val newFaces = results.faces.map {
                val rect = it.rect
                val left = (rect.left.toFloat() * factor).toInt()
                val top = (rect.top.toFloat() * factor).toInt()
                val right = (rect.right.toFloat() * factor).toInt()
                val bottom = (rect.bottom.toFloat() * factor).toInt()
                it.rect = Rect(left, top, right, bottom)
                it
            }

            val mutableBitmap = original.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(mutableBitmap)
            newFaces.forEach { it.draw(canvas) }
            var fos: FileOutputStream? = null
            try {
                fos = FileOutputStream(file)
                mutableBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                fos.flush()
                fos.close()
                fos = null
            } catch (e: IOException) {
                e.printStackTrace()
                withUIContext {
                    _isLoading.postValue(false)
                    _error.postValue(NO_SPACE)
                }
            } finally {
                if (fos != null) {
                    try {
                        fos.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
            withUIContext {
                _isLoading.postValue(false)
                _isSaved.postValue(file.toString())
            }
        }
    }

    fun detectFromBitmap(bitmap: Bitmap) {
        size = null
        _isLoading.postValue(true)
        launchDefault {
            _originalPhoto.postValue(bitmap)
            val scaled = scaledBitmap(bitmap, 1024)
            size = Size(scaled.width, scaled.height)
            val image = FirebaseVisionImage.fromBitmap(scaled)
            withUIContext {
                runDetection(image)
            }
        }
    }

    fun detectFromFile(context: Context, uri: Uri) {
        _isLoading.postValue(true)
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
                _isLoading.postValue(false)
                _error.postValue(NO_IMAGE)
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

    private fun runDetection(image: FirebaseVisionImage) {
        Timber.d("runDetection: ")
        detector.detectInImage(image)
            .addOnSuccessListener {
                Timber.d("runDetection: success $it")
                _isLoading.postValue(false)
                if (it.isEmpty()) {
                    _error.postValue(NO_FACES)
                } else {
                    size?.let { s ->
                        _foundFaces.postValue(ScanResult(s, it))
                    }
                }
            }
            .addOnCanceledListener {
                _isLoading.postValue(false)
            }
            .addOnFailureListener {
                _isLoading.postValue(false)
                _error.postValue(NO_FACES)
            }
    }

    data class ScanResult(val size: Size, val list: List<FirebaseVisionFace>)

    companion object Error {
        const val NO_IMAGE = 0
        const val NO_SD = 1
        const val NO_SPACE = 2
        const val NO_FACES = 3
    }
}