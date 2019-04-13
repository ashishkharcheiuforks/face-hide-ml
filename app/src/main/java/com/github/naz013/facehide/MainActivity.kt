package com.github.naz013.facehide

import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import com.github.naz013.facehide.data.RecognitionViewModel
import com.github.naz013.facehide.databinding.*
import com.github.naz013.facehide.utils.Permissions
import com.github.naz013.facehide.utils.PhotoSelectionUtil
import com.github.naz013.facehide.utils.Prefs
import com.github.naz013.facehide.utils.UriUtil
import com.github.naz013.facehide.views.EmojiAdapter
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.jetbrains.anko.toast
import timber.log.Timber

class MainActivity : AppCompatActivity(), PhotoSelectionUtil.UriCallback {

    private lateinit var photoSelectionUtil: PhotoSelectionUtil
    private lateinit var viewModel: RecognitionViewModel
    private lateinit var binding: ActivityMainBinding

    private val adapter = EmojiAdapter()
    private var mDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        binding.loadingView.visibility = View.GONE

        binding.galleryButton.setOnClickListener { photoSelectionUtil.pickFromGallery() }
        binding.cameraButton.setOnClickListener { photoSelectionUtil.takePhoto() }
        binding.moreButton.setOnClickListener { showMorePopup() }
        binding.manipulationView.emojiPopupListener = { index, has ->
            showEmojiPopup(index, has)
        }
        binding.loadingView.setOnClickListener { }

        photoSelectionUtil = PhotoSelectionUtil(this, false, this)
        initViewModel()
    }

    private fun showMorePopup() {
        val builder = AlertDialog.Builder(this)
        val view = DialogMoreBinding.inflate(LayoutInflater.from(this), null, false)
        if (binding.manipulationView.hasPhoto()) {
            view.saveButton.visibility = View.VISIBLE
        } else {
            view.saveButton.visibility = View.GONE
        }
        view.saveButton.setOnClickListener {
            mDialog?.dismiss()
            saveChanges()
        }
        view.settingsButton.setOnClickListener {
            mDialog?.dismiss()
            openSettings()
        }
        builder.setView(view.root)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
        mDialog = dialog
    }

    private fun autoFaceState(b: Boolean): String {
        return if (b) {
            getString(R.string.yes)
        } else {
            getString(R.string.no)
        }
    }

    private fun openSettings() {
        val builder = AlertDialog.Builder(this)
        val view = DialogSettingsBinding.inflate(LayoutInflater.from(this), null, false)
        view.autoFaceState.text = autoFaceState(Prefs.getInstance(this).isAutoFace())
        view.autoButton.setOnClickListener {
            Prefs.getInstance(this).setAutoFace(!Prefs.getInstance(this).isAutoFace())
            view.autoFaceState.text = autoFaceState(Prefs.getInstance(this).isAutoFace())
        }

        try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            view.versionView.text = pInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
        }

        builder.setView(view.root)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
        mDialog = dialog
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
        val view = DialogSaveResultBinding.inflate(LayoutInflater.from(this), null, false)
        view.pathView.text = filePath
        view.viewButton.setOnClickListener {
            mDialog?.dismiss()
            showPhoto(filePath)
        }
        view.cancelButton.setOnClickListener {
            mDialog?.dismiss()
        }
        builder.setView(view.root)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
        mDialog = dialog
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

    private fun showEmojiPopup(face: Int, hasFace: Boolean) {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = DialogEmojiListBinding.inflate(layoutInflater)
        view.emojiList.layoutManager = GridLayoutManager(this, 5)
        adapter.clickListener = {
            binding.manipulationView.setEmojiToFace(face, it)
            bottomSheetDialog.dismiss()
        }
        adapter.setData(emojis.toList())
        view.emojiList.adapter = adapter
        if (hasFace) {
            view.removeButton.visibility = View.VISIBLE
            view.removeButton.setOnClickListener {
                binding.manipulationView.setEmojiToFace(face, 0)
                bottomSheetDialog.dismiss()
            }
        } else {
            view.removeButton.visibility = View.GONE
        }
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
        view.saveButton.setOnClickListener {
            mDialog?.dismiss()
            savePhoto(view.fileNameField.text.toString().trim())
        }
        view.cancelButton.setOnClickListener {
            mDialog?.dismiss()
        }
        builder.setView(view.root)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
        mDialog = dialog
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
