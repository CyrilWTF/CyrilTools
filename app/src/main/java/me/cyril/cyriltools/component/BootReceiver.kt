package me.cyril.cyriltools.component

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.preference.PreferenceManager
import me.cyril.cyriltools.R

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val sp = PreferenceManager.getDefaultSharedPreferences(context)
            if (sp.getBoolean(context.getString(R.string.notification_key), false)) {
                context.startForegroundService(Intent(context, SpeedIndicatorService::class.java))
            }
        }
    }
}
