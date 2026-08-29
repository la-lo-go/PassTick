package org.ligi.passandroid.repository.io

import android.content.Context
import org.ligi.passandroid.model.PassStore
import org.ligi.passandroid.repository.io.UnzipPassController.FailCallback
import org.ligi.passandroid.repository.io.UnzipPassController.SuccessCallback
import java.io.File

open class UnzipControllerSpec(var targetPath: File,
                               val context: Context,
                               val passStore: PassStore,
                               val onSuccessCallback: SuccessCallback?,
                               val failCallback: FailCallback?) {
    var overwrite = false

    constructor(context: Context, passStore: PassStore, onSuccessCallback: SuccessCallback?, failCallback: FailCallback?)
            : this(File(context.filesDir, "passes"), context, passStore, onSuccessCallback, failCallback)

}
