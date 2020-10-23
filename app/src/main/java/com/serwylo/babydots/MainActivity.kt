package com.serwylo.babydots

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.leinardi.android.speeddial.SpeedDialView
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var dots: AnimatedDots
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var speedDial: SpeedDialView
    private lateinit var sleepTimeWrapper: View
    private lateinit var toolbar: Toolbar
    private lateinit var timerWrapper: View
    private lateinit var timerLabel: TextView
    private lateinit var timerIcon: ImageView

    private var isMusicOn = false
        set(value) {
            if (value) {
                mediaPlayer.start()
            } else {
                mediaPlayer.pause()
            }

            field = value
        }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        dots = findViewById(R.id.dots)
        dots.colourScheme = Preferences.getColourScheme(this)
        dots.speed = Preferences.getSpeed(this)
        dots.size = Preferences.getSize(this)

        dots.setOnClickListener {
            dots.restartDots()
            speedDial.close()
        }

        mediaPlayer = MediaPlayer.create(this, R.raw.classical)
        mediaPlayer.isLooping = true

        speedDial = findViewById(R.id.speed_dial)
        speedDial.inflate(R.menu.speed_dial)
        speedDial.setOnActionSelectedListener { item ->
            when (item.id) {
                R.id.menu_colour -> changeColour()
                R.id.menu_size -> changeSize()
                R.id.menu_speed -> changeSpeed()
                R.id.menu_timer -> startTimer()
            }
            true // Prevents the menu from closing when an option is selected.
        }

        sleepTimeWrapper = findViewById(R.id.sleep_time_wrapper)
        timerWrapper = findViewById(R.id.timer_wrapper)
        timerLabel = findViewById(R.id.timer)
        timerIcon = findViewById(R.id.timer_icon)

        timerWrapper.setOnClickListener {
            promptToStopTimer()
        }

        sleepTimeWrapper.setOnClickListener {
            promptToCancelSleepTime()
        }
    }

    override fun onPause() {
        super.onPause()

        if (isMusicOn) {
            mediaPlayer.pause()
        }

        pauseTimer()
    }

    override fun onResume() {
        super.onResume()

        if (isMusicOn) {
            mediaPlayer.start()
        }

        if (timerCounter > 0) {
            resumeTimer()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)

        setMenuIconForSound(menu?.findItem(R.id.menu_sound), isMusicOn)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_sound -> onSoundSelected(item)
            R.id.menu_settings -> startActivity(Intent(this, SettingsActivity::class.java))
            R.id.menu_about -> startActivity(Intent(this, AboutActivity::class.java))
        }
        return false
    }

    private fun onSoundSelected(item: MenuItem) {
        isMusicOn = !isMusicOn

        setMenuIconForSound(item, isMusicOn)
    }

    private fun setMenuIconForSound(item: MenuItem?, isMusicOn: Boolean) {
        item?.setIcon(
            if (isMusicOn) R.drawable.ic_sound_on else R.drawable.ic_sound_off
        )
    }

    private fun changeSize() {
        dots.size = when (dots.size) {
            AnimatedDots.Size.Large -> AnimatedDots.Size.Small
            AnimatedDots.Size.Medium -> AnimatedDots.Size.Large
            else -> AnimatedDots.Size.Medium
        }

        Preferences.setSize(this, dots.size)
    }

    private fun changeColour() {
        dots.colourScheme = when (dots.colourScheme) {
            AnimatedDots.ColourScheme.Rainbow -> AnimatedDots.ColourScheme.SplashOfColour
            AnimatedDots.ColourScheme.SplashOfColour -> AnimatedDots.ColourScheme.Monochrome
            AnimatedDots.ColourScheme.Monochrome -> AnimatedDots.ColourScheme.Dark
            AnimatedDots.ColourScheme.Dark -> AnimatedDots.ColourScheme.Neon
            AnimatedDots.ColourScheme.Neon -> AnimatedDots.ColourScheme.Rainbow
        }

        Preferences.setColourScheme(this, dots.colourScheme)
    }

    private fun changeSpeed() {
        dots.speed = when (dots.speed) {
            AnimatedDots.Speed.Slow -> AnimatedDots.Speed.Normal
            AnimatedDots.Speed.Normal -> AnimatedDots.Speed.Fast
            AnimatedDots.Speed.Fast -> AnimatedDots.Speed.Slow
        }

        Preferences.setSpeed(this, dots.speed)
    }

    private var timer: Timer? = null
    private var timerCounter = 0L

    private fun sleepTimer(): Long {
        return (Preferences.getSleepTimerMins(this) * 60 * 1000).toLong()
    }

    private fun startTimer() {
        timer?.cancel()
        timerCounter = 0

        resumeTimer()
    }

    private fun resumeTimer() {
        timer = Timer()

        timer?.schedule(object : TimerTask() {
            override fun run() {
                timerCounter += 1000
                runOnUiThread {
                    if (timerCounter > sleepTimer()) {
                        startSleepTime()
                    } else {
                        updateTimer()
                    }
                }
            }
        }, 1000, 1000)

        runOnUiThread {
            timerWrapper.visibility = View.VISIBLE
            updateTimer()
        }
    }

    private fun promptToStopTimer() {
        AlertDialog.Builder(this)
            .setTitle(R.string.sleep_timer)
            .setMessage(getString(R.string.sleep_timer_description))
            .setNegativeButton(R.string.back, null)
            .setNeutralButton(R.string.settings) { _, _ -> startActivity(Intent(this, SettingsActivity::class.java))}
            .setPositiveButton(R.string.stop_timer_button) { _, _ -> cancelTimer() }
            .create()
            .show()
    }

    private fun updateTimer() {

        val timeLeft = sleepTimer() - timerCounter

        val seconds = (timeLeft / 1000) % 60
        val minutes = (timeLeft / 1000) / 60

        val secondPadding = if (seconds < 10) "0" else ""

        val label = "${minutes}:${secondPadding}${seconds}"
        timerLabel.text = label
    }

    private fun cancelTimer() {
        timer?.cancel()
        timer = null
        timerCounter = 0

        timerWrapper.visibility = View.GONE
    }

    private fun pauseTimer() {
        timer?.cancel()
        timer = null
    }

    /**
     * Does a multitude of things:
     *  - Stops music
     *  - Stops the timer
     *  - Changes the timer to say "Sleep time"
     *  - Release the wake lock
     *  - Attach listener to timer for a prompt to cancel sleep time
     */
    private fun startSleepTime() {
        if (isMusicOn) {
            isMusicOn = false
            invalidateOptionsMenu()
        }

        timer?.cancel()
        timer = null

        sleepTimeWrapper.visibility = View.VISIBLE

        timerWrapper.visibility = View.INVISIBLE
        toolbar.visibility = View.INVISIBLE
        dots.visibility = View.INVISIBLE
        speedDial.visibility = View.INVISIBLE

        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LOW_PROFILE

    }

    private fun promptToCancelSleepTime() {
        AlertDialog.Builder(this)
            .setTitle(R.string.sleep_time)
            .setMessage(getString(R.string.stop_sleep_time_message))
            .setNegativeButton(getString(R.string.back), null)
            .setPositiveButton(getString(R.string.resume_dots_button)) { _, _ -> cancelSleepTime() }
            .create()
            .show()
    }

    private fun cancelSleepTime() {
        sleepTimeWrapper.visibility = View.INVISIBLE
        timerWrapper.visibility = View.INVISIBLE

        toolbar.visibility = View.VISIBLE
        dots.visibility = View.VISIBLE
        speedDial.visibility = View.VISIBLE

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
    }

    override fun onDestroy() {
        cancelTimer()
        super.onDestroy()
    }

}
