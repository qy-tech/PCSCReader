package com.jax.pcscdemo.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.qytech.pcscreader.apdu.PassportInfo

class MainViewModel : ViewModel() {
    private val _passportInfo = MutableLiveData<PassportInfo?>()
    val passportInfo: LiveData<PassportInfo?> = _passportInfo

    fun setPassportInfo(passportInfo: PassportInfo?) {
        _passportInfo.value = passportInfo
    }

}