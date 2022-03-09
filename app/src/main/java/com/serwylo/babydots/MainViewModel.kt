package com.serwylo.babydots

import android.app.Application
import android.media.MediaPlayer
import androidx.lifecycle.AndroidViewModel

class MainViewModel(application: Application) : AndroidViewModel(application) {

    lateinit var mediaPlayer: MediaPlayer

    var isMusicOn = false
        set(value) {
            if (value) {
                mediaPlayer.start()
            } else {
                mediaPlayer.pause()
            }

            field = value
        }

    private var currentSong: String? = null

    /**
     * If the song we are currently playing is different to that specified by the preferences,
     * then stop any existing music and re-create the [MediaPlayer].
     *
     * Does *not* play the newly created music player, that is up to you.
     */
    fun reloadMusicPlayer() {
        val songName = Preferences.getSongName(getApplication())
        if (currentSong == songName) {
            return
        }

        currentSong = songName

        if (isMusicOn) {
            mediaPlayer.stop()
        }

        val songRes = when(songName) {
            "canon_in_d_major" -> R.raw.canon_in_d_major
            "gymnopedie_1" -> R.raw.gymnopedie_1
            else -> R.raw.vivaldi
        }

        mediaPlayer = MediaPlayer.create(getApplication(), songRes)
        mediaPlayer.isLooping = true
    }

    fun stopMusic() {
        if (isMusicOn) {
            mediaPlayer.pause()
        }
    }

    fun resumeMusic() {
        if (isMusicOn) {
            mediaPlayer.start()
        }
    }

}