package com.adventure.tapper.services

import android.annotation.SuppressLint
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.core.content.res.ResourcesCompat
import androidx.preference.PreferenceManager
import com.adventure.tapper.R
import com.google.android.material.imageview.ShapeableImageView


class FloatingWidgetService : Service(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var pointerViewTouchListener: View.OnTouchListener
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var startStopView: ShapeableImageView

    private lateinit var windowManager: WindowManager

    private lateinit var floatingView: View
    private lateinit var layoutParams: WindowManager.LayoutParams

    private lateinit var pointerView: View
    private lateinit var pointerViewLayoutParams: WindowManager.LayoutParams

    private var isServiceStarted = false
    private val binder = LocalBinder()
    private var isRunning = false

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                AutoClickService.ACTION_RESET_TAP_CONTROL -> {
                    stopTapper()
                }
            }
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): FloatingWidgetService = this@FloatingWidgetService
    }

    fun isServiceRunning(): Boolean {
        return isServiceStarted
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Service logic
        return START_STICKY
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(broadcastReceiver, IntentFilter().apply {
                addAction(AutoClickService.ACTION_RESET_TAP_CONTROL)
            }, RECEIVER_EXPORTED)
        } else {
            registerReceiver(broadcastReceiver, IntentFilter().apply {
                addAction(AutoClickService.ACTION_RESET_TAP_CONTROL)
            })
        }

        isServiceStarted = true
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_widget_layout, null)
        pointerView =
            LayoutInflater.from(this).inflate(R.layout.floating_widget_pointer_layout, null)

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        layoutParams.x = 0
        layoutParams.y = 100

        pointerViewLayoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        pointerViewLayoutParams.x = 0
        pointerViewLayoutParams.y = 100

        startStopView = floatingView.findViewById(R.id.startButton)
        startStopView.setOnClickListener {
            if (isRunning) {
                stopTapper()
            } else {
                startTapper()
            }
        }

        floatingView.findViewById<ImageView>(R.id.pauseButton).setOnClickListener {
            isRunning = false
            sendBroadCastEvent(AutoClickService.ACTION_STOP_AUTO_CLICKER)
        }

        floatingView.findViewById<ImageView>(R.id.stopButton).setOnClickListener {
            isRunning = false
            sendBroadCastEvent(AutoClickService.ACTION_STOP_AUTO_CLICKER)
            stopSelf()
        }

        floatingView.findViewById<ImageView>(R.id.dragButton)
            .setOnTouchListener(object : View.OnTouchListener {
                private var initialX = 0
                private var initialY = 0
                private var initialTouchX = 0f
                private var initialTouchY = 0f

                override fun onTouch(v: View?, event: MotionEvent): Boolean {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            initialX = layoutParams.x
                            initialY = layoutParams.y
                            initialTouchX = event.rawX
                            initialTouchY = event.rawY
                            return true
                        }

                        MotionEvent.ACTION_MOVE -> {
                            layoutParams.x = initialX + (event.rawX - initialTouchX).toInt()
                            layoutParams.y = initialY + (event.rawY - initialTouchY).toInt()
                            windowManager.updateViewLayout(floatingView, layoutParams)
                            return true
                        }
                    }
                    return false
                }
            })

        pointerViewTouchListener = object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = pointerViewLayoutParams.x
                        initialY = pointerViewLayoutParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        pointerViewLayoutParams.x = initialX + (event.rawX - initialTouchX).toInt()
                        pointerViewLayoutParams.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(pointerView, pointerViewLayoutParams)
                        return true
                    }

                    MotionEvent.ACTION_UP -> {

                        val location = IntArray(2)
                        pointerView.getLocationOnScreen(location)
                        val centerX: Int = location[0] + pointerView.width / 2
                        val centerY: Int = location[1] + 63
                        Log.d("AutoClickService", "Dragged point $centerX : $centerY")
                        saveCoordinatesToPreferences(
                            centerX,
                            centerY
                        )
                        return true
                    }
                }
                return false
            }
        }

        pointerView.setOnTouchListener(pointerViewTouchListener)

        windowManager.addView(floatingView, layoutParams)
        windowManager.addView(pointerView, pointerViewLayoutParams)
    }

    private fun startTapper() {
        isRunning = true
        pointerView.setOnTouchListener(null)
        pointerView.visibility = View.GONE
        startStopView.setImageDrawable(
            ResourcesCompat.getDrawable(
                this.resources,
                R.drawable.ic_stop,
                null
            )
        )
        sendBroadCastEvent(AutoClickService.ACTION_START_AUTO_CLICKER)
    }

    private fun stopTapper() {
        isRunning = false
        pointerView.visibility = View.VISIBLE
        pointerView.setOnTouchListener(pointerViewTouchListener)
        startStopView.setImageDrawable(
            ResourcesCompat.getDrawable(
                this.resources,
                R.drawable.ic_start,
                null
            )
        )
        sendBroadCastEvent(AutoClickService.ACTION_STOP_AUTO_CLICKER)
    }

    private fun sendBroadCastEvent(action: String) {
        val intent = Intent(action)
        sendBroadcast(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceStarted = false
        if (::floatingView.isInitialized) {
            windowManager.removeView(floatingView)
            windowManager.removeView(pointerView)
        }

        unregisterReceiver(broadcastReceiver)
    }

    private fun saveCoordinatesToPreferences(x: Int, y: Int) {
        val editor = sharedPreferences.edit()
        editor.putString("click_x", x.toString())
        editor.putString("click_y", y.toString())
        editor.apply()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopTapper()
        stopSelf()
    }

    override fun onSharedPreferenceChanged(p0: SharedPreferences?, p1: String?) {
        if (isRunning) {
            stopTapper()
        }
    }
}


