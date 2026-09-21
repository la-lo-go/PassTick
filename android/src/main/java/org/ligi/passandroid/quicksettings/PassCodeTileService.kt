package org.ligi.passandroid.quicksettings

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.ligi.passandroid.MainActivity
import org.ligi.passandroid.repository.DataStoreSettingsRepository
import org.ligi.passandroid.widget.PassWidgetSnapshotStore
import org.ligi.passandroid.widget.openPassIntent
import org.ligi.passandroid.widget.resolveCodePass
import java.time.Instant
import java.time.ZoneId

class PassCodeTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var resolvedPassId: String? = null

    override fun onStartListening() {
        super.onStartListening()
        scope.launch {
            val settings = DataStoreSettingsRepository(this@PassCodeTileService).settings.first()
            val snapshot = PassWidgetSnapshotStore(this@PassCodeTileService).read()
            val pass = resolveCodePass(
                passes = snapshot.passes,
                codePassId = settings.codePassId,
                sortOrder = settings.sortOrder,
                passOrder = settings.passOrder,
                now = Instant.now(),
                zoneId = ZoneId.systemDefault(),
            )
            resolvedPassId = pass?.id
            qsTile?.state = if (pass == null) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
            qsTile?.updateTile()
        }
    }

    // The Intent overload throws on API 34 and later; the PendingIntent overload does not exist below 34.
    @SuppressLint("StartActivityAndCollapseDeprecated")
    override fun onClick() {
        super.onClick()
        val intent = resolvedPassId?.let { openPassIntent(this, it, showCode = true) }
            ?: Intent(this, MainActivity::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(
                PendingIntent.getActivity(
                    this,
                    0,
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}