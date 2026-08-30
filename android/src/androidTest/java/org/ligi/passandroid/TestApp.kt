package org.ligi.passandroid

import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.ligi.passandroid.injections.FixedPassListPassStore
import org.ligi.passandroid.model.PassStore
import org.ligi.passandroid.model.pass.BarCode
import org.ligi.passandroid.model.pass.Pass
import org.ligi.passandroid.model.pass.PassBarCodeFormat
import org.ligi.passandroid.model.pass.PassImpl
import org.ligi.passandroid.platform.AndroidPlatformActions
import org.ligi.passandroid.platform.PlatformActions
import org.ligi.passandroid.repository.DataStoreSettingsRepository
import org.ligi.passandroid.repository.FilePassRepository
import org.ligi.passandroid.repository.PassRepository
import org.ligi.passandroid.repository.SettingsRepository
import org.ligi.passandroid.ui.state.MainViewModel
import org.mockito.Mockito.mock
import java.util.*

class TestApp : App() {

    override fun createKoin(): Module {

        return module {
            single { passStore as PassStore }
            single { tracker }
            single<PassRepository> { FilePassRepository(this@TestApp, get(), get()) }
            single<SettingsRepository> { DataStoreSettingsRepository(this@TestApp) }
            single<PlatformActions> { AndroidPlatformActions(this@TestApp) }
            viewModel { MainViewModel(get(), get(), get()) }
        }
    }

    companion object {

        val tracker = mock(Tracker::class.java)
        val passStore = FixedPassListPassStore(
            listOf(PassImpl("recovery-pass").apply { description = "Recovery pass" }),
        )
        fun populatePassStoreWithSinglePass() {

            val passList = ArrayList<Pass>()
            val pass = PassImpl(UUID.randomUUID().toString())
            pass.description = "description"
            pass.barCode = BarCode(PassBarCodeFormat.AZTEC, "messageprobe")
            passList.add(pass)

            fixedPassListPassStore().setList(passList)

            passStore.classifier.moveToTopic(pass, "test")
        }

        fun emptyPassStore() {
            fixedPassListPassStore().setList(emptyList())
        }

        private fun fixedPassListPassStore() = passStore
    }
}
