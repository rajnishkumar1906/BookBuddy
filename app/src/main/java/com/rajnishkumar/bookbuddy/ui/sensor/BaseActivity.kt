package com.rajnishkumar.bookbuddy.ui.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.rajnishkumar.bookbuddy.R
import kotlin.math.atan2
import kotlin.math.sqrt

abstract class BaseActivity : AppCompatActivity(), SensorEventListener {

    private val TAG = "BaseActivityLogger"
    private lateinit var sensorManager: SensorManager
    private var proximitySensor: Sensor? = null
    private var accelerometer: Sensor? = null
    
    private val handler = Handler(Looper.getMainLooper())
    private var lastToastTime = 0L
    private val TOAST_COOLDOWN = 5000L
    
    private var alertDialog: AlertDialog? = null
    private var isStartupProtected = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "🚀 onCreate: ${this.javaClass.simpleName}")
        
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        
        Log.d(TAG, "🔍 Sensors: Proximity=${proximitySensor != null}, Accel=${accelerometer != null}")
        
        handler.postDelayed({ 
            isStartupProtected = false 
            Log.d(TAG, "🛡️ Startup protection disabled for ${this.javaClass.simpleName}")
        }, 8000)
        
        startUsageTimer()
    }

    override fun setContentView(layoutResID: Int) {
        Log.d(TAG, "🖼️ Setting content view for: ${this.javaClass.simpleName}")
        val root = FrameLayout(this)
        root.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        
        // Apply the global app border
        root.setBackgroundResource(R.drawable.bg_app_border)
        
        layoutInflater.inflate(layoutResID, root, true)
        super.setContentView(root)
    }

    override fun setContentView(view: View?) {
        if (view == null) return
        
        val root = FrameLayout(this)
        root.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        
        // Apply the global app border
        root.setBackgroundResource(R.drawable.bg_app_border)
        
        root.addView(view)
        super.setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "▶️ onResume: ${this.javaClass.simpleName}")
        proximitySensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "⏸️ onPause: ${this.javaClass.simpleName}")
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (isStartupProtected) return 

        event?.let {
            when (it.sensor.type) {
                Sensor.TYPE_PROXIMITY -> handleProximity(it.values[0])
                Sensor.TYPE_ACCELEROMETER -> handleAccelerometer(it.values)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun handleProximity(distance: Float) {
        proximitySensor?.let { sensor ->
            Log.v(TAG, "📏 Proximity distance: $distance (Max: ${sensor.maximumRange})")
            if (distance < 1.0f) { 
                Log.w(TAG, "🚨 Proximity Triggered! Hand/Face too close to screen.")
                showWarningDialog("Eye Health", "Please keep your eyes at a safe distance from the screen.") {
                    Log.i(TAG, "💡 User clicked OK on proximity warning.")
                }
            }
        }
    }

    private fun handleAccelerometer(values: FloatArray) {
        val x = values[0]
        val y = values[1]
        val z = values[2]
        val tilt = Math.toDegrees(atan2(sqrt((x * x + z * z).toDouble()), y.toDouble()))
        if (tilt > 80.0) { 
            Log.w(TAG, "📐 Tilt Triggered! Angle: $tilt")
            showToast("Please maintain a proper viewing angle.")
        }
    }

    private fun startUsageTimer() {
        UsageTracker.startTimer {
            Log.i(TAG, "⏰ Usage limit reached (30 min).")
            showWarningDialog("Take a Break", "Time to rest your eyes!") {
                Log.i(TAG, "💡 User acknowledged break timer.")
            }
        }
    }

    private fun showWarningDialog(title: String, message: String, onDismiss: () -> Unit) {
        if (isFinishing || isDestroyed) return
        if (alertDialog?.isShowing == true) return

        Log.d(TAG, "💬 Showing Warning Dialog: $title")
        alertDialog = AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                onDismiss()
            }
            .show()
    }

    private fun showToast(message: String) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastToastTime > TOAST_COOLDOWN) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            lastToastTime = currentTime
        }
    }
}