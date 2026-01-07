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
    private val sensor: Sensor? = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    private var baseline: Float? = null

    fun start() {
        baseline = null
        sensor?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
    }

    fun stop() {
        sm.unregisterListener(this)
        baseline = null
    }

    override fun onSensorChanged(event: SensorEvent) {
        val total = event.values.firstOrNull() ?: return
        val base = baseline
        if (base == null) {
            baseline = total
            onStepsDelta(0)
            return
        }
        val delta = (total - base).toInt().coerceAtLeast(0)
        onStepsDelta(delta)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
