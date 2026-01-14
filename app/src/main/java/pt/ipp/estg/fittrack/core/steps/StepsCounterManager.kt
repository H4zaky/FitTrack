package pt.ipp.estg.fittrack.core.steps

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class StepCounterManager(
    context: Context,
    private val onStepsDelta: (Int) -> Unit
) : SensorEventListener {

    private val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepSensor: Sensor? = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    private var running = false
    private var base: Float? = null
    private var lastDelta = 0

    fun start() {
        if (running) return
        running = true
        base = null
        lastDelta = 0
        stepSensor?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
    }

    fun stop() {
        if (!running) return
        running = false
        sm.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!running) return
        val total = event.values.firstOrNull() ?: return
        if (base == null) base = total

        val delta = (total - (base ?: total)).toInt().coerceAtLeast(0)
        if (delta != lastDelta) {
            lastDelta = delta
            onStepsDelta(delta)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
