package com.levp.randombitmap

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import kotlinx.android.synthetic.main.fragment_generate.*
import kotlin.math.roundToInt
import kotlin.random.Random


fun generateImage(screenWidth: Int, height: Int, width: Int): Pair<Bitmap?,Bitmap?> {
    //val pixels = Array<IntArray>(height){IntArray(width){ Random.nextInt()%256} }
    //val emptyBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    val genBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val pixels5 = IntArray(width * height) { 0 }

    genBitmap.setPixels(pixels5, 0, width, 0, 0, width, height)


    var resHeight = screenWidth / (1.2)
    var resWidth = screenWidth / (1.2)
    if (height > width) {
        resWidth = resWidth * width / height
    }
    if (height < width) {
        resHeight = resHeight * height / width
    }
    val currBitmap = processingBitmap(genBitmap)

    return Pair(currBitmap, currBitmap?.let {
        Bitmap.createScaledBitmap(it, resWidth.roundToInt(), resHeight.roundToInt(), false)
    })


}

fun processingBitmap(src: Bitmap): Bitmap? {
    val dest = Bitmap.createBitmap(src.width, src.height, src.config)

    for (x in 0 until src.width) {
        for (y in 0 until src.height) {
            // получим каждый пиксель
            //val pixelColor = src.getPixel(x, y)
            // получим информацию о прозрачности
            val pixelAlpha: Int = darkAlpha()
            // получим цвет каждого пикселя
            val pixelRed: Int = randPix()
            val pixelGreen: Int = randPix()
            val pixelBlue: Int = randPix()
            // перемешаем цвета
            val newPixel: Int = Color.argb(pixelAlpha, pixelRed, pixelGreen, pixelBlue)
            //Log.d("pixels", "$pixelAlpha $pixelRed $pixelGreen $pixelBlue")
            // полученный результат вернём в Bitmap
            dest.setPixel(x, y, newPixel)
        }
    }

    return dest
}

fun randPix(): Int {
    return Random.nextInt(256)
}

fun darkAlpha(): Int {
    return Random.nextInt(192) + 64
}




fun createWavAndPlay(bitmap: Bitmap) {
    val h = bitmap.height
    val w = bitmap.width
    val byteArray = ByteArray(h * w) { 0 }
    for (x in 0 until w) {
        for (y in 0 until h) {
            byteArray[x * y + y] = bitmap.getPixel(x, y).toByte()
        }
    }
}

fun blur(context: Context?, image: Bitmap): Bitmap? {
    val BITMAP_SCALE = 0.4f;
    val BLUR_RADIUS = 17.5f;

    val width = (image.width * BITMAP_SCALE).roundToInt()
    val height = (image.height * BITMAP_SCALE).roundToInt()
    val inputBitmap = Bitmap.createScaledBitmap(image, width, height, false)
    val outputBitmap = Bitmap.createBitmap(inputBitmap)
    val rs = RenderScript.create(context)
    val theIntrinsic = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
    val tmpIn = Allocation.createFromBitmap(rs, inputBitmap)
    val tmpOut = Allocation.createFromBitmap(rs, outputBitmap)
    theIntrinsic.setRadius(BLUR_RADIUS)
    theIntrinsic.setInput(tmpIn)
    theIntrinsic.forEach(tmpOut)
    tmpOut.copyTo(outputBitmap)
    return outputBitmap
}
