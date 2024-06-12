package com.adventure.tapper.ui.dashboard

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
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

    private var myService: FloatingWidgetService? = null
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as FloatingWidgetService.LocalBinder
            myService = binder.getService()
            isBound = true
            setupUi()
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            myService = null
            isBound = false
            setupUi()
        }
    }

    private fun isTapControllerRunning(): Boolean {
        return isBound && (myService?.isServiceRunning() ?: false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewBinding = DataBindingUtil.setContentView(this, R.layout.activity_dashboard)
        viewBinding.lifecycleOwner = this
        viewBinding.setVariable(BR.viewModel, viewModel)
        viewBinding.executePendingBindings()

        initObservers()

        setupUi()
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
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    private fun initObservers() {
        viewModel.dashboardEvent.observe(this) { event ->
            when (event) {
                DashboardViewModel.DashboardEvent.OpenAccessibilitySettingsEvent -> promptEnableAccessibilityService()
                DashboardViewModel.DashboardEvent.RequestOverlayPermissionEvent -> requestOverlayPermission()
                DashboardViewModel.DashboardEvent.OpenTapSettingsEvent -> openTapSettings()
                DashboardViewModel.DashboardEvent.StartTapServiceEvent -> startFloatingWidgetService()
            }
        }
    }

    private fun setupUi() {
        val hasAccessibilityServicePermission = hasAccessibilityServicePermission()
        viewModel.showAccessibilityPermissionScreen.value = hasAccessibilityServicePermission.not()

        val hasOverlayPermission = hasOverlayPermission()
        viewModel.showOverlayPermissionScreen.value =
            hasAccessibilityServicePermission && hasOverlayPermission.not()


        val tapControllerRunning = isTapControllerRunning()
        if (hasAccessibilityServicePermission && hasOverlayPermission && tapControllerRunning.not()) {
            startFloatingWidgetService()
            return
        }

        viewModel.showDashboardScreen.value =
            hasAccessibilityServicePermission && hasOverlayPermission && tapControllerRunning
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