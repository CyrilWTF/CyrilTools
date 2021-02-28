package me.cyril.cyriltools.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import me.cyril.cyriltools.R
import me.cyril.cyriltools.component.SpeedIndicatorService

class MiscFragment : PreferenceFragmentCompat() {

    private var mActivity: MainActivity? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mActivity = activity as? MainActivity
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.misc, rootKey)
    }

    override fun onResume() {
        super.onResume()
        findPreference<Preference>(getString(R.string.notification_listener_key))?.summary =
            if (isNotificationListenerEnabled()) "Enabled" else "Disabled"
    }

    override fun onDetach() {
        super.onDetach()
        mActivity = null
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        when (preference?.key) {
            getString(R.string.notification_key) -> {
                if ((preference as SwitchPreference).isChecked) {
                    mActivity?.startForegroundService(
                        Intent(
                            mActivity,
                            SpeedIndicatorService::class.java
                        )
                    )
                } else {
                    mActivity?.stopService(Intent(mActivity, SpeedIndicatorService::class.java))
                }
            }
        }
        return super.onPreferenceTreeClick(preference)
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val name = mActivity?.packageName
        val listeners =
            Settings.Secure.getString(mActivity?.contentResolver, "enabled_notification_listeners")
        val components = listeners?.split(":")
        components?.let {
            for (c in it) {
                val cn = ComponentName.unflattenFromString(c)
                if (cn?.packageName.equals(name)) {
                    return true
                }
            }
        }
        return false
    }

}