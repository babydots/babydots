package com.serwylo.babydots

import android.content.Intent
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.leinardi.android.speeddial.SpeedDialView

class MainActivity : AppCompatActivity() {

    private lateinit var dots: AnimatedDots
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var speedDial: SpeedDialView

    private var isMusicOn = false

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        dots = findViewById(R.id.dots)

        dots.setOnClickListener {
            dots.restartDots()
            speedDial.close()
        }

        mediaPlayer = MediaPlayer.create(this, R.raw.classical)
        mediaPlayer.isLooping = true

        speedDial = findViewById<SpeedDialView>(R.id.speed_dial)
        speedDial.inflate(R.menu.speed_dial)
        speedDial.setOnActionSelectedListener { item ->
            when (item.id) {
                R.id.menu_colour -> dots.changeColour()
                R.id.menu_size -> dots.changeSize()
                R.id.menu_speed -> dots.changeSpeed()
            }
            true // Prevents the menu from closing when an option is selected.
        }

    }

    override fun onResume() {
        super.onResume()

        if (isMusicOn) {
            mediaPlayer.start()
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
            R.id.menu_about -> startActivity(Intent(this, AboutActivity::class.java))
        }
        return false
    }

    private fun onSoundSelected(item: MenuItem) {
        if (isMusicOn) {
            mediaPlayer.pause()
            isMusicOn = false
        } else {
            mediaPlayer.start()
            isMusicOn = true
        }

        setMenuIconForSound(item, isMusicOn)
    }

    private fun setMenuIconForSound(item: MenuItem?, isMusicOn: Boolean) {
        if (isMusicOn) {
            item?.setIcon(R.drawable.ic_sound_on)
        } else {
            item?.setIcon(R.drawable.ic_sound_off)
        }
    }
}