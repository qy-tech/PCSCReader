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
import com.qytech.pcscreader.apdu.PCSCUtils
import com.qytech.pcscreader.manager.ISmartCardManager
import com.qytech.pcscreader.manager.SCardStatusChangeListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        dataBinding.viewmodel = viewModel
        dataBinding.lifecycleOwner = viewLifecycleOwner
        smartCardManager = SmartCardManager(requireContext(), smartCardStatusListener)
    }

    override fun onResume() {
        super.onResume()
        smartCardManager.launch {
            smartCardManager.connectCardReader()
        }
    }

    override fun onStop() {
        super.onStop()
        smartCardManager.disConnectCardReader()
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

            smartCardManager.launch(Dispatchers.Main) {
                withContext(Dispatchers.IO) {
                    smartCardManager.readCard(EF.COM)
                }?.let {
                    Timber.d("onSCardConnect: ${EF.parsingCOM(it)}")
                }

                withContext(Dispatchers.IO) {
                    smartCardManager.readCard(EF.DG1)
                }?.let {
                    val dg1 = EF.parsingDG1(it)
                    viewModel.setPassportInfo(PCSCUtils.getPassportInfo(dg1))
                    Timber.d("onSCardConnect: $dg1")
                }

                withContext(Dispatchers.IO) {
                    smartCardManager.readCard(EF.DG2)
                }?.let {
                    val dg2 = EF.parsingDG2(it)
                    dataBinding.ivPassportAvatars.setImageBitmap(dg2)
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
