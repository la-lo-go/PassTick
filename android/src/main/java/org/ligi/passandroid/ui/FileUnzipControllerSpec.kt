package org.ligi.passandroid.repository.io

class FileUnzipControllerSpec(
    val zipFileString: String,
    val source: String,
    spec: UnzipControllerSpec,
) : UnzipControllerSpec(spec.targetPath, spec.context, spec.passStore, spec.onSuccessCallback, spec.failCallback) {

    constructor(zipFileString: String, spec: UnzipPassController.InputStreamUnzipControllerSpec) :
        this(zipFileString, spec.inputStreamWithSource.source, spec)

    init {
        overwrite = spec.overwrite
    }
}
