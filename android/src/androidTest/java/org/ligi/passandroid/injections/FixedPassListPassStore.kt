package org.ligi.passandroid.injections

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.ligi.passandroid.model.PassClassifier
import org.ligi.passandroid.model.PassStore
import org.ligi.passandroid.model.PassStoreUpdateEvent
import org.ligi.passandroid.model.pass.Pass
import java.io.File

class FixedPassListPassStore(private var passes: List<Pass>) : PassStore {
    var pathForId: File = File("")

    override var classifier: PassClassifier = PassClassifier(HashMap(), this)

    fun setList(newPasses: List<Pass>, newCurrentPass: Pass? = newPasses.firstOrNull()) {
        currentPass = newCurrentPass
        passes = newPasses
        passMap.clear()
        passMap.putAll(createHashMap())

        classifier = PassClassifier(HashMap(), this)
    }

    override var currentPass: Pass? = null

    override val passMap: HashMap<String, Pass> by lazy {
        return@lazy createHashMap()
    }

    private fun createHashMap(): HashMap<String, Pass> {
        val hashMap = HashMap<String, Pass>()

        passes.forEach { hashMap[it.id] = it }
        return hashMap
    }

    override fun getPassbookForId(id: String): Pass? {
        return passMap[id]
    }


    override fun deletePassWithId(id: String): Boolean {
        return false
    }

    override fun getPathForID(id: String): File {
        return pathForId
    }

    private val mutableUpdates = MutableSharedFlow<PassStoreUpdateEvent>(extraBufferCapacity = 1)
    override val updates = mutableUpdates.asSharedFlow()

    override fun save(pass: Pass) = Unit

    override fun notifyChange() = Unit

    override fun syncPassStoreWithClassifier(defaultTopic: String) = Unit

}
