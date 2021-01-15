package com.serwylo.babydots

import android.content.Intent
import androidx.annotation.IdRes
import androidx.preference.PreferenceManager
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.GeneralClickAction
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Tap
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.locale.LocaleTestRule


@LargeTest
@RunWith(AndroidJUnit4::class)
class FastlaneScreengrabTest {

    @Rule
    @JvmField
    val activityTestRule = object : ActivityTestRule<MainActivity>(MainActivity::class.java) {
        override fun beforeActivityLaunched() {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
            super.beforeActivityLaunched()
        }
    }

    /**
     * Required for Fastlane to be able to automatically switch locales:
     * https://docs.fastlane.tools/getting-started/android/screenshots/
     */
    @Rule
    @JvmField
    val localeTestRule = LocaleTestRule()

    /**
     * Note: Naming of screenshots is all off, incorrect order, and not informative enough.
     * This is because of https://gitlab.com/fdroid/fdroidserver/-/issues/490 whereby old
     * screenshots are not deleted from the index. If we rename the images now, we end up with
     * duplicate copies in the repo.
     */
    @Test
    fun mainActivityTest() {

        Screengrab.screenshot("01_splash_of_colour")

        clickFloatingActionButton()
        clickSpeedDialButton(R.id.menu_speed_dial_colour)
        clickSpeedDialButton(R.id.menu_speed_dial_colour)

        // Rename to 02_dark_with_menu when fdroidserver#490 is fixed
        Screengrab.screenshot("04_dark")

        clickSpeedDialButton(R.id.menu_speed_dial_colour)
        clickSpeedDialButton(R.id.menu_speed_dial_size)
        clickSpeedDialButton(R.id.menu_speed_dial_size)
        clickDots()

        // Rename to 03_neon_small when fdroidserver#490 is fixed
        Screengrab.screenshot("05_neon")

        clickFloatingActionButton()
        clickSpeedDialButton(R.id.menu_speed_dial_colour)
        clickSpeedDialButton(R.id.menu_speed_dial_size)
        clickSpeedDialButton(R.id.menu_speed_dial_size)
        clickDots()

        // Rename to 04_rainbow_large when fdroidserver#490 is fixed
        Screengrab.screenshot("02_rainbow")

        clickFloatingActionButton()
        clickSpeedDialButton(R.id.menu_speed_dial_colour)
        clickSpeedDialButton(R.id.menu_speed_dial_size)
        clickSpeedDialButton(R.id.menu_speed_dial_timer)
        Thread.sleep(3000) // Wait for toast message to disappear

        // Rename to 05_timer_with_menu when fdroidserver#490 is fixed
        Screengrab.screenshot("06_sleep_timer")

        clickDots() // Hide menu
        clickTimer()

        // Rename to 06_timer_popup when fdroidserver#490 is fixed
        Screengrab.screenshot("06a_sleep_timer_details")

        clickSettingsFromModalDialog()

        Screengrab.screenshot("08_settings")

        Espresso.pressBack()
        activityTestRule.launchActivity(
            Intent(
                InstrumentationRegistry.getInstrumentation().targetContext,
                MainActivity::class.java
            ).putExtra(MainActivity.EXTRA_SLEEP_TIME, true)
        )

        Screengrab.screenshot("07_sleep_time")

    }

    private fun clickTimer() {
        val timer = onView(withId(R.id.timer_wrapper))

        timer.perform(click())
    }

    private fun clickSettingsFromModalDialog() {
        val neutralButton = onView(withId(android.R.id.button3))

        neutralButton.perform(click())
    }

    private fun clickFloatingActionButton() {
        val floatingActionButton = onView(withId(R.id.sd_main_fab))

        floatingActionButton.perform(click())
    }

    private fun clickDots() {
        val dots = onView(withId(R.id.dots))

        // Don't use a default click(), that picks the centre of the view.
        // On some emulators in some languages, the menu items in the speed dial menu overlap with
        // this area, but we typically want to click the dots to REMOVE the menu item (due to
        // esspresso issues clicking the FAB again to remove the menu). As such, lets click
        // near the top left so as to try to ensure we actually hide the menu.
        dots.perform(clickXY(10, 10))
    }

    private fun clickSpeedDialButton(@IdRes id: Int) {
        val speedDialButton = onView(
            allOf(
                withId(R.id.sd_fab),
                withParent(withId(id)),
            )
        )

        speedDialButton.perform(click())
    }

    // https://stackoverflow.com/a/22798043/2391921
    fun clickXY(x: Int, y: Int): ViewAction? {
        return GeneralClickAction(
            Tap.SINGLE,
            { view ->
                val screenPos = IntArray(2)
                view.getLocationOnScreen(screenPos)
                val screenX = screenPos[0] + x.toFloat()
                val screenY = screenPos[1] + y.toFloat()
                floatArrayOf(screenX, screenY)
            },
            Press.FINGER
        )
    }
}
