package org.ligi.passandroid

import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

class AppReplacingRunner : AndroidJUnitRunner() {
    override fun newApplication(classLoader: ClassLoader?, className: String?, context: Context?) =
        super.newApplication(classLoader, TestApp::class.java.name, context)
}
