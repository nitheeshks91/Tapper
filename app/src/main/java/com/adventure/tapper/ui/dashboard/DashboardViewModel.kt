package com.adventure.tapper.ui.dashboard

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.adventure.tapper.utils.SingleLiveEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor() : ViewModel() {

    val showAccessibilityPermissionScreen = MutableLiveData(false)
    val showOverlayPermissionScreen = MutableLiveData(false)
    val showDashboardScreen = MutableLiveData(false)
    //  <!-- Not using for time being-->
    val showStartTapControllerServiceScreen = MutableLiveData(false)

    private val _dashboardEvent = SingleLiveEvent<DashboardEvent>()
    val dashboardEvent: SingleLiveEvent<DashboardEvent> = _dashboardEvent

    fun openAccessibilitySettings() {
        _dashboardEvent.postValue(DashboardEvent.OpenAccessibilitySettingsEvent)
    }

    fun requestOverlayPermission() {
        _dashboardEvent.postValue(DashboardEvent.RequestOverlayPermissionEvent)
    }

    fun openTapSettings() {
        _dashboardEvent.postValue(DashboardEvent.OpenTapSettingsEvent)
    }

    fun startTapService() {
        _dashboardEvent.postValue(DashboardEvent.StartTapServiceEvent)
    }

    sealed class DashboardEvent {
        object OpenAccessibilitySettingsEvent : DashboardEvent()
        object RequestOverlayPermissionEvent : DashboardEvent()
        object OpenTapSettingsEvent : DashboardEvent()
        object StartTapServiceEvent : DashboardEvent()
    }
}