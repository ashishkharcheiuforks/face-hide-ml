package com.github.naz013.facehide.utils

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.appcompat.app.AlertDialog
import com.github.naz013.facehide.R
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class PhotoSelectionUtil(private val activity: Activity, private val urlSupported: Boolean = true, private val mCallback: UriCallback?) {

    private var imageUri: Uri? = null

    fun selectImage() {
        val hasCamera = Module.hasCamera(activity)
        val items = if (urlSupported) {
            if (hasCamera) {
                arrayOf(
                        activity.getString(R.string.gallery),
                        activity.getString(R.string.take_a_shot)
                )
            } else {
                arrayOf(activity.getString(R.string.gallery))
            }
        } else {
            if (hasCamera) {
                arrayOf(activity.getString(R.string.gallery), activity.getString(R.string.take_a_shot))
            } else {
                arrayOf(activity.getString(R.string.gallery))
            }
        }
        val builder = AlertDialog.Builder(activity)
        builder.setTitle(R.string.image)
        builder.setItems(items) { dialog, item ->
            dialog.dismiss()
            if (hasCamera) {
                when (item) {
                    0 -> pickFromGallery()
                    1 -> takePhoto()
                }
            } else {
                when (item) {
                    0 -> pickFromGallery()
                }
            }
        }
        builder.create().show()
    }

    private fun pickFromGallery() {
        if (!checkSdPermission(REQUEST_SD_CARD)) {
            return
        }
        var intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        if (urlSupported) {
            intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "image/*"
        }
        val chooser = Intent.createChooser(intent, activity.getString(R.string.gallery))
        try {
            activity.startActivityForResult(chooser, PICK_FROM_GALLERY)
        } catch (e: ActivityNotFoundException) {
            checkSdPermission(REQUEST_SD_CARD)
        }
    }

    private fun checkSdPermission(code: Int): Boolean {
        return Permissions.ensurePermissions(activity, code, Permissions.READ_EXTERNAL, Permissions.WRITE_EXTERNAL)
    }

    private fun checkCameraPermission(code: Int): Boolean {
        return Permissions.ensurePermissions(activity, code, Permissions.READ_EXTERNAL, Permissions.WRITE_EXTERNAL, Permissions.CAMERA)
    }

    private fun showPhoto(imageUri: Uri) {
        Timber.d("showPhoto: %s", imageUri)
        mCallback?.onImageSelected(imageUri, null)
    }

    private fun takePhoto() {
        if (!checkCameraPermission(REQUEST_CAMERA)) {
            return
        }
        val pictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (pictureIntent.resolveActivity(activity.packageManager) == null) {
            return
        }
        if (Module.isNougat) {
            if (pictureIntent.resolveActivity(activity.packageManager) != null) {
                val photoFile = createImageFile()
                imageUri = UriUtil.getUri(activity, photoFile)
                pictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
                try {
                    activity.startActivityForResult(pictureIntent, PICK_FROM_CAMERA)
                } catch (e: ActivityNotFoundException) {
                    checkCameraPermission(REQUEST_CAMERA)
                }
            }
        } else {
            val values = ContentValues()
            values.put(MediaStore.Images.Media.TITLE, "Picture")
            values.put(MediaStore.Images.Media.DESCRIPTION, "From your Camera")
            imageUri = activity.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            pictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
            try {
                activity.startActivityForResult(pictureIntent, PICK_FROM_CAMERA)
            } catch (e: ActivityNotFoundException) {
                checkCameraPermission(REQUEST_CAMERA)
            }
        }
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val imageFileName = "IMG_" + timeStamp + "_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        if (!storageDir.exists()) storageDir.mkdirs()
        return File(storageDir, "$imageFileName.jpg")
    }

    private fun getExternalFilesDir(directoryPictures: String): File {
        val sd = Environment.getExternalStorageDirectory()
        return File(sd, File(directoryPictures, "Reminder").toString())
    }

    @Suppress("UNUSED_PARAMETER")
    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (Permissions.isAllGranted(grantResults)) {
            when (requestCode) {
                REQUEST_SD_CARD -> pickFromGallery()
                REQUEST_CAMERA -> takePhoto()
            }
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Timber.d("onActivityResult: %d, %d, %s", requestCode, resultCode, data)
        if (requestCode == PICK_FROM_CAMERA && resultCode == Activity.RESULT_OK) {
            val uri = imageUri ?: return
            showPhoto(uri)
        } else if (requestCode == PICK_FROM_GALLERY && resultCode == Activity.RESULT_OK) {
            imageUri = data?.data
            val clipData = data?.clipData
            val uri = imageUri
            if (uri != null) {
                showPhoto(uri)
            } else if (clipData != null) {
                mCallback?.onImageSelected(null, clipData)
            }
        }
    }

    interface UriCallback {
        fun onImageSelected(uri: Uri?, clipData: ClipData?)

        fun onBitmapReady(bitmap: Bitmap)
    }

    companion object {

        private const val PICK_FROM_GALLERY = 25
        private const val PICK_FROM_CAMERA = 26
        private const val REQUEST_SD_CARD = 1112
        private const val REQUEST_CAMERA = 1113
    }
}
