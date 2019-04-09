package com.github.naz013.facehide

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.ViewModel
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import timber.log.Timber
import java.io.IOException

class RecognitionViewModel : ViewModel(), LifecycleObserver {

    // Real-time contour detection of multiple faces
    private val realTimeOpts = FirebaseVisionFaceDetectorOptions.Builder()
        .setPerformanceMode(FirebaseVisionFaceDetectorOptions.FAST)
        .setContourMode(FirebaseVisionFaceDetectorOptions.NO_CONTOURS)
        .build()

    private val detector = FirebaseVision.getInstance()
        .getVisionFaceDetector(realTimeOpts)

    fun detectFromBitmap(bitmap: Bitmap) {
        val image = FirebaseVisionImage.fromBitmap(bitmap)
        runDetection(image)
    }

    fun detectFromFile(context: Context, uri: Uri) {
        val image: FirebaseVisionImage
        try {
            image = FirebaseVisionImage.fromFilePath(context, uri)
            runDetection(image)
        } catch (e: IOException) {
            showError()
        }
    }

    private fun showError() {

    }

    private fun runDetection(image: FirebaseVisionImage) {
        Timber.d("runDetection: ")
        detector.detectInImage(image)
            .addOnSuccessListener { Timber.d("runDetection: success $it") }
            .addOnCanceledListener { Timber.d("runDetection: cancel ") }
            .addOnFailureListener { Timber.d("runDetection: failure $it") }
    }
}