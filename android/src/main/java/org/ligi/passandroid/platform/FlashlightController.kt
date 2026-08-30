package org.ligi.passandroid.platform

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class FlashlightState(
    val isAvailable: Boolean = false,
    val isEnabled: Boolean = false,
)

interface FlashlightController : AutoCloseable {
    val state: StateFlow<FlashlightState>
    fun setEnabled(enabled: Boolean)
}

class AndroidFlashlightController(
    context: Context,
    private val cameraManager: CameraManager = requireNotNull(context.getSystemService(CameraManager::class.java)),
) : FlashlightController {
    private val cameraId = findFlashCameraId(cameraManager)
    private val mutableState = MutableStateFlow(FlashlightState(isAvailable = cameraId != null))
    override val state: StateFlow<FlashlightState> = mutableState.asStateFlow()

    private val callback = object : CameraManager.TorchCallback() {
        override fun onTorchModeUnavailable(id: String) {
            if (id == cameraId) mutableState.value = FlashlightState()
        }

        override fun onTorchModeChanged(id: String, enabled: Boolean) {
            if (id == cameraId) mutableState.value = FlashlightState(isAvailable = true, isEnabled = enabled)
        }
    }

    private val callbackRegistered = try {
        cameraManager.registerTorchCallback(ContextCompat.getMainExecutor(context), callback)
        true
    } catch (_: IllegalArgumentException) {
        mutableState.value = FlashlightState()
        false
    } catch (_: SecurityException) {
        mutableState.value = FlashlightState()
        false
    }

    override fun setEnabled(enabled: Boolean) {
        val id = cameraId ?: return
        if (!enabled || mutableState.value.isAvailable) {
            try {
                cameraManager.setTorchMode(id, enabled)
                mutableState.value = mutableState.value.copy(isEnabled = enabled)
            } catch (_: CameraAccessException) {
                mutableState.value = FlashlightState()
            } catch (_: IllegalArgumentException) {
                mutableState.value = FlashlightState()
            } catch (_: SecurityException) {
                mutableState.value = FlashlightState()
            }
        }
    }

    override fun close() {
        setEnabled(false)
        if (callbackRegistered) cameraManager.unregisterTorchCallback(callback)
    }
}

private fun findFlashCameraId(cameraManager: CameraManager): String? {
    return try {
        cameraManager.cameraIdList
            .mapNotNull { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                if (characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) != true) return@mapNotNull null
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                id to if (facing == CameraCharacteristics.LENS_FACING_BACK) 0 else 1
            }
            .minByOrNull { (_, priority) -> priority }
            ?.first
    } catch (_: CameraAccessException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    } catch (_: SecurityException) {
        null
    }
}
