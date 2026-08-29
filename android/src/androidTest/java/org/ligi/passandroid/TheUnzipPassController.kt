package org.ligi.passandroid

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.Test
import org.ligi.passandroid.model.InputStreamWithSource
import org.ligi.passandroid.model.PassStore
import org.ligi.passandroid.ui.UnzipPassController
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

class TheUnzipPassController {
    @Mock lateinit var failCallback: UnzipPassController.FailCallback
    @Mock lateinit var successCallback: UnzipPassController.SuccessCallback
    @Mock lateinit var passStore: PassStore

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun brokenPassFails() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val stream = instrumentation.context.assets.open("passes/broken/fail.pkpass")
        val spec = UnzipPassController.InputStreamUnzipControllerSpec(
            InputStreamWithSource("none", stream),
            instrumentation.targetContext,
            passStore,
            successCallback,
            failCallback,
        )

        UnzipPassController.processInputStream(spec)

        verify(successCallback, never()).call(anyString())
        verify(failCallback).fail(anyString())
    }
}
