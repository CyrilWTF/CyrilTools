package me.cyril.cyriltools.component

import android.app.*
import android.content.*
import android.graphics.*
import android.graphics.drawable.Icon
import android.net.*
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.preference.PreferenceManager
import me.cyril.cyriltools.*
import me.cyril.cyriltools.ui.MainActivity

class SpeedIndicatorService : Service() {

    private val mListener = SharedPreferences.OnSharedPreferenceChangeListener { sp, key ->
        when (key) {
            getString(R.string.network_indicator_key) -> mSource[0] =
                if (sp.getBoolean(key, false)) NetSpeedMonitor() else null
            getString(R.string.network_indicator_update_interval_key) -> frequency =
                sp.getInt(key, 2)
        }
    }

    private val mReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    for (s in mSource) {
                        s?.free()
                    }
                    stop()
                }
                Intent.ACTION_USER_PRESENT -> {
                    resume()
                }
            }
        }

    }

    private val mNotificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    private val mBuilder by lazy {
        Notification.Builder(
            this,
            NOTIFICATION_CHANNEL_ID
        )
    }

    private val mHandler = Looper.myLooper()?.let { Handler(it) }
    private val mRunnable = object : Runnable {
        override fun run() {
            for (s in mSource) {
                s?.update()
            }
            mNotificationManager.notify(
                NOTIFICATION_ID,
                mBuilder.setContentText(mSource[0]?.text)
                    .setSmallIcon(Icon.createWithBitmap(mSource[0]?.icon)).build()
            )
            mHandler?.postDelayed(this, 1000L * frequency)
        }
    }


    private val mPaint = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
        textSize = 36f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    private val mSource = arrayOfNulls<NotificationSource>(1)

    private var frequency = 2
    private var isNotifying = false

    override fun onCreate() {
        super.onCreate()

        with(PreferenceManager.getDefaultSharedPreferences(this)) {
            frequency = getInt(getString(R.string.network_indicator_update_interval_key), 2)
            registerOnSharedPreferenceChangeListener(mListener)
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(mReceiver, filter)
        resume()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
        }
        mNotificationManager.createNotificationChannel(channel)
        val pi = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), 0)
        startForeground(
            NOTIFICATION_ID, mBuilder.setVisibility(Notification.VISIBILITY_SECRET)
                .setContentIntent(pi)
                .setContentTitle("Network Speed")
                .setSmallIcon(R.drawable.ic_doze).build()
        )
        return START_REDELIVER_INTENT
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()

        for (s in mSource) {
            s?.free()
        }
        stop()
        unregisterReceiver(mReceiver)
        PreferenceManager.getDefaultSharedPreferences(this)
            .unregisterOnSharedPreferenceChangeListener(mListener)
    }

    private fun stop() {
        mHandler?.removeCallbacks(mRunnable)
        isNotifying = false
    }

    private fun resume() {
        for (s in mSource) {
            s?.init()
        }
    }

    private inner class NetSpeedMonitor : NotificationSource {

        private val mConnectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        private val mNetworkCallback = object : ConnectivityManager.NetworkCallback() {

            override fun onAvailable(network: Network) {
                mNetworkMap[network] = mConnectivityManager.getLinkProperties(network)
                if (!isNotifying) {
                    mHandler?.post(mRunnable)
                    isNotifying = true
                }
            }

            override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
                if (mNetworkMap.containsKey(network)) {
                    mNetworkMap[network] = linkProperties
                }
            }

            override fun onLost(network: Network) {
                mNetworkMap.remove(network)
                if (mNetworkMap.isEmpty()) {
                    stop()
                    mNotificationManager.notify(
                        NOTIFICATION_ID,
                        mBuilder.setSmallIcon(R.drawable.ic_doze)
                            .setContentText("No network available")
                            .build()
                    )
                }
            }

        }
        private val units = arrayOf("KB", "MB", "GB")
        private val mNetworkMap = HashMap<Network, LinkProperties?>()
        private val mRequest =
            NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN).build()

        private var lastRx = 0L
        private var lastTx = 0L
        private var lastTime = System.currentTimeMillis()

        override var title = ""
        override var text = ""

        override fun init() {
            mConnectivityManager.registerNetworkCallback(mRequest, mNetworkCallback)

            lastRx = 0L
            lastTx = 0L
            for (lp in mNetworkMap.values) {
                lp?.interfaceName?.let {
                    lastRx += Reflector.GetRxBytes(it)
                    lastTx += Reflector.GetTxBytes(it)
                }
            }
        }

        override fun update() {
            var nowRx = 0L
            var nowTx = 0L

            for (lp in mNetworkMap.values) {
                lp?.interfaceName?.let {
                    nowRx += Reflector.GetRxBytes(it)
                    nowTx += Reflector.GetTxBytes(it)
                }
            }

            val nowTime = System.currentTimeMillis()
            val interval = nowTime - lastTime
            lastTime = nowTime

            val speedRx = generate((nowRx - lastRx) * 1000 / interval)
            val speedTx = generate((nowTx - lastTx) * 1000 / interval)

            lastRx = nowRx
            lastTx = nowTx

            text = "${speedRx.first} ${speedRx.second} ↓ | ${speedTx.first} ${speedTx.second} ↑"

            with(canvas) {
                drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                drawText(speedTx.first + speedTx.second[0], 48f, 36f, mPaint)
                drawText(speedRx.first + speedRx.second[0], 48f, 84f, mPaint)
            }
        }

        override fun free() = mConnectivityManager.unregisterNetworkCallback(mNetworkCallback)

        private fun generate(speed: Long): Pair<String, String> {
            if (speed < 1000) {
                return Pair(speed.toString(), "B")
            }

            var speedF = speed / 1024f
            for (i in 0 until 3) {
                if (speedF < 1000) {
                    return Pair(String.format("%4f", speedF), units[i])
                }
                speedF /= 1024f
            }

            return Pair("N/A", " ")
        }

    }

    private interface NotificationSource {

        val icon: Bitmap
            get() = Bitmap.createBitmap(96, 96, Bitmap.Config.ALPHA_8)
        val canvas: Canvas
            get() = Canvas(icon)

        var title: String
        var text: String

        fun init()
        fun update()
        fun free()

    }

}