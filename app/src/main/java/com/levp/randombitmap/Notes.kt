package com.levp.randombitmap

import android.util.Log
import kotlin.math.pow

object Notes {
    val notes = FloatArray(25){0.0f}
    init {
        for(i in -12..12){
            notes[i+12] = toDeg(i, 12)
        }
    }

    fun toDeg(a:Int, b:Int):Float{
        return 2.0.pow(a.toDouble() / b.toDouble()).toFloat()
    }

    fun getNote(pitch : Float) : Float{
        return notes.find { it > pitch } ?: pitch
    }

    fun check(){
        for(i in notes.indices)
            Log.d("notes :::: -->","note #$i = ${notes[i]}")
    }
}