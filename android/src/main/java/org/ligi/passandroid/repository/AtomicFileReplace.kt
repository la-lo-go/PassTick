package org.ligi.passandroid.repository

import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

private const val REPLACE_ATTEMPTS = 5
private const val REPLACE_RETRY_DELAY_MILLIS = 25L

/**
 * Replaces [target] with [source], atomically when the file system supports it.
 *
 * A transient lock on the target (a virus scanner or an indexer) makes the move fail with
 * AccessDeniedException, so a locked target is retried before the failure propagates.
 */
internal fun replaceFileAtomically(source: File, target: File) {
    repeat(REPLACE_ATTEMPTS) { attempt ->
        try {
            Files.move(
                source.toPath(),
                target.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
            return
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
            return
        } catch (error: Throwable) {
            if (attempt == REPLACE_ATTEMPTS - 1) throw error
            Thread.sleep(REPLACE_RETRY_DELAY_MILLIS * (attempt + 1))
        }
    }
}
