package com.adventure.tapper.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import androidx.preference.PreferenceManager

class AutoClickService : AccessibilityService(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    private val handler = Handler(Looper.getMainLooper())
    private var isAutoClickerEnabled = false
    private var clickInterval: Long = 1000
    private var clickX: Float = 500f
    private var clickY: Float = 500f
    private var numberOfClicks: Int = 10
    private var currentClicks: Int = 0
    private var startDelay: Long = 1000
    private var continueAfterClicks: Boolean = false
    private var delayAfterClicks: Long = 1000 // Default delay after clicks
    private lateinit var sharedPreferences: SharedPreferences

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_START_AUTO_CLICKER -> startAutoClickerService()
                ACTION_STOP_AUTO_CLICKER -> stopAutoClicker()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("AutoClickService", "Service Created")
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        updatePreferences()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Handle accessibility events if needed
    }

    override fun onInterrupt() {
        Log.d("AutoClickService", "Service Interrupted")
        // Handle service interruption
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        registerReceiver(broadcastReceiver, IntentFilter().apply {
            addAction(ACTION_START_AUTO_CLICKER)
            addAction(ACTION_STOP_AUTO_CLICKER)
        })

    }

    private fun startAutoClickerService() {
        Log.d("AutoClickService", "Service Connected")
//        if (isAutoClickerEnabled) {
        showToast("Starting in $startDelay ms")
        handler.postDelayed({
            startAutoClicker(clickX, clickY, clickInterval, numberOfClicks, continueAfterClicks)
        }, startDelay)
//        }
    }

    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    private fun startAutoClicker(
        x: Float,
        y: Float,
        interval: Long,
        clicks: Int,
        continueAfter: Boolean
    ) {
        Log.d(
            "AutoClickService",
            "Starting Auto Clicker at ($x, $y) every $interval ms for $clicks clicks, continue after: $continueAfter"
        )
        isAutoClickerEnabled = true
        currentClicks = 0
        handler.post(object : Runnable {
            override fun run() {
                if (isAutoClickerEnabled && (continueAfter || currentClicks < clicks)) {
                    performClick(x, y)
                    currentClicks++
                    handler.postDelayed(this, interval)
                } else {
                    stopAutoClicker()
                    handler.postDelayed({ // Adding delay after clicks
                        if (isAutoClickerEnabled) {
                            startAutoClicker(x, y, interval, clicks, continueAfter)
                        }
                    }, delayAfterClicks)
                }
            }
        })
    }

    private fun stopAutoClicker() {
        Log.d("AutoClickService", "Stopping Auto Clicker")
        isAutoClickerEnabled = false
        handler.removeCallbacksAndMessages(null)
    }

    private fun performClick(x: Float, y: Float) {
        Log.d("AutoClickService", "Performing click at ($x, $y)")
        val path = Path()
        path.moveTo(x, y)
        val gestureDescription = GestureDescription.Builder()
            .addStroke(
                GestureDescription.StrokeDescription(
                    path,
                    0,
                    100
                )
            ) // Increased duration to 100ms
            .build()
        try {
            dispatchGesture(gestureDescription, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    super.onCompleted(gestureDescription)
                    Log.d("AutoClickService", "Click completed at: ($x, $y)")

                    showToast("Clicked at ($x, $y)")
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    super.onCancelled(gestureDescription)
                    Log.d("AutoClickService", "Click cancelled")
                }
            }, null)
        } catch (e: Exception) {
            Log.e("AutoClickService", "Gesture dispatch failed", e)
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        updatePreferences()
    }

    private fun updatePreferences() {
        Log.d("AutoClickService", "Updating Preferences")
        isAutoClickerEnabled = sharedPreferences.getBoolean("enable_auto_clicker", false)
        clickInterval = sharedPreferences.getString("click_interval", "1000")?.toLong() ?: 1000
        clickX = sharedPreferences.getString("click_x", "500")?.toFloat() ?: 500f
        clickY = sharedPreferences.getString("click_y", "500")?.toFloat() ?: 500f
        numberOfClicks = sharedPreferences.getString("number_of_clicks", "10")?.toInt() ?: 10
        startDelay = sharedPreferences.getString("start_delay", "1000")?.toLong() ?: 1000
        continueAfterClicks = sharedPreferences.getBoolean("continue_after_clicks", false)
        delayAfterClicks = sharedPreferences.getString("delay_after_clicks", "1000")?.toLong()
            ?: 1000 // Fetching delay after clicks

        if (isAutoClickerEnabled) {
            showToast("Settings updated. please start if job already running")
            stopAutoClicker()

//            handler.postDelayed({
//                startAutoClicker(clickX, clickY, clickInterval, numberOfClicks, continueAfterClicks)
//            }, startDelay)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("AutoClickService", "Service Destroyed")
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        unregisterReceiver(broadcastReceiver)
    }

    companion object {
        const val ACTION_START_AUTO_CLICKER = "com.adventure.tapper.START_AUTO_CLICKER"
        const val ACTION_STOP_AUTO_CLICKER = "com.adventure.tapper.STOP_AUTO_CLICKER"
    }
}
