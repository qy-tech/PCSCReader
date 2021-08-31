# Android PcscReader lib 使用说明

1. 导入依赖库

   在对应的 app 中的 build.gradle 文件中添加一下依赖

   ```groovy
       implementation files('libs/pcscReader-release.aar')
       implementation "com.jakewharton.timber:timber:4.7.1"
       implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.4.3'
       implementation "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.3"
       implementation 'com.gemalto.jp2:jp2-android:1.0'	
   ```


2. 基本使用介绍

   ```kotlin
   override fun onActivityCreated(savedInstanceState: Bundle?) {
           super.onActivityCreated(savedInstanceState)
           //省略其他代码......
           smartCardManager = SmartCardManager(requireContext(), smartCardStatusListener)

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
   
   ```

3. API 文档

[PCSCReader DOC](https://qy-tech.github.io/PCSCReader/)

4. 更新日志

* `[ 2021-08-31 ]` ： 修复连续刷卡或者切后台时可能会出现读取设备状态异常并且无法恢复问题
