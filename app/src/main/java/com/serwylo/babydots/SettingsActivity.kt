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

            val sleepTimer = findPreference<ListPreference>("sleep_timer")
            sleepTimer?.setDefaultValue(Preferences.DEFAULT_SLEEP_TIMER.toString())
            sleepTimer?.entries = sleepTimerMinutes.map { getString(R.string.pref_x_minutes, it) }.toTypedArray()
            sleepTimer?.entryValues = sleepTimerMinutes.map { it.toString() }.toTypedArray()
            sleepTimer?.summaryProvider = Preference.SummaryProvider<ListPreference> { getString(R.string.pref_x_minutes, it.value?.toInt() ?: Preferences.DEFAULT_SLEEP_TIMER) }
        }

    }

}
