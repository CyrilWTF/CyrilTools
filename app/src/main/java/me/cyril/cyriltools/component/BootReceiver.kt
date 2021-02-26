package me.cyril.cyriltools.component

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.preference.PreferenceManager
import me.cyril.cyriltools.INDICATOR_SWITCH_KEY

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val sp = PreferenceManager.getDefaultSharedPreferences(context)
            if (sp.getBoolean(INDICATOR_SWITCH_KEY, false)) {
                context.startForegroundService(Intent(context, SpeedIndicatorService::class.java))
            }
        }
    }
}
