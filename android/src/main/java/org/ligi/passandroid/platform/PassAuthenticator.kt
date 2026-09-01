package org.ligi.passandroid.platform

import android.app.Activity
import android.app.KeyguardManager
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal
import java.util.concurrent.atomic.AtomicBoolean

class PassAuthenticator(private val activity: Activity) {
    fun canAuthenticate(): Boolean =
        activity.getSystemService(KeyguardManager::class.java)?.isDeviceSecure == true

    fun authenticate(onResult: (Boolean) -> Unit) {
        val delivered = AtomicBoolean(false)
        fun finish(authenticated: Boolean) {
            if (delivered.compareAndSet(false, true)) onResult(authenticated)
        }

        BiometricPrompt.Builder(activity)
            .setTitle("Unlock protected pass")
            .setSubtitle("Use biometrics or your screen lock")
            .setDeviceCredentialAllowed(true)
            .build()
            .authenticate(
                CancellationSignal().apply { setOnCancelListener { finish(false) } },
                activity.mainExecutor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                        finish(true)
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                        finish(false)
                    }
                },
            )
    }
}
