package me.cyril.cyriltools.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.View
import android.widget.TextView
import me.cyril.cyriltools.NO_MENU
import me.cyril.cyriltools.R
import me.cyril.cyriltools.base.BaseFragment
import kotlin.math.*

class LevelFragment : BaseFragment() {
    override val resLayout: Int
        get() = R.layout.fragment_level
    override val resMenu: Int
        get() = NO_MENU

    private val mSensorManager by lazy { mActivity?.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    private val mListener = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

        }

        @Suppress("SetTextI18n")
        override fun onSensorChanged(event: SensorEvent?) {
            if (event?.sensor?.type == Sensor.TYPE_GRAVITY) {
                with(event.values) {
                    g = sqrt(this[0].pow(2) + this[1].pow(2) + this[2].pow(2))
                    xNormalized = this[0] / g
                    yNormalized = this[1] / g
                    zNormalized = this[2] / g
                }
                gamma = acos(zNormalized)
                phi = acos(xNormalized / sin(gamma))
                tvLevel?.text = "x: %.4f\ny: %.4f\nz: %.4f\nφ: %.4f\nslope: %.4f".format(
                    xNormalized,
                    yNormalized,
                    zNormalized,
                    Math.toDegrees(phi.toDouble()),
                    tan(gamma)
                )
            }
        }

    }
    private val mGravity by lazy { mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY) }

    private val tvLevel by bindView<TextView>(R.id.level)

    private var xNormalized = 0f
    private var yNormalized = 0f
    private var zNormalized = 0f
    private var g = 0f
    private var phi = 0f
    private var gamma = 0f

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mSensorManager.registerListener(mListener, mGravity, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        mSensorManager.unregisterListener(mListener)
    }
}