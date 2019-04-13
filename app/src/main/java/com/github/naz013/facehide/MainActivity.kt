package com.github.naz013.facehide

import android.content.ClipData
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import com.github.naz013.facehide.databinding.ActivityMainBinding
import com.github.naz013.facehide.databinding.DialogEmojiListBinding
import com.github.naz013.facehide.databinding.DialogSavePhotoBinding
import com.github.naz013.facehide.utils.Permissions
import com.github.naz013.facehide.utils.PhotoSelectionUtil
import com.github.naz013.facehide.utils.UriUtil
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.jetbrains.anko.toast
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
        binding.moreButton.setOnClickListener { showMorePopup(it) }
        binding.manipulationView.emojiPopupListener = { showEmojiPopup(it) }
        binding.loadingView.setOnClickListener { }

        photoSelectionUtil = PhotoSelectionUtil(this, false, this)
        initViewModel()
    }

    private fun showMorePopup(view: View) {
        val items = if (binding.manipulationView.hasPhoto()) {
            arrayOf(getString(R.string.save_photo), getString(R.string.settings))
        } else {
            arrayOf(getString(R.string.settings))
        }
        showPopup(view, {
            when (it) {
                0 -> saveChanges()
                1 -> openSettings()
            }
        }, *items)
    }

    private fun openSettings() {

    }

    private fun initViewModel() {
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
        viewModel.error.observe(this, Observer {
            if (it != null) {
                showError(it)
            }
        })
        viewModel.isSaved.observe(this, Observer {
            if (it != null) showSuccess(it)
        })
        viewModel.isLoading.observe(this, Observer {
            if (it != null) {
                if (it) binding.loadingView.visibility = View.VISIBLE
                else binding.loadingView.visibility = View.GONE
            }
        })
    }

    private fun showSuccess(filePath: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.photo_saved))
        builder.setMessage(filePath)
        builder.setPositiveButton(getString(R.string.view)) { dialog, _ ->
            dialog.dismiss()
            showPhoto(filePath)
        }
        builder.setNeutralButton(getString(R.string.cancel)) { dialog, _ ->
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.WHITE)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.WHITE)
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(Color.WHITE)
        }
        dialog.show()
    }

    private fun showPhoto(filePath: String) {
        val intent = Intent()
        intent.action = Intent.ACTION_VIEW
        val photoURI = UriUtil.getUri(this, filePath)
        intent.setDataAndType(photoURI, "image/*")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            startActivity(Intent.createChooser(intent, getString(R.string.view_using)))
        } catch (e: Exception) {
            toast(getString(R.string.image_view_application_not_found))
        }
    }

    private fun showError(e: Int) {
        Timber.d("showError: $e")
        when (e) {
            RecognitionViewModel.NO_IMAGE -> toast(getString(R.string.no_photo))
            RecognitionViewModel.NO_SD -> toast(getString(R.string.no_sd_card))
            RecognitionViewModel.NO_SPACE -> toast(getString(R.string.no_enough_space))
            RecognitionViewModel.NO_FACES -> toast(getString(R.string.failed_to_find_faces_on_photo))
        }
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
        if (!checkSdPermission(REQ_SD)) return
        showFieldDialog()
    }

    private fun showFieldDialog() {
        val builder = AlertDialog.Builder(this)
        val view = DialogSavePhotoBinding.inflate(layoutInflater)
        builder.setTitle(getString(R.string.save_photo))
        builder.setView(view.root)
        builder.setPositiveButton(getString(R.string.save)) { dialog, _ ->
            dialog.dismiss()
            savePhoto(view.fileNameField.text.toString().trim())
        }
        builder.setNeutralButton(getString(R.string.cancel)) { dialog, _ ->
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.WHITE)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.WHITE)
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(Color.WHITE)
        }
        dialog.show()
    }

    private fun savePhoto(fileName: String) {
        val res = binding.manipulationView.prepareResults()
        if (res != null) {
            viewModel.savePhoto(fileName, res)
        } else {
            toast(getString(R.string.failed_to_read_photo))
        }
    }

    private fun checkSdPermission(code: Int): Boolean {
        return Permissions.ensurePermissions(this, code, Permissions.READ_EXTERNAL, Permissions.WRITE_EXTERNAL)
    }

    private fun showPopup(
        anchor: View,
        listener: ((Int) -> Unit)?, vararg actions: String
    ) {
        val popupMenu = PopupMenu(anchor.context, anchor)
        popupMenu.setOnMenuItemClickListener { item ->
            listener?.invoke(item.order)
            true
        }
        for (i in actions.indices) {
            popupMenu.menu.add(1, i + 1000, i, actions[i])
        }
        popupMenu.show()
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
        if (Permissions.isAllGranted(grantResults)) {
            if (requestCode == REQ_SD) {
                saveChanges()
            }
        }
    }

    override fun onDestroy() {
        binding.manipulationView.clear()
        viewModel.clear()
        super.onDestroy()
    }

    companion object {
        const val REQ_SD = 1445
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
