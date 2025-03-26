package com.serwylo.babydots

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.res.ResourcesCompat
import com.leinardi.android.speeddial.SpeedDialActionItem
import com.leinardi.android.speeddial.SpeedDialView
import com.serwylo.immersivelock.ImmersiveLock
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var dots: AnimatedDots
    private lateinit var speedDial: SpeedDialView
    private lateinit var sleepTimeWrapper: View
    private lateinit var toolbar: Toolbar
    private lateinit var timerWrapper: View
    private lateinit var timerLabel: TextView
    private lateinit var timerIcon: ImageView
    private lateinit var immersiveLock: ImmersiveLock

    private val viewModel: MainViewModel by viewModels()

    /**
     * Remember this item so that we can swap the "Sleep timer" with the "Stop timer".
     */
    private lateinit var sleepTimerMenuItem: SpeedDialActionItem

    companion object {
        /**
         * Starting the MainActivity with this mode will force it into sleep time.
         * This is used for automated screenshot generation so we can easily get a screenshot
         * of sleep time without having to wait 10 mins.
         */
        const val EXTRA_SLEEP_TIME = "com.serwylo.babydots.MainActivity.EXTRA_SLEEP_MODE"

        /**
         * To escape sticky immersive mode when locking the screen, the user must press the lock
         * icon in the corner this many times.
         */
        const val TOUCHES_REQUIRED_TO_UNLOCK = 5
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        speedDial = findViewById(R.id.speed_dial)

        dots = findViewById(R.id.dots)
        dots.colourScheme = Preferences.getColourScheme(this)
        dots.speed = Preferences.getSpeed(this)
        dots.size = Preferences.getSize(this)
        dots.shape = Preferences.getShape(this)

        dots.setOnClickListener {
            speedDial.close()
        }

        viewModel.reloadMusicPlayer()

        sleepTimerMenuItem = SpeedDialActionItem.Builder(R.id.menu_speed_dial_timer, R.drawable.ic_timer)
            .setLabel(R.string.sleep_timer)
            .setFabImageTintColor(ResourcesCompat.getColor(resources, R.color.white, theme))
            .create()

        speedDial.addActionItem(sleepTimerMenuItem)

        speedDial.addActionItem(
            SpeedDialActionItem.Builder(R.id.menu_speed_dial_colour, R.drawable.ic_colour)
                .setLabel(R.string.colour_scheme)
                .setFabImageTintColor(ResourcesCompat.getColor(resources, R.color.white, theme))
                .create()
        )

        speedDial.addActionItem(
            SpeedDialActionItem.Builder(R.id.menu_speed_dial_size, R.drawable.ic_size)
                .setLabel(R.string.size)
                .setFabImageTintColor(ResourcesCompat.getColor(resources, R.color.white, theme))
                .create()
        )

        speedDial.addActionItem(
            SpeedDialActionItem.Builder(R.id.menu_speed_dial_speed, R.drawable.ic_speed)
                .setLabel(R.string.speed)
                .setFabImageTintColor(ResourcesCompat.getColor(resources, R.color.white, theme))
                .create()
        )

        speedDial.addActionItem(
            SpeedDialActionItem.Builder(R.id.menu_speed_dial_shape, R.drawable.ic_shape)
                .setLabel(R.string.shape)
                .setFabImageTintColor(ResourcesCompat.getColor(resources, R.color.white, theme))
                .create()
        )

        speedDial.setOnActionSelectedListener { item ->
            when (item.id) {
                R.id.menu_speed_dial_colour -> changeColour()
                R.id.menu_speed_dial_size -> changeSize()
                R.id.menu_speed_dial_speed -> changeSpeed()
                R.id.menu_speed_dial_timer -> toggleTimer()
                R.id.menu_speed_dial_shape -> changeShape()
            }
            true // Prevents the menu from closing when an option is selected.
        }

        immersiveLock = ImmersiveLock.Builder(findViewById(R.id.unlock_wrapper))
            .onStopImmersiveMode {
                speedDial.visibility = View.VISIBLE
                toolbar.visibility = View.VISIBLE
            }
            .build()

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

        if (intent.getBooleanExtra(EXTRA_SLEEP_TIME, false)) {
            startSleepTime()
        }
    }

    override fun onPause() {
        super.onPause()

        viewModel.stopMusic()

        pauseTimer()
    }

    override fun onResume() {
        super.onResume()

        // If we are returning from settings, then we may have changed the song - in which case
        // we need to re-create the music player.
        viewModel.reloadMusicPlayer()

        viewModel.resumeMusic()

        if (timerCounter > 0) {
            resumeTimer()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)

        setMenuIconForSound(menu.findItem(R.id.menu_sound), viewModel.isMusicOn)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_sound -> onSoundSelected(item)
            R.id.menu_settings -> startActivity(Intent(this, SettingsActivity::class.java))
            R.id.menu_lock -> {
                immersiveLock.startImmersiveMode(this)
                speedDial.visibility = View.GONE
                toolbar.visibility = View.GONE
            }
            R.id.menu_about -> startActivity(Intent(this, AboutActivity::class.java))
        }
        return false
    }

    private fun onSoundSelected(item: MenuItem) {
        viewModel.isMusicOn = !viewModel.isMusicOn

        setMenuIconForSound(item, viewModel.isMusicOn)
    }

    private fun setMenuIconForSound(item: MenuItem, isMusicOn: Boolean) {
        item.setIcon(
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
            AnimatedDots.ColourScheme.Rainbow -> AnimatedDots.ColourScheme.BrightRainbow
            AnimatedDots.ColourScheme.BrightRainbow -> AnimatedDots.ColourScheme.SplashOfColour
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

    private fun changeShape() {
        dots.shape = when(dots.shape) {
            AnimatedDots.Shape.Circle -> AnimatedDots.Shape.Square
            AnimatedDots.Shape.Square -> AnimatedDots.Shape.Triangle
            AnimatedDots.Shape.Triangle -> AnimatedDots.Shape.Heart
            AnimatedDots.Shape.Heart -> null 
            null -> AnimatedDots.Shape.Circle
        }

        Preferences.setShape(this, dots.shape)
    }

    private var timer: Timer? = null
    private var timerCounter = 0L

    private fun sleepTimer(): Long {
        return (Preferences.getSleepTimerMins(this) * 60 * 1000).toLong()
    }

    private fun toggleTimer() {
        if (timerCounter > 0) {
            cancelTimer()
        } else {
            startTimer()
        }
    }

    private fun startTimer() {
        timer?.cancel()
        timerCounter = 1000

        resumeTimer()

        Toast.makeText(this, R.string.sleep_timer_started_help_text, Toast.LENGTH_SHORT).show()
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

            val stopTimerMenuItem = SpeedDialActionItem.Builder(R.id.menu_speed_dial_timer, R.drawable.ic_stop_sleep_timer)
                .setLabel(R.string.stop_timer_button)
                .setFabImageTintColor(ResourcesCompat.getColor(resources, R.color.white, theme))
                .create()
            speedDial.replaceActionItem(sleepTimerMenuItem, stopTimerMenuItem)
            sleepTimerMenuItem = stopTimerMenuItem

            updateTimer()
        }
    }

    private fun promptToStopTimer() {
        AlertDialog.Builder(this)
            .setTitle(R.string.sleep_timer)
            .setMessage(getString(R.string.sleep_timer_description))
            .setNegativeButton(R.string.back, null)
            .setNeutralButton(R.string.settings) { _, _ -> startActivity(
                Intent(
                    this,
                    SettingsActivity::class.java
                )
            )}
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

        val startTimerMenuItem = SpeedDialActionItem.Builder(R.id.menu_speed_dial_timer, R.drawable.ic_timer)
            .setLabel(R.string.sleep_timer)
            .setFabImageTintColor(ResourcesCompat.getColor(resources, R.color.white, theme))
            .create()
        speedDial.replaceActionItem(sleepTimerMenuItem, startTimerMenuItem)
        sleepTimerMenuItem = startTimerMenuItem
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
        if (viewModel.isMusicOn) {
            viewModel.isMusicOn = false
            invalidateOptionsMenu()
        }

        timer?.cancel()
        timer = null

        sleepTimeWrapper.visibility = View.VISIBLE

        timerWrapper.visibility = View.INVISIBLE
        toolbar.visibility = View.INVISIBLE
        dots.visibility = View.INVISIBLE
        speedDial.visibility = View.INVISIBLE

        @Suppress("DEPRECATION") // The recommended alternative was only introduced in API 30.
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        @Suppress("DEPRECATION") // The recommended alternative was only introduced in API 30.
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

        @Suppress("DEPRECATION") // The recommended alternative was only introduced in API 30.
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        @Suppress("DEPRECATION") // The recommended alternative was only introduced in API 30.
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
    }

    override fun onDestroy() {
        cancelTimer()
        super.onDestroy()
    }

}
