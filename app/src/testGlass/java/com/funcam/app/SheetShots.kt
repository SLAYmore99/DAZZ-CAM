package com.funcam.app

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.funcam.app.gl.FilmRenderer
import com.funcam.app.model.Engine
import com.funcam.app.ui.CamViewModel
import com.funcam.app.ui.EditionRoot
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Screenshots of this edition's panels and states (see ScreenshotTest for the main screen). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class SheetShots {
    @get:Rule val rule = createComposeRule()

    private fun vm(): CamViewModel {
        FilmRenderer.headless = true
        Engine.v4 = true
        return CamViewModel(ApplicationProvider.getApplicationContext<Application>())
    }

    private fun shoot(name: String, sheet: String?, setup: (CamViewModel) -> Unit = {}) {
        val vm = vm().also(setup)
        rule.setContent { EditionRoot(vm, sheet) }
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("build/screens/${BuildConfig.FLAVOR}-$name.png")
    }

    @Test fun cameras() = shoot("cams", "cams")
    @Test fun tune() = shoot("tune", "tune")
    @Test fun lens() = shoot("lens", "lens") { it.updateAcc(it.acc.copy(mist = true)) }
    @Test fun settings() = shoot("settings", "settings")
    @Test fun adjusted() = shoot("adjusted", null) { it.exposure = 1.3f; it.setManualValues(shutter = 0.25f); it.toggleFlash(); it.setHistogram(true) }
    @Test fun video() = shoot("video", null) { it.setVideoMode(true) }
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class TrayShot {
    @get:Rule val rule = createComposeRule()
    @Test fun tray() {
        FilmRenderer.headless = true; Engine.v4 = true
        val vm = CamViewModel(ApplicationProvider.getApplicationContext<Application>())
        rule.setContent { EditionRoot(vm, null, tray = true) }
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("build/screens/glass-tray.png")
    }
}
