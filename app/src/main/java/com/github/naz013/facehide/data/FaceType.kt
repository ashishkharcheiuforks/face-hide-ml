package com.github.naz013.facehide.data

import com.github.naz013.facehide.R

sealed class FaceType(emoji: Int) {
    object Smiling: FaceType(R.drawable.ic_smiling)
    object NotSmiling: FaceType(R.drawable.ic_confused)
    object Sad: FaceType(R.drawable.ic_sad)
}