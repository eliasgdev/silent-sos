package com.example.silentsos.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.silentsos.data.repository.EmergencyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val repository: EmergencyRepository
) : ViewModel() {

    fun onBienvenidaVista() {
        repository.marcarBienvenidaVista()
    }
}
