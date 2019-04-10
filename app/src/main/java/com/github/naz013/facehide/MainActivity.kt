package com.github.naz013.facehide

import android.content.ClipData
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.github.naz013.facehide.utils.PhotoManipulationView
import com.github.naz013.facehide.utils.PhotoSelectionUtil
import com.google.android.material.button.MaterialButton
import timber.log.Timber

class MainActivity : AppCompatActivity(), PhotoSelectionUtil.UriCallback {

    private lateinit var photoSelectionUtil: PhotoSelectionUtil
    private lateinit var viewModel: RecognitionViewModel
    private lateinit var photoManipulationView: PhotoManipulationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        photoManipulationView = findViewById(R.id.manipulationView)

        findViewById<MaterialButton>(R.id.selectPhotoButton).setOnClickListener { photoSelectionUtil.selectImage() }
        findViewById<MaterialButton>(R.id.saveButton).setOnClickListener { saveChanges() }

        photoSelectionUtil = PhotoSelectionUtil(this, false, this)
        viewModel = ViewModelProviders.of(this).get(RecognitionViewModel::class.java)
        viewModel.foundFaces.observe(this, Observer {
            Timber.d("onCreate: faces $it")
            if (it != null) {
                photoManipulationView.showFaces(it)
            }
        })
        viewModel.originalPhoto.observe(this, Observer {
            Timber.d("onCreate: photo $it")
            if (it != null) {
                photoManipulationView.setPhoto(it)
            }
        })
    }

    private fun saveChanges() {

    }

    override fun onImageSelected(uri: Uri?, clipData: ClipData?) {
        if (uri != null) {
            viewModel.detectFromFile(this, uri)
        }
    }

    override fun onBitmapReady(bitmap: Bitmap) {
        viewModel.detectFromBitmap(bitmap)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        photoSelectionUtil.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        photoSelectionUtil.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}
