package com.levp.randombitmap

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Point
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import java.lang.reflect.Field
import java.util.*
import kotlin.math.roundToInt
import kotlin.random.Random


class MainActivity : AppCompatActivity() {
    val colorCount = 256 * 256 * 256
    var screenWidth: Int = 1000
    var screenHeight: Int = 1000

    var resBitmap: Bitmap? = null
    var currBitmap: Bitmap? = null

    private var viewModelJob = Job()
    private var uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    var timesASec = 2.0f
    var minTimesASec = 2.0f
    var isPlaying = true
    var instrumentStr: String = "bell"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        screenWidth = getRealScreenSize().first
        screenHeight = getRealScreenSize().second

        buttonGenerate.setOnClickListener {
            val height = imageHeightTextView.text.toString().toIntOrNull() ?: 5
            val width = imageWidthTextView.text.toString().toIntOrNull() ?: 5
            generateImage(height, width)
        }

        buttonPlay.setOnClickListener {

            currBitmap?.let { playImage(it) }
        }
        buttonStop.setOnClickListener {
            CoroutineScope(Dispatchers.Default).launch{
                viewModelJob.cancelAndJoin()
                isPlaying = false
                delay((1000L / minTimesASec).toLong())
                isPlaying = true
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "playing stopped", Toast.LENGTH_SHORT).show()
                }

                //viewModelJob.complete()
            }

        }
        val spinner = findViewById<Spinner>(R.id.pickInstrumentSpinner)

        val adapter: ArrayAdapter<*> = ArrayAdapter.createFromResource(
            this,
            R.array.instruments,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        // Вызываем адаптер
        spinner.adapter = adapter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                itemSelected: View?, selectedItemPosition: Int, selectedId: Long
            ) {
                val choose = resources.getStringArray(R.array.instruments)
                instrumentStr = choose[selectedItemPosition].toLowerCase(Locale.ROOT)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun generateImage(height: Int, width: Int) {
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
        currBitmap = processingBitmap(genBitmap)

        resBitmap = currBitmap?.let {
            Bitmap.createScaledBitmap(it, resWidth.roundToInt(), resHeight.roundToInt(), false)
        }


        imageView.setImageBitmap(resBitmap)

    }

    fun randPix(): Int {
        val logger = Random.nextInt(256)
        return logger
    }

    fun darkAlpha(): Int {
        val logger = Random.nextInt(192) + 64
        return logger
    }

    private fun processingBitmap(src: Bitmap): Bitmap? {
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

    fun getRealScreenSize(): Pair<Int, Int> { //<width, height>
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val size = Point()
            display?.getRealSize(size)
            Pair(size.x, size.y)
        } else {
            val size = Point()
            windowManager.defaultDisplay.getRealSize(size)
            Pair(size.x, size.y)

        }
    }

    fun getResId(resName: String, c: Class<*>): Int {
        return try {
            val idField: Field = c.getDeclaredField(resName)
            idField.getInt(idField)
        } catch (e: Exception) {
            e.printStackTrace()
            -1
        }
    }

    fun setInstrument(string: String): Int {
        MainActivity::class.java.getResource("/res/raw/$string.wav")
        when (string) {
            "Bell" -> {
                return R.raw.bell
            }
        }
        return -1
    }

    fun playImage(bitmap: Bitmap) {
        val instrumentId = getResId(instrumentStr, R.raw::class.java)
        val h = bitmap.height
        val w = bitmap.width
        timesASec = frequencyEditText.text.toString().toFloatOrNull() ?: 2.0f
        if(minTimesASec > timesASec)
            minTimesASec = timesASec
        val bitRate = 1000f / timesASec
        var factor = 1f
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            viewModelJob = Job(uiScope.launch {
                withContext(Dispatchers.IO) {
                    //Do background tasks...

                    for (y in 0 until h) {
                        for (x in 0 until w) {
                            if(!isPlaying)
                                break
                            //val bells = MediaPlayer.create(this@MainActivity, R.raw.bell)
                            val bells = MediaPlayer.create(this@MainActivity, instrumentId)
                            factor = bells.duration / bitRate
                            val pixel = bitmap.getPixel(x, y)
                            val alpha = pixel.alpha.toFloat()
                            bells.setVolume(alpha / 256f, alpha / 256f)

                            //bells.setVolume(pixel.alpha.toFloat() / 256, pixel.alpha.toFloat() / 256)
                            var value = pixel.red + pixel.green + pixel.blue


                            val params: PlaybackParams = PlaybackParams()
                            params.pitch = (value.toFloat() / 382f + 0.2f)
                            params.speed = factor
                            bells.playbackParams = params
                            //bells.playbackParams.pitch = (blue.toFloat())

                            bells.start()
                            val playDuration = (bells.duration.toLong() / factor).toLong()
                            withContext(Dispatchers.Main) {
                                currentColor.setBackgroundColor(pixel)

                                val alph = pixel.alpha.toString(16)
                                val red = pixel.red.toString(16)
                                val green = pixel.green.toString(16)
                                val blue = pixel.blue.toString(16)

                                colorCodeTW.text = "current color:\n#$alph $red $green $blue"

                            }
                            delay(playDuration)

                            //Log.d("sound", "run #${y * h + x + 1} : ${params.pitch}")
                            bells.stop()
                            bells.reset()
                            bells.release()

                            if(!isPlaying)
                                break
                        }

                    }
                }
            })


        }
    }

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
