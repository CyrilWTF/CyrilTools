package me.cyril.cyriltools.component

import android.content.ComponentName
import android.content.Context
import android.os.PowerManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class ActiveDisplayService : NotificationListenerService() {

    companion object {
        private const val TAG = "ActiveDisplayService:wakelock"
    }

    private val mPowerManager by lazy { getSystemService(Context.POWER_SERVICE) as PowerManager }

    override fun onListenerDisconnected() {
        requestRebind(ComponentName(applicationContext, ActiveDisplayService::class.java))
    }

    @Suppress("DEPRECATION")
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn?.also {
            if (!it.isOngoing and !mPowerManager.isInteractive) {
                val wl = mPowerManager.newWakeLock(
                    PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.SCREEN_DIM_WAKE_LOCK,
                    TAG
                )
                wl.acquire(5000)
            }
        }
    }

}
