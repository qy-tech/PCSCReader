package com.jax.pcscdemo.ui.main

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jax.pcscdemo.R
import com.jax.pcscdemo.databinding.MainFragmentBinding
import com.qytech.pcscreader.apdu.EF
import com.qytech.pcscreader.apdu.PCSCUtils
import com.qytech.pcscreader.apdu.SmartCardManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber

class MainFragment : Fragment() {

    companion object {
        fun newInstance() = MainFragment()
    }

    private lateinit var viewModel: MainViewModel
    private lateinit var dataBinding: MainFragmentBinding
    private lateinit var smartCardManager: SmartCardManager
    private var mrzDT3 =
        "POCHNLI<<DONGHAN<<<<<<<<<<<<<<<<<<<<<<<<<<<<\nE872545052CHN8404039M2610092MAOOLGKLLKLKA998"
    private var mrzDT2 =
        "I<UTOSTEVENSON<<PETER<JOHN<<<<<<<<<<D23145890<UTO3407127M95071227349<<<8"
    private var mrzDT1 =
        "I<UTOD23145890<7349<<<<<<<<<<<3407127M9507122UTO<<<<<<<<<<<2STEVENSON<<PETER<JOHN<<<<<<<<<"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dataBinding = MainFragmentBinding.inflate(inflater, container, false)
        return dataBinding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        dataBinding.viewmodel = viewModel
        dataBinding.lifecycleOwner = viewLifecycleOwner
        smartCardManager = SmartCardManager(requireContext(), smartCardStatusListener)
        val result = smartCardManager.connectCardReader()
        if (!result) {
            Timber.e("connectCardReader failed ,try aging later")
            return
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        smartCardManager.destroyCardReader()
    }

    private val smartCardStatusListener = object : SmartCardManager.SCardStatusChangeListener {
        override fun onSCardConnect(atr: String) {
            Timber.d("onSCardConnect atr is $atr")
            if (atr.isEmpty()) {
                return
            }
            val result = smartCardManager.initPassport(mrzDT3)
            if (!result) {
                return
            }
            dataBinding.progress.visibility = View.VISIBLE
            GlobalScope.launch(Dispatchers.IO) {
                var data = smartCardManager.readingPassport(EF.COM)
                val com = EF.parsingCOM(data)
                Timber.d("EF.COM is $com")
                data = smartCardManager.readingPassport(EF.DG1)
                val mrzInfo = EF.parsingDG1(data)
                Timber.d("EF.DG1 is $mrzInfo")
                var passportInfo = PCSCUtils.getPassportInfo(mrzInfo)
                data = smartCardManager.readingPassport(EF.DG2)
                val bitmap = EF.parsingDG2(data)
                if (bitmap != null) {
                    launch(Dispatchers.Main) {
                        dataBinding.ivPassportAvatars.setImageBitmap(bitmap)
                    }
                }
                data = smartCardManager.readingPassport(EF.DG11)
                passportInfo = EF.parsingDG11(data, passportInfo)
                data = smartCardManager.readingPassport(EF.DG12)
                passportInfo = EF.parsingDG12(data, passportInfo)
                launch(Dispatchers.Main) {
                    viewModel.setPassportInfo(passportInfo)
                    Timber.d("dismiss progress")
                    dataBinding.progress.visibility = View.GONE
                }
            }


        }

        override fun onSCardDisconnect() {
            Timber.d("onSCardDisconnect")
            viewModel.setPassportInfo(null)
            dataBinding.ivPassportAvatars.setImageResource(R.color.cardview_dark_background)
        }
    }
}