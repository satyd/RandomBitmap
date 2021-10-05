package com.levp.randombitmap

import android.graphics.Bitmap
import android.graphics.Point
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.*
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.ViewModelProvider
import com.levp.randombitmap.generate.GenerateFragment
import com.levp.randombitmap.generate.GenerateViewModel
import com.levp.randombitmap.generate.GenerateViewModelFactory
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_generate.*
import kotlinx.android.synthetic.main.play_controls.*
import kotlinx.android.synthetic.main.play_controls.buttonPlay
import kotlinx.coroutines.*
import java.lang.reflect.Field
import java.util.*


class MainActivity : AppCompatActivity() {
    companion object {
        const val gen_fragment_name = "GenerateFragment"
    }
    val colorCount = 256 * 256 * 256
    var screenWidth: Int = 1000
    var screenHeight: Int = 1000

    var resBitmap: Bitmap? = null
    var currBitmap: Bitmap? = null

    private var viewModelJob = Job()
    private var jobList = ArrayList<Job>()
    private var uiScope = CoroutineScope(Dispatchers.Main)

    lateinit var generate : GenerateFragment

    var isPlaying = true
    var instrumentStr: String = "bell"
    var playingNotesOn = true

    //private val generateViewModel: GenerateViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        screenWidth = getRealScreenSize().first
        screenHeight = getRealScreenSize().second

        val application = requireNotNull(this).application
        val genViewModelFactory = GenerateViewModelFactory(application)
        val generateViewModel : GenerateViewModel = ViewModelProvider(this, genViewModelFactory).get(GenerateViewModel::class.java)

        if (savedInstanceState == null) {
            generate = GenerateFragment.newInstance(screenWidth)
            val transaction = supportFragmentManager.beginTransaction()
            transaction.add(main_frag_container.id, generate)
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            transaction.commit()
            //Нужно только если хотим отменять фрагменты кнопкой back, здесь нам это незачем
            //transaction.addToBackStack(null)
        }
        else{
            generate = supportFragmentManager.getFragment(savedInstanceState, gen_fragment_name) as GenerateFragment
        }

        buttonPlay.setOnClickListener {

            currBitmap = generate.getCurrentImage()

            var timesASec = frequencyEditText.text.toString().toFloatOrNull() ?: 2f

            if(timesASec < 0.1)
            {
                Toast.makeText(this@MainActivity, "that's too slow...", Toast.LENGTH_SHORT).show()
            }
            if(timesASec > 15)
            {
                Toast.makeText(this@MainActivity, ">15 won't make any sense :/", Toast.LENGTH_SHORT).show()
            }

            if(timesASec in 0.1..15.0)
                currBitmap?.let { jobList.add(playImage(it)) }

        }


        areNotesOn.setOnCheckedChangeListener { _ , isChecked ->
            playingNotesOn = isChecked
        }

        buttonStop.setOnClickListener {
            if(jobList.size > 0)
            {
                uiScope.cancel()
                jobList.clear()
                uiScope = CoroutineScope(Dispatchers.Main)
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        supportFragmentManager.putFragment(outState, gen_fragment_name, generate)
    }
    override fun onStop() {
        super.onStop()
        uiScope.cancel()
        jobList.clear()
//        supportFragmentManager.beginTransaction().remove(generate).commit()
    }


    private fun getRealScreenSize(): Pair<Int, Int> { //<width, height>
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

    private fun getResId(resName: String, c: Class<*>): Int {
        return try {
            val idField: Field = c.getDeclaredField(resName)
            idField.getInt(idField)
        } catch (e: Exception) {
            e.printStackTrace()
            -1
        }
    }

    private fun playImage(bitmap: Bitmap) : Job {
        return uiScope.launch {

            val instrumentId = getResId(instrumentStr, R.raw::class.java)
            val h = bitmap.height
            val w = bitmap.width
            //Log.d("sizes_heh","$h $w")
            var timesASec = frequencyEditText.text.toString().toFloatOrNull() ?: 2f

            if(timesASec < 0.1)
            {
                timesASec = 0.1f
                Toast.makeText(this@MainActivity, "that's too slow...", Toast.LENGTH_SHORT).show()
            }
            if(timesASec > 12)
            {
                timesASec = 12f
                Toast.makeText(this@MainActivity, "more won't make any sense :/", Toast.LENGTH_SHORT).show()

            }

            val bitRate = 1000f / (timesASec ?: 1f)
            var factor = 1f

            val playingNotes = playingNotesOn

            withContext(Dispatchers.IO) {

                for (y in 0 until h) {
                    for (x in 0 until w) {
                        if (!isActive)
                            break
                        val sound = MediaPlayer.create(this@MainActivity, instrumentId)
                        val pixel = bitmap.getPixel(x, y)
                        factor = sound.duration / bitRate

                        val alpha = pixel.alpha.toFloat()
                        sound.setVolume(alpha / 256f, alpha / 256f)

                        val value = pixel.red + pixel.green + pixel.blue


                        val params: PlaybackParams = PlaybackParams()
                        if(playingNotes)
                        {
                            params.pitch = Notes.getNote(value.toFloat() / 382f + 0.2f)
                        }
                        else{
                            params.pitch = (value.toFloat() / 382f + 0.2f)
                        }

                        params.speed = factor

                        sound.playbackParams = params
                        sound.start()

                        val playDuration = (sound.duration.toLong() / factor).toLong()

                        withContext(Dispatchers.Main) {
                            generate!!.setColor(pixel)
                        }
                        //Log.d("sound", "run #${y * h + x + 1} : ${params.pitch}")
                        delay(playDuration)
                        sound.stop()
                        sound.reset()
                        //sound.release()

                    }
                }
            };
        }
    }

}
