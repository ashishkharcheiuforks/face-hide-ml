package com.github.naz013.facehide

import android.content.ClipData
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import com.github.naz013.facehide.databinding.ActivityMainBinding
import com.github.naz013.facehide.databinding.DialogEmojiListBinding
import com.github.naz013.facehide.utils.PhotoSelectionUtil
import com.google.android.material.bottomsheet.BottomSheetDialog
import timber.log.Timber

class MainActivity : AppCompatActivity(), PhotoSelectionUtil.UriCallback {

    private lateinit var photoSelectionUtil: PhotoSelectionUtil
    private lateinit var viewModel: RecognitionViewModel
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        binding.galleryButton.setOnClickListener { photoSelectionUtil.pickFromGallery() }
        binding.cameraButton.setOnClickListener { photoSelectionUtil.takePhoto() }
        binding.manipulationView.emojiPopupListener = { showEmojiPopup(it) }

        photoSelectionUtil = PhotoSelectionUtil(this, false, this)
        viewModel = ViewModelProviders.of(this).get(RecognitionViewModel::class.java)
        viewModel.foundFaces.observe(this, Observer {
            Timber.d("onCreate: faces $it")
            if (it != null) {
                binding.manipulationView.showFaces(it)
            }
        })
        viewModel.originalPhoto.observe(this, Observer {
            Timber.d("onCreate: photo $it")
            if (it != null) {
                binding.manipulationView.setPhoto(it)
            }
        })
    }

    private fun showEmojiPopup(face: Int) {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = DialogEmojiListBinding.inflate(layoutInflater)
        view.emojiList.layoutManager = GridLayoutManager(this, 5)
        val adapter = EmojiAdapter()
        adapter.clickListener = {
            binding.manipulationView.setEmojiToFace(face, it)
            bottomSheetDialog.dismiss()
        }
        adapter.setData(emojis.toList())
        view.emojiList.adapter = adapter
        bottomSheetDialog.setContentView(view.root)
        bottomSheetDialog.show()
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
    }
}
