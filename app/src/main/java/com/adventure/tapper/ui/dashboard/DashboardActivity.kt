package com.adventure.tapper.ui.dashboard

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.MotionEvent
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.preference.PreferenceManager
import com.adventure.tapper.BR
import com.adventure.tapper.R
import com.adventure.tapper.databinding.ActivityDashboardBinding
import com.adventure.tapper.services.AutoClickService
import com.adventure.tapper.services.FloatingWidgetService
import com.adventure.tapper.ui.settings.SettingsActivity
import com.adventure.tapper.utils.isAccessibilityServiceEnabled
import dagger.hilt.android.AndroidEntryPoint

@Suppress("DEPRECATION")
@AndroidEntryPoint
class DashboardActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityDashboardBinding

    private val viewModel: DashboardViewModel by viewModels()

    private lateinit var sharedPreferences: SharedPreferences
    private var isRecording = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewBinding = DataBindingUtil.setContentView(this, R.layout.activity_dashboard)
        viewBinding.lifecycleOwner = this
        viewBinding.setVariable(BR.viewModel, viewModel)
        viewBinding.executePendingBindings()

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        initObservers()

        setupUi()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun startRecording() {
        isRecording = true
        viewBinding.overlayLayout.visibility = View.VISIBLE
        viewBinding.overlayLayout.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val x = event.x.toInt()
                val y = event.y.toInt()
                saveCoordinatesToPreferences(x, y)
                viewBinding.overlayLayout.setOnTouchListener(null)
                stopRecording()
                return@setOnTouchListener true
            }
            return@setOnTouchListener false
        }

    }

    @SuppressLint("ClickableViewAccessibility")
    private fun stopRecording() {
        isRecording = false
        viewBinding.overlayLayout.visibility = View.GONE
    }

    private fun saveCoordinatesToPreferences(x: Int, y: Int) {
        val editor = sharedPreferences.edit()
        editor.putString("click_x", x.toString())
        editor.putString("click_y", y.toString())
        editor.apply()
    }


    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            ACCESSIBILITY_PERMISSION_REQUEST, OVERLAY_PERMISSION_REQUEST -> setupUi()
        }

    }

    private fun startFloatingWidgetService() {
        val intent = Intent(this, FloatingWidgetService::class.java)
        startService(intent)
    }

    private fun initObservers() {
        viewModel.dashboardEvent.observe(this) { event ->
            when (event) {
                DashboardViewModel.DashboardEvent.OpenAccessibilitySettingsEvent -> promptEnableAccessibilityService()
                DashboardViewModel.DashboardEvent.RequestOverlayPermissionEvent -> requestOverlayPermission()
                DashboardViewModel.DashboardEvent.OpenTapSettingsEvent -> openTapSettings()
                DashboardViewModel.DashboardEvent.SetTapPositionEvent -> setTapPosition()
            }
        }
    }

    private fun setupUi() {
        val hasAccessibilityServicePermission = hasAccessibilityServicePermission()
        viewModel.showAccessibilityPermissionScreen.value = hasAccessibilityServicePermission.not()

        val hasOverlayPermission = hasOverlayPermission()
        viewModel.showOverlayPermissionScreen.value =
            hasAccessibilityServicePermission && hasOverlayPermission.not()

        viewModel.showDashboardScreen.value =
            hasAccessibilityServicePermission && hasOverlayPermission

        if (hasAccessibilityServicePermission && hasOverlayPermission) {
            startFloatingWidgetService()
        }
    }

    private fun setTapPosition() {
        if (!isRecording) {
            startRecording()
        } else {
            stopRecording()
        }
    }

    private fun saveTapPosition() {

    }

    private fun openTapSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    private fun hasAccessibilityServicePermission() =
        isAccessibilityServiceEnabled(this, AutoClickService::class.java)

    private fun hasOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(this)

    }

    private fun promptEnableAccessibilityService() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivityForResult(intent, ACCESSIBILITY_PERMISSION_REQUEST)
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${this.packageName}")
        )
        startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST)
    }

    companion object {
        const val ACCESSIBILITY_PERMISSION_REQUEST = 100
        const val OVERLAY_PERMISSION_REQUEST = 200
    }

}