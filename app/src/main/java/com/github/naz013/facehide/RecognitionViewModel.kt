package com.github.naz013.facehide

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.ViewModel
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import java.io.IOException

class RecognitionViewModel : ViewModel(), LifecycleObserver {

    // High-accuracy landmark detection and face classification
    private val highAccuracyOpts = FirebaseVisionFaceDetectorOptions.Builder()
        .setPerformanceMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
        .setLandmarkMode(FirebaseVisionFaceDetectorOptions.NO_LANDMARKS)
        .setClassificationMode(FirebaseVisionFaceDetectorOptions.NO_CLASSIFICATIONS)
        .build()

    // Real-time contour detection of multiple faces
    private val realTimeOpts = FirebaseVisionFaceDetectorOptions.Builder()
        .setContourMode(FirebaseVisionFaceDetectorOptions.ALL_CONTOURS)
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
        detector.detectInImage(image)
            .addOnSuccessListener {

            }
            .addOnFailureListener { showError() }
    }
}