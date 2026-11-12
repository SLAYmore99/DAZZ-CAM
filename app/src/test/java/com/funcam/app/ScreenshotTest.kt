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

    private fun vm(): CamViewModel {
        FilmRenderer.headless = true
        Engine.v4 = BuildConfig.V4
        return CamViewModel(ApplicationProvider.getApplicationContext<Application>())
    }

    private fun shoot(name: String, content: @androidx.compose.runtime.Composable () -> Unit) {
        rule.setContent(content)
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("build/screens/${BuildConfig.FLAVOR}-$name.png")
    }

    @Test fun cameraScreen() { val vm = vm(); shoot("camera") { EditionRoot(vm) } }
}
