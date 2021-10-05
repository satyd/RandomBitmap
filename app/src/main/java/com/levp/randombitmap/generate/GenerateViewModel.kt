package com.levp.randombitmap.generate

import android.R.attr.data
import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData


class GenerateViewModel(application: Application) : AndroidViewModel(application) {

    var generatedPicture = MutableLiveData<Pair<Bitmap?, Bitmap?>>()

    val picHeight = MutableLiveData<Int>()
    val picWidth = MutableLiveData<Int>()


}