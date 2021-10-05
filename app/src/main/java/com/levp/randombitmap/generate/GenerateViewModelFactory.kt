package com.levp.randombitmap.generate

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class GenerateViewModelFactory ( private val application: Application) : ViewModelProvider.Factory{
    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {

        if (modelClass.isAssignableFrom(GenerateViewModel::class.java)) {
            return GenerateViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}