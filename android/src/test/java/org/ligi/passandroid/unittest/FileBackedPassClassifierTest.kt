package org.ligi.passandroid.unittest

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ligi.passandroid.model.FileBackedPassClassifier
import org.ligi.passandroid.model.PassStore
import org.ligi.passandroid.model.createPassMoshi
import org.ligi.passandroid.model.pass.PassImpl
import org.mockito.Mockito.mock
import java.nio.file.Files

class FileBackedPassClassifierTest {
    private val passStore = mock(PassStore::class.java)
    private val moshi = createPassMoshi()

    @Test
    fun `loads existing classifications`() {
        val file = temporaryFile("{\"pass-1\":\"Travel\"}")

        val classifier = FileBackedPassClassifier(file, passStore, moshi)

        assertThat(classifier.topicByIdMap).containsEntry("pass-1", "Travel")
    }

    @Test
    fun `persists classification changes`() {
        val file = temporaryFile("{}")
        val classifier = FileBackedPassClassifier(file, passStore, moshi)

        classifier.moveToTopic(PassImpl("pass-2"), "Events")

        assertThat(FileBackedPassClassifier(file, passStore, moshi).topicByIdMap)
            .containsEntry("pass-2", "Events")
    }

    @Test
    fun `recovers from a corrupt classification file`() {
        val file = temporaryFile("not-json")

        val classifier = FileBackedPassClassifier(file, passStore, moshi)

        assertThat(classifier.topicByIdMap).isEmpty()
    }

    private fun temporaryFile(content: String) =
        Files.createTempFile("pass-classifications", ".json").toFile().apply { writeText(content) }
}
