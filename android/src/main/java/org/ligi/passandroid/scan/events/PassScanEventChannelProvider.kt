package org.ligi.passandroid.scan.events

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class PassScanEventChannelProvider {
    private val mutableEvents = MutableSharedFlow<PassScanEvent>(extraBufferCapacity = 1)
    val events = mutableEvents.asSharedFlow()

    suspend fun emit(event: PassScanEvent) {
        mutableEvents.emit(event)
    }
}
