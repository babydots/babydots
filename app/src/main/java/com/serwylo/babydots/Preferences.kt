package com.serwylo.babydots

import android.content.Context
import androidx.preference.PreferenceManager
import java.lang.IllegalArgumentException

/**
 * At this point, intentionally don't remember whether music is on.
 * This is because if you are with a nearly-sleeping baby and then open the app, we don't want
 * to  wake them by accidentally blasting music.
 */
object Preferences {

    private const val PREF_COLOUR_SCHEME = "colourScheme"
    private const val PREF_SPEED = "speed"
    private const val PREF_SIZE = "size"
    private const val PREF_SHAPE = "shape"

    /**
     * Defined in settings.xml (rather than managed by this class)
     */
    private const val PREF_SLEEP_TIMER = "sleep_timer"
    public const val DEFAULT_SLEEP_TIMER = 10

    /**
     * Defined in settings.xml (rather than managed by this class)
     */
    private const val PREF_SONG = "song"
    public const val DEFAULT_SONG = "classical"

    fun getSpeed(context: Context): AnimatedDots.Speed {
        return try {
            val speed = PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_SPEED, null) ?: AnimatedDots.Speed.Normal.toString()
            AnimatedDots.Speed.valueOf(speed)
        } catch (e: IllegalArgumentException) {
            AnimatedDots.Speed.Normal
        }
    }

    fun getColourScheme(context: Context): AnimatedDots.ColourScheme {
        return try {
            val colourScheme = PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_COLOUR_SCHEME, null) ?: AnimatedDots.ColourScheme.SplashOfColour.toString()
            AnimatedDots.ColourScheme.valueOf(colourScheme)
        } catch (e: IllegalArgumentException) {
            AnimatedDots.ColourScheme.SplashOfColour
        }
    }

    fun getSize(context: Context): AnimatedDots.Size {
        return try {
            val size = PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_SIZE, null) ?: AnimatedDots.Size.Medium.toString()
            AnimatedDots.Size.valueOf(size)
        } catch (e: IllegalArgumentException) {
            AnimatedDots.Size.Medium
        }

    }

    fun getShape(context: Context): AnimatedDots.Shape? {
        return try {
            val shape = PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_SHAPE, null)
            if (shape == null) null else AnimatedDots.Shape.valueOf(shape)
        } catch (e: IllegalArgumentException) {
            AnimatedDots.Shape.Circle
        }
    }

    fun getSleepTimerMins(context: Context): Int {
        val pref = PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_SLEEP_TIMER, DEFAULT_SLEEP_TIMER.toString())
        val int = pref?.toInt()
        val number = int ?: 10
        return number
    }

    fun getSongName(context: Context): String {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_SONG, null) ?: DEFAULT_SONG
    }

    fun setSpeed(context: Context, speed: AnimatedDots.Speed) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(PREF_SPEED, speed.toString()).apply()
    }

    fun setColourScheme(context: Context, colourScheme: AnimatedDots.ColourScheme) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(PREF_COLOUR_SCHEME, colourScheme.toString()).apply()
    }

    fun setSize(context: Context, size: AnimatedDots.Size) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(PREF_SIZE, size.toString()).apply()
    }

    fun setShape(context: Context, shape: AnimatedDots.Shape?) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(PREF_SHAPE, shape?.toString()).apply()
    }

}