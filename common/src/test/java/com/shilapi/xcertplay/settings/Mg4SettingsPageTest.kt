package com.shilapi.xcertplay.settings

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.shilapi.xcertplay.AirPlayPersistence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "w1920dp-h720dp-land-mdpi")
class Mg4SettingsPageTest {
    private lateinit var activity: Activity
    private lateinit var page: Mg4SettingsPage
    private var running = false
    private var reconnects = 0

    private val host = object : Mg4SettingsPage.Host {
        override val sessionRunning get() = running
        override val hotspotSetupPending = false
        override fun render() = activity.setContentView(page.build())
        override fun goHome() {}
        override fun hotspotOff() = false
        override fun choosePhone() {}
        override fun editHotspotDetails(onSaved: () -> Unit) {}
        override fun openCarHotspotSettings() {}
        override fun openCarWifiSettings() {}
        override fun editNavigationChannel(onSaved: () -> Unit) {}
        override fun setLocationReporting(enabled: Boolean) {}
        override fun checkForUpdates() {}
        override fun openAppPermissions() {}
        override fun openBluetoothSettings() {}
        override fun saveReport() {}
        override fun downloadReport() {}
        override fun chooseReportLocation() {}
        override fun bindReportControls(save: TextView, download: TextView, downloadHint: TextView) {}
        override fun wirelessHelp() {}
        override fun reconnect() { reconnects++ }
        override fun version() = "test"
    }

    @Before fun setUp() {
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        page = Mg4SettingsPage(activity, host)
        host.render()
    }

    private fun texts(view: View = activity.window.decorView): List<String> = when (view) {
        is TextView -> listOf(view.text.toString())
        is ViewGroup -> (0 until view.childCount).flatMap { texts(view.getChildAt(it)) }
        else -> emptyList()
    }

    private fun find(text: String, view: View = activity.window.decorView): TextView? = when (view) {
        is TextView -> view.takeIf { it.text.toString() == text }
        is ViewGroup -> (0 until view.childCount).firstNotNullOfOrNull { find(text, view.getChildAt(it)) }
        else -> null
    }

    /** Clicks the row holding [text], as a driver tapping it would. */
    private fun tap(text: String) {
        var view: View? = find(text)
        assertNotNull("no \"$text\"", view)
        while (view != null && !view.hasOnClickListeners()) view = view.parent as? View
        assertNotNull("\"$text\" isn't tappable", view)
        view!!.performClick()
    }

    @Test fun everyCategoryAndPageBuilds() {
        SettingsCategory.entries.forEach { category ->
            page.open(category)
            assertTrue(category.name, texts().size > 5)
        }
        page.open(SettingsCategory.DISPLAY)
        tap("Advanced")
        assertTrue("Overlay video renderer" in texts())
        assertTrue(page.back())
        assertTrue("CarPlay screen".uppercase() in texts())
    }

    @Test fun pickerSavesAndGoesBack() {
        AirPlayPersistence.saveVideoColourRange(activity, 0)
        page.open(SettingsCategory.DISPLAY)
        tap("Advanced")
        tap("Colour range (test)")
        tap("Full (PC range)")
        assertEquals(2, AirPlayPersistence.loadVideoColourRange(activity))
        assertTrue("Overlay video renderer" in texts())
    }

    @Test fun segmentedControlSaves() {
        AirPlayPersistence.saveFps(activity, 30)
        page.open(SettingsCategory.DISPLAY)
        tap("60 fps")
        assertEquals(60, AirPlayPersistence.loadFps(activity))
    }

    @Test fun changeWhileConnectedOffersReconnect() {
        running = true
        AirPlayPersistence.saveFps(activity, 30)
        page.open(SettingsCategory.DISPLAY)
        val bar = find("Reconnect now")!!.parent as View
        assertEquals(View.GONE, bar.visibility)
        tap("60 fps")
        assertEquals(View.VISIBLE, bar.visibility)
        tap("Reconnect now")
        assertEquals(1, reconnects)
        assertEquals(View.GONE, bar.visibility)
    }

    @Test fun changeWhileDisconnectedJustSaves() {
        running = false
        AirPlayPersistence.saveFps(activity, 30)
        page.open(SettingsCategory.DISPLAY)
        tap("60 fps")
        assertEquals(View.GONE, (find("Reconnect now")!!.parent as View).visibility)
    }

    @Test fun backLeavesSettingsFromTheTopLevel() {
        page.open(SettingsCategory.SOUND)
        assertFalse(page.back())
    }

    @Test @Config(qualifiers = "w600dp-h720dp-port-mdpi") fun narrowScreenDrillsDown() {
        page.open(SettingsCategory.SOUND)
        assertFalse("Connection" in texts())
        assertTrue(page.back())
        assertTrue("Connection" in texts())
        assertFalse(page.back())
    }
}
