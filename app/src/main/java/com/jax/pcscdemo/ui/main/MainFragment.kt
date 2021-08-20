package com.jax.pcscdemo.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.jax.pcscdemo.R
import com.jax.pcscdemo.databinding.MainFragmentBinding
import com.qytech.pcscreader.SmartCardManager
import com.qytech.pcscreader.apdu.EF
import com.qytech.pcscreader.apdu.ISmartCardManger
import com.qytech.pcscreader.apdu.PCSCUtils
import com.qytech.pcscreader.apdu.SCardStatusChangeListener
import timber.log.Timber

class MainFragment : Fragment() {

    companion object {
        fun newInstance() = MainFragment()
    }

    private lateinit var viewModel: MainViewModel
    private lateinit var dataBinding: MainFragmentBinding
    private lateinit var smartCardManager: ISmartCardManger
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        dataBinding.viewmodel = viewModel
        dataBinding.lifecycleOwner = viewLifecycleOwner
        smartCardManager = SmartCardManager(requireContext(), smartCardStatusListener)
    }

    override fun onResume() {
        super.onResume()
        smartCardManager.connectCardReader { }
    }

    override fun onStop() {
        super.onStop()
        smartCardManager.disConnectCardReader { }
    }

    override fun onDestroy() {
        super.onDestroy()
        smartCardManager.release()
    }

    private val smartCardStatusListener = object : SCardStatusChangeListener {
        override fun onSCardConnect(atr: String) {
            Timber.d("onSCardConnect atr is $atr")
            if (atr.isEmpty()) {
                return
            }
            smartCardManager.initCard(mrzDT3)
            // 同步读取和异步读取任选一种
            // 推荐使用异步方式,
            // 不要同步和异步混用，
            // 如果同步和异步混用的话基本上会导致 MAC 计算错误

            if (System.currentTimeMillis() % 2 == 0L) {
                readAsync()
            } else {
                readSync()
            }
        }

        override fun onSCardDisconnect() {
            Timber.d("onSCardDisconnect")
            viewModel.setPassportInfo(null)
            dataBinding.ivPassportAvatars.setImageResource(R.color.cardview_dark_background)
        }
    }

    private fun readAsync() {
        smartCardManager.readCard(EF.COM) { com ->
            val efCom = EF.parsingCOM(com.getOrDefault(""))
            Timber.d("readAsync EF.COM is $efCom")

            smartCardManager.readCard(EF.DG1) { dg1 ->
                val mrzInfo = EF.parsingDG1(dg1.getOrDefault(""))
                Timber.d("readAsync EF.DG1 is $mrzInfo")
                val passportInfo = PCSCUtils.getPassportInfo(mrzInfo)
                viewModel.setPassportInfo(passportInfo)
            }
        }
    }

    private fun readSync() {
        smartCardManager.readCard(EF.COM, async = false) { com ->
            val efCom = EF.parsingCOM(com.getOrDefault(""))
            Timber.d("readSync EF.COM is $efCom")
        }

        smartCardManager.readCard(EF.DG1, async = false) { dg1 ->
            val mrzInfo = EF.parsingDG1(dg1.getOrDefault(""))
            Timber.d("readSync EF.DG1 is $mrzInfo")
            val passportInfo = PCSCUtils.getPassportInfo(mrzInfo)
            viewModel.setPassportInfo(passportInfo)
        }
    }
}
