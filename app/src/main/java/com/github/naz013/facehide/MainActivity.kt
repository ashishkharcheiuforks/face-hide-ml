package com.github.naz013.facehide

import android.content.ClipData
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.ViewModelProviders
import com.github.naz013.facehide.utils.PhotoSelectionUtil
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity(), PhotoSelectionUtil.UriCallback {

    private lateinit var photoSelectionUtil: PhotoSelectionUtil
    private lateinit var viewModel: RecognitionViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<MaterialButton>(R.id.selectPhotoButton).setOnClickListener { photoSelectionUtil.selectImage() }
        findViewById<MaterialButton>(R.id.saveButton).setOnClickListener { saveChanges() }

        photoSelectionUtil = PhotoSelectionUtil(this, false, this)
        viewModel = ViewModelProviders.of(this).get(RecognitionViewModel::class.java)
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
