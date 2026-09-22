package org.ligi.passandroid.model

import android.content.Context
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okio.buffer
import okio.sink
import okio.source
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.ligi.passandroid.BuildConfig
import org.ligi.passandroid.Tracker
import org.ligi.passandroid.functions.safePassIdOrNull
import org.ligi.passandroid.model.pass.Pass
import org.ligi.passandroid.model.pass.PassImpl
import org.ligi.passandroid.reader.AppleStylePassReader
import org.ligi.passandroid.reader.PassReader
import java.io.File
import java.util.*

object PassStoreUpdateEvent

class AndroidFileSystemPassStore(
        private val context: Context,
        private val moshi: Moshi
) : PassStore, KoinComponent {

    private val mutableUpdates = MutableSharedFlow<PassStoreUpdateEvent>(extraBufferCapacity = 1)
    override val updates = mutableUpdates.asSharedFlow()

    private val path = File(context.filesDir, "passes")

    override val passMap = HashMap<String, Pass>()

    override var currentPass: Pass? = null

    private val tracker: Tracker by inject()

    override val classifier: PassClassifier by lazy {
        val classificationFile = File(context.filesDir, "state/classifier_state.json")
        FileBackedPassClassifier(classificationFile, this, moshi)
    }

    override fun save(pass: Pass) {
        val jsonAdapter = moshi.adapter(PassImpl::class.java)

        val pathForID = getPathForID(pass.id)

        if (!pathForID.exists()) {
            pathForID.mkdirs()
        }

        val buffer = File(pathForID, "main.json").sink().buffer()

        if (BuildConfig.DEBUG) {
            val of = com.squareup.moshi.JsonWriter.of(buffer)
            of.indent = "  "
            jsonAdapter.toJson(of, pass as PassImpl)
            buffer.close()
            of.close()
        } else {
            jsonAdapter.toJson(buffer, pass as PassImpl)
            buffer.close()
        }

        passMap[pass.id] = pass
    }

    private fun readPass(id: String): Pass? {
        val pathForID = getPathForID(id)
        val language = context.resources.configuration.locale.language

        if (!pathForID.exists() || !pathForID.isDirectory) {
            return null
        }

        val file = File(pathForID, "main.json")
        var result: Pass? = null
        var dirty = true
        if (file.exists()) {
            val jsonAdapter = moshi.adapter(PassImpl::class.java)
            dirty = false
            try {
                result = jsonAdapter.fromJson(file.source().buffer())
            } catch (ignored: JsonDataException) {
                tracker.trackException("invalid main.json", false)
            }
        }

        if (result == null && File(pathForID, "data.json").exists()) {
            result = PassReader.read(pathForID)
            File(pathForID, "data.json").delete()
        }

        if (result == null && File(pathForID, "pass.json").exists()) {
            result = AppleStylePassReader.read(pathForID, language, context, tracker)
        }

        if (result != null) {
            val corrected = (result as? PassImpl)?.let { ApplePassbookQuirkCorrector(tracker).correctQuirks(it) } == true
            if (dirty || corrected) {
                save(result)
            }
            passMap[id] = result
            notifyChange()
        }

        return result
    }

    override fun getPassbookForId(id: String): Pass? {
        return passMap[id] ?: readPass(id)
    }

    override fun deletePassWithId(id: String): Boolean {
        val result = getPathForID(id).deleteRecursively()
        if (result) {
            passMap.remove(id)
            classifier.removePass(id)
            notifyChange()
        }
        return result
    }

    override fun getPathForID(id: String): File {
        val safeId = safePassIdOrNull(id) ?: throw IllegalArgumentException("Unsafe pass id: $id")
        return File(path, safeId)
    }

    override fun getPassDirectories(): List<File> =
        path.listFiles().orEmpty().filter { it.isDirectory && safePassIdOrNull(it.name) != null }

    override fun notifyChange() {
        mutableUpdates.tryEmit(PassStoreUpdateEvent)
    }

    override fun syncPassStoreWithClassifier(defaultTopic: String) {
        val keysToRemove = classifier.topicByIdMap.keys.filter { getPassbookForId(it) == null }

        for (key in keysToRemove) {
            classifier.topicByIdMap.remove(key)
        }

        val allPasses = path.listFiles()
        allPasses?.forEach {
            classifier.getTopic(it.name, defaultTopic)
        }
    }
}
