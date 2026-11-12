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

/**
 * Renders this edition's camera screen on the JVM and saves PNGs to app/build/screens/.
 * There is no camera here, so the viewfinder area is empty; everything around it is the real UI.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class ScreenshotTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun cameraScreen() {
        FilmRenderer.headless = true
        Engine.v4 = BuildConfig.V4
        val vm = CamViewModel(ApplicationProvider.getApplicationContext<Application>())
        rule.setContent { EditionRoot(vm) }
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("build/screens/${BuildConfig.FLAVOR}-camera.png")
    }
}
