package com.levp.randombitmap.generate

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.lifecycle.ViewModelProvider
import com.levp.randombitmap.*
import kotlinx.android.synthetic.main.fragment_generate.*

private const val ARG_SCREEN_WIDTH = "screenWidth"
class GenerateFragment : Fragment() {

    companion object {
        var areParamsSet = false


        var currImage: Bitmap? = null

        @JvmStatic
        fun newInstance(screenWidth: Int) =
            GenerateFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SCREEN_WIDTH, screenWidth)


                }
            }
    }
    var screenWidth: Int = 1000

    lateinit var generateViewModel : GenerateViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            screenWidth = it.getInt(ARG_SCREEN_WIDTH)
            areParamsSet = true

        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        //Log.d("ViewModel", "fragment created!")
        val application = requireNotNull(this.activity).application
        val genViewModelFactory = GenerateViewModelFactory(application)
        generateViewModel = ViewModelProvider(requireActivity().viewModelStore, genViewModelFactory).get(GenerateViewModel::class.java)

        return inflater.inflate(R.layout.fragment_generate, container, false)

    }

    override fun onStart() {
        super.onStart()
        //Log.d("ViewModel", "${generateViewModel.generatedPicture.value?.second?.height}")
        if(generateViewModel.generatedPicture.value?.first !=null)
        {
            imageView.setImageBitmap(generateViewModel.generatedPicture.value?.second)
            //Log.e("ViewModel", "THIS SHIT FINALLY WORKED")
        }
        buttonGenerate.setOnClickListener {
            val height = imageHeightTextView.text.toString().toIntOrNull() ?: 9
            val width = imageWidthTextView.text.toString().toIntOrNull() ?: 9

            generateViewModel.picHeight.value = height
            generateViewModel.picWidth.value = width

            if (width in 0..500 && height in 0..500) {
                val res = generateImage(screenWidth, height, width)

                generateViewModel.generatedPicture.value = res
                //Log.d("ViewModelInit", "${generateViewModel.generatedPicture.value?.second?.height}")
                currImage = res.first
                imageView.setImageBitmap(res.second)
            }

        }
    }

//    override fun onStop() {
//        super.onStop()
//        activity?.supportFragmentManager?.beginTransaction()?.remove(this)?.commit()
//    }
    fun getCurrentImage(): Bitmap? {
        return currImage
    }

    fun setColor(pixel: Int) {
        currentColor.setBackgroundColor(pixel)

        val alph = pixel.alpha.toString(16)
        val red = pixel.red.toString(16)
        val green = pixel.green.toString(16)
        val blue = pixel.blue.toString(16)

        colorCodeTW.text = "current color:\n#$alph $red $green $blue"
    }

    fun clearViews() {
        currentColor.setBackgroundColor(0)
        colorCodeTW.text = "current color:\n"
    }

    fun initParams(width: Int) {
        screenWidth = width
        areParamsSet = true
    }


}