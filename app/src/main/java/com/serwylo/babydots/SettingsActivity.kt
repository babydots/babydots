package com.serwylo.babydots

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.preference.*

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_settings)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        supportFragmentManager.beginTransaction().replace(R.id.settings_wrapper, Fragment()).commit()
    }

    class Fragment : PreferenceFragmentCompat() {

        private val sleepTimerMinutes = arrayOf(3, 10, 30, 60)

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            addPreferencesFromResource(R.xml.settings)

            findPreference<ListPreference>("song")?.apply {
                setDefaultValue(Preferences.DEFAULT_SONG)
                entries = resources.getStringArray(R.array.song_labels)
                entryValues = resources.getStringArray(R.array.song_keys)
                summaryProvider = Preference.SummaryProvider<ListPreference> {
                    it.entry ?: getString(R.string.song_vivaldi)
                }
            }

            findPreference<ListPreference>("sleep_timer")?.apply {
                setDefaultValue(Preferences.DEFAULT_SLEEP_TIMER.toString())
                entries = sleepTimerMinutes.map { resources.getQuantityString(R.plurals.pref_x_minutes, it, it) }.toTypedArray()
                entryValues = sleepTimerMinutes.map { it.toString() }.toTypedArray()
                summaryProvider = Preference.SummaryProvider<ListPreference> {
                    val mins = it.value?.toInt() ?: Preferences.DEFAULT_SLEEP_TIMER
                    resources.getQuantityString(R.plurals.pref_x_minutes, mins, mins)
                }
            }
        }

    }

}
