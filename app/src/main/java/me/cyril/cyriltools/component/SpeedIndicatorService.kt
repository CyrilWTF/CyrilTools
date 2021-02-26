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
        if (key == UPDATE_FREQUENCY_KEY) {
            frequency = sp.getInt(key, 2)
        }
    }

    private val mReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    mConnectivityManager.unregisterNetworkCallback(mNetworkCallback)
                    stop()
                }
                Intent.ACTION_USER_PRESENT -> {
                    resume()
                }
            }
        }

    }

    private val mConnectivityManager by lazy { getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager }
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
                    mBuilder.setSmallIcon(R.drawable.ic_doze).setContentText("No network available")
                        .build()
                )
            }
        }

    }
    private val units = arrayOf("B", "KB", "MB")
    private val mNetworkMap = HashMap<Network, LinkProperties?>()
    private val mRequest by lazy {
        NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN).build()
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
            update()
            mNotificationManager.notify(
                NOTIFICATION_ID,
                mBuilder.setContentText(speed).build()
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
    private val mIcon by lazy { Bitmap.createBitmap(96, 96, Bitmap.Config.ALPHA_8) }
    private val mCanvas by lazy { Canvas(mIcon) }

    private var speed = ""
    private var frequency = 2
    private var isNotifying = false

    private var lastRx = 0L
    private var lastTx = 0L
    private var lastTime = 0L

    private lateinit var speedRx: String
    private lateinit var speedTx: String
    private lateinit var unitRx: String
    private lateinit var unitTx: String

    override fun onCreate() {
        super.onCreate()

        with(PreferenceManager.getDefaultSharedPreferences(this)) {
            frequency = getInt(UPDATE_FREQUENCY_KEY, 2)
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
        stop()
        mConnectivityManager.unregisterNetworkCallback(mNetworkCallback)
        unregisterReceiver(mReceiver)
        PreferenceManager.getDefaultSharedPreferences(this)
            .unregisterOnSharedPreferenceChangeListener(mListener)
    }

    private fun stop() {
        mHandler?.removeCallbacks(mRunnable)
        isNotifying = false
    }

    private fun resume() {
        mConnectivityManager.registerNetworkCallback(mRequest, mNetworkCallback)
        lastRx = 0L
        lastTx = 0L
        lastTime = System.currentTimeMillis()
    }

    private fun update() {
        var nowRx = 0L
        var nowTx = 0L
        val nowTime = System.currentTimeMillis()
        val interval = nowTime - lastTime

        for (lp in mNetworkMap.values) {
            lp?.interfaceName?.let {
                nowRx += Reflector.GetRxBytes(it)
                nowTx += Reflector.GetTxBytes(it)
            }
        }

        generate((nowRx - lastRx) * 1000f / interval, 'r')
        generate((nowTx - lastTx) * 1000f / interval, 't')

        speed = "$speedTx $unitTx ↑ | $speedRx $unitRx ↓"
        with(mCanvas) {
            drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            drawText(speedTx + unitTx[0], 48f, 36f, mPaint)
            drawText(speedRx + unitRx[0], 48f, 84f, mPaint)
        }
        mBuilder.setSmallIcon(Icon.createWithBitmap(mIcon))

        lastRx = nowRx
        lastTx = nowTx
        lastTime = nowTime
    }

    private fun generate(speed: Float, type: Char) {
        var unit = "GB"
        var formatted = ""
        var sp = speed
        for (i in 0 until 3) {
            if (sp < 1000) {
                formatted = if (sp < 100 && i != 0) {
                    String.format("%.1f", sp)
                } else {
                    String.format("%.0f", sp)
                }
                unit = units[i]
                break
            }
            sp /= 1000
        }

        when (type) {
            'r' -> {
                speedRx = formatted
                unitRx = unit
            }
            't' -> {
                speedTx = formatted
                unitTx = unit
            }
        }
    }

}