package org.ligi.passandroid.repository

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.ligi.passandroid.R
import org.ligi.passandroid.Tracker
import org.ligi.passandroid.functions.fromURI
import org.ligi.passandroid.model.PassStore
import org.ligi.passandroid.model.pass.Pass
import org.ligi.passandroid.ui.PassExporter
import org.ligi.passandroid.ui.UnzipPassController
import java.io.File

interface PassRepository {
    fun observePasses(): Flow<List<Pass>>

    fun find(id: String): Pass?

    suspend fun import(uri: Uri): Result<Pass>

    suspend fun save(pass: Pass)

    suspend fun delete(id: String): Boolean

    suspend fun export(id: String, destination: Uri): Result<Unit>
}

class FilePassRepository(
    private val context: Context,
    private val passStore: PassStore,
    private val tracker: Tracker,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : PassRepository {
    override fun observePasses(): Flow<List<Pass>> = flow {
        passStore.syncPassStoreWithClassifier(context.getString(R.string.topic_new))
        emit(snapshot())
        emitAll(passStore.updates.map { snapshot() })
    }

    override fun find(id: String) = passStore.getPassbookForId(id)

    override suspend fun import(uri: Uri): Result<Pass> = withContext(ioDispatcher) {
        runCatching {
            val source = requireNotNull(fromURI(context, uri, tracker)) { "Cannot open the selected file" }
            var importedId: String? = null
            var failure: String? = null
            val spec = UnzipPassController.InputStreamUnzipControllerSpec(
                source,
                context,
                passStore,
                object : UnzipPassController.SuccessCallback {
                    override fun call(uuid: String) {
                        importedId = uuid
                    }
                },
                object : UnzipPassController.FailCallback {
                    override fun fail(reason: String) {
                        failure = reason
                    }
                },
            )
            UnzipPassController.processInputStream(spec)
            failure?.let { error(it) }
            val pass = requireNotNull(importedId?.let(passStore::getPassbookForId)) { "Imported pass is unreadable" }
            passStore.classifier.moveToTopic(pass, context.getString(R.string.topic_new))
            pass
        }
    }

    override suspend fun save(pass: Pass) = withContext(ioDispatcher) {
        passStore.save(pass)
        passStore.notifyChange()
    }

    override suspend fun delete(id: String) = withContext(ioDispatcher) {
        passStore.deletePassWithId(id)
    }

    override suspend fun export(id: String, destination: Uri): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val target = File.createTempFile("pass-export-", ".espass", context.cacheDir)
            try {
                val exporter = PassExporter(passStore.getPathForID(id), target)
                exporter.export()
                exporter.exception?.let { throw it }
                context.contentResolver.openOutputStream(destination)?.use { output ->
                    target.inputStream().use { it.copyTo(output) }
                } ?: error("Cannot open the export destination")
                Unit
            } finally {
                target.delete()
            }
        }
    }

    private fun snapshot() = passStore.passMap.values.toList()
}
