package com.example.ui.physics

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

data class CosmicZeroGParticle(
    val id: Int,
    val x: Float, // 0f..1f
    val y: Float, // 0f..1f
    val radius: Float,
    val alpha: Float,
    val colorHex: String
)

data class LayerPhysicsOffset(
    val deltaX: Float = 0f,
    val deltaY: Float = 0f,
    val deltaRotation: Float = 0f
)

/**
 * Real-time Anti-Gravity Zero-G Physics Simulation Engine.
 * Simulates weightless floating, harmonic levitation, device tilt response (accelerometer),
 * and cosmic stardust particles.
 */
class AntiGravityPhysicsEngine(
    private val context: Context,
    private val scope: CoroutineScope
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val _isAntiGravityActive = MutableStateFlow(false)
    val isAntiGravityActive: StateFlow<Boolean> = _isAntiGravityActive.asStateFlow()

    private val _sensorTiltX = MutableStateFlow(0f)
    private val _sensorTiltY = MutableStateFlow(0f)

    private val _layerOffsets = MutableStateFlow<Map<String, LayerPhysicsOffset>>(emptyMap())
    val layerOffsets: StateFlow<Map<String, LayerPhysicsOffset>> = _layerOffsets.asStateFlow()

    private val _particles = MutableStateFlow<List<CosmicZeroGParticle>>(emptyList())
    val particles: StateFlow<List<CosmicZeroGParticle>> = _particles.asStateFlow()

    private var simulationJob: Job? = null
    private var isSensorRegistered = false

    private val initialParticles = (0..18).map { idx ->
        CosmicZeroGParticle(
            id = idx,
            x = (idx * 0.053f + 0.05f) % 0.95f,
            y = (idx * 0.071f + 0.1f) % 0.95f,
            radius = (1.5f + (idx % 4) * 1.2f),
            alpha = (0.25f + (idx % 5) * 0.15f),
            colorHex = when (idx % 3) {
                0 -> "#38BDF8" // Cyan
                1 -> "#A855F7" // Violet
                else -> "#FDE047" // Gold
            }
        )
    }

    init {
        _particles.value = initialParticles
    }

    fun setAntiGravityActive(active: Boolean, layerIds: List<String> = emptyList()) {
        _isAntiGravityActive.value = active
        if (active) {
            registerSensor()
            startSimulation(layerIds)
        } else {
            unregisterSensor()
            stopSimulation()
            _layerOffsets.value = emptyMap()
        }
    }

    private fun registerSensor() {
        if (!isSensorRegistered && accelerometer != null) {
            sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
            isSensorRegistered = true
        }
    }

    private fun unregisterSensor() {
        if (isSensorRegistered) {
            sensorManager?.unregisterListener(this)
            isSensorRegistered = false
            _sensorTiltX.value = 0f
            _sensorTiltY.value = 0f
        }
    }

    private fun startSimulation(layerIds: List<String>) {
        simulationJob?.cancel()
        simulationJob = scope.launch(Dispatchers.Default) {
            var timeStep = 0f
            while (isActive) {
                timeStep += 0.045f

                val tiltX = -(_sensorTiltX.value / 9.8f).coerceIn(-0.4f, 0.4f)
                val tiltY = (_sensorTiltY.value / 9.8f).coerceIn(-0.4f, 0.4f)

                // 1. Calculate weightless harmonic levitation for all layers
                val offsets = mutableMapOf<String, LayerPhysicsOffset>()
                layerIds.forEachIndexed { idx, id ->
                    val phase = idx * 1.35f
                    // Anti-gravity sinusoidal drift
                    val dx = (sin(timeStep * 1.3f + phase) * 0.022f) + (tiltX * 0.05f)
                    val dy = (cos(timeStep * 1.7f + phase) * 0.032f) - (tiltY * 0.05f)
                    val dRot = (sin(timeStep * 0.9f + phase) * 3.5f)

                    offsets[id] = LayerPhysicsOffset(deltaX = dx, deltaY = dy, deltaRotation = dRot)
                }
                _layerOffsets.value = offsets

                // 2. Animate cosmic floating zero-G particles
                val currentP = _particles.value
                val updatedP = currentP.map { p ->
                    var newY = p.y - (0.0022f * (p.radius / 2f)) // drift upwards defying gravity
                    if (newY < -0.05f) newY = 1.05f

                    var newX = p.x + (sin(timeStep + p.id) * 0.0015f) + (tiltX * 0.003f)
                    if (newX < -0.05f) newX = 1.05f
                    if (newX > 1.05f) newX = -0.05f

                    p.copy(x = newX, y = newY, alpha = (0.3f + sin(timeStep * 2f + p.id) * 0.25f).coerceIn(0.1f, 0.9f))
                }
                _particles.value = updatedP

                delay(22L) // ~45-50 FPS smooth physics loop
            }
        }
    }

    private fun stopSimulation() {
        simulationJob?.cancel()
        simulationJob = null
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null && event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            _sensorTiltX.value = event.values[0]
            _sensorTiltY.value = event.values[1]
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun release() {
        unregisterSensor()
        stopSimulation()
    }
}
