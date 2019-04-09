package com.github.naz013.facehide

import android.content.ClipData
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.ViewModelProviders
import com.github.naz013.facehide.utils.PhotoSelectionUtil

class MainActivity : AppCompatActivity(), PhotoSelectionUtil.UriCallback {

    private lateinit var photoSelectionUtil: PhotoSelectionUtil
    private lateinit var viewModel: RecognitionViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        photoSelectionUtil = PhotoSelectionUtil(this, false, this)
        viewModel = ViewModelProviders.of(this).get(RecognitionViewModel::class.java)
    }

    override fun onImageSelected(uri: Uri?, clipData: ClipData?) {
    }

    override fun onBitmapReady(bitmap: Bitmap) {
    }
}
